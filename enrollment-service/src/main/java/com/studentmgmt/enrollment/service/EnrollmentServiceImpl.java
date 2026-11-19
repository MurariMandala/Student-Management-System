package com.studentmgmt.enrollment.service;

import com.studentmgmt.enrollment.client.CourseClient;
import com.studentmgmt.enrollment.client.CourseDto;
import com.studentmgmt.enrollment.client.RemoteConflictException;
import com.studentmgmt.enrollment.client.RemoteNotFoundException;
import com.studentmgmt.enrollment.client.RemoteResponse;
import com.studentmgmt.enrollment.client.StudentClient;
import com.studentmgmt.enrollment.client.StudentDto;
import com.studentmgmt.enrollment.dto.EnrollmentDTO;
import com.studentmgmt.enrollment.event.EnrollmentEvent;
import com.studentmgmt.enrollment.event.EnrollmentEventPublisher;
import com.studentmgmt.enrollment.exception.BusinessRuleException;
import com.studentmgmt.enrollment.exception.CourseNotFoundException;
import com.studentmgmt.enrollment.exception.DuplicateEnrollmentException;
import com.studentmgmt.enrollment.exception.EnrollmentNotFoundException;
import com.studentmgmt.enrollment.exception.ServiceUnavailableException;
import com.studentmgmt.enrollment.exception.StudentNotFoundException;
import com.studentmgmt.enrollment.model.Enrollment;
import com.studentmgmt.enrollment.repository.EnrollmentRepository;
import com.studentmgmt.enrollment.specification.EnrollmentSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Deliberately NOT @Transactional at class level. enroll() and drop() make remote calls, and a database
 * transaction must never stay open across them. Each repository call is its own short transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentClient studentClient;
    private final CourseClient courseClient;
    private final EnrollmentEventPublisher eventPublisher;

    // ---------------------------------------------------------------- enroll

    @Override
    public EnrollmentDTO.Response enroll(Long studentId, Long courseId) {
        // 1. Is the student real and ACTIVE? (student-service owns that fact)
        StudentDto student = fetchStudent(studentId);
        if (!"ACTIVE".equals(student.status())) {
            throw new BusinessRuleException(
                    "Student " + studentId + " is not active (status: " + student.status() + ")");
        }

        // 2. Duplicate check. Cheap and gives a friendly error, but not race-proof. The unique
        //    constraint below is the real guarantee. A DROPPED row may be re-enrolled.
        Optional<Enrollment> existing = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (existing.isPresent() && existing.get().getStatus() != Enrollment.Status.DROPPED) {
            throw new DuplicateEnrollmentException(studentId, courseId);
        }

        // 3. Take a seat (course-service owns capacity). Throws 404 / 409 / 503.
        reserveSeat(courseId);

        // 4. Store the link. Whatever goes wrong from here, the seat must not leak.
        boolean saved = false;
        try {
            Enrollment result = existing.isPresent()
                    ? reactivate(existing.get())
                    : enrollmentRepository.saveAndFlush(new Enrollment(studentId, courseId));
            saved = true;
            publish(EnrollmentEvent.Type.STUDENT_ENROLLED, result);
            log.info("Student {} enrolled in course {} (enrollment {})", studentId, courseId, result.getId());
            return EnrollmentDTO.Response.fromEntity(result);

        } catch (DataIntegrityViolationException e) {
            // The only constraint that can fire is the unique pair: another request won the race.
            throw new DuplicateEnrollmentException(studentId, courseId);

        } finally {
            if (!saved) {
                releaseSeatQuietly(courseId);
            }
        }
    }

    /** DROPPED to ENROLLED in one conditional UPDATE. 0 rows means someone else re-enrolled first. */
    private Enrollment reactivate(Enrollment existing) {
        int updated = enrollmentRepository.reactivate(existing.getId(),
                Enrollment.Status.DROPPED, Enrollment.Status.ENROLLED, LocalDateTime.now());
        if (updated == 0) {
            throw new DuplicateEnrollmentException(existing.getStudentId(), existing.getCourseId());
        }
        return findById(existing.getId());
    }

    // ---------------------------------------------------------------- drop / complete

    @Override
    public EnrollmentDTO.Response drop(Long id) {
        Enrollment enrollment = findById(id);

        // Only the request that actually flips ENROLLED to DROPPED may free the seat,
        // so two simultaneous drops release exactly one seat.
        int updated = enrollmentRepository.drop(id,
                Enrollment.Status.ENROLLED, Enrollment.Status.DROPPED, LocalDateTime.now());
        if (updated == 0) {
            throw new BusinessRuleException(
                    "Enrollment " + id + " is not active (status: " + enrollment.getStatus() + ")");
        }

        releaseSeatQuietly(enrollment.getCourseId());
        Enrollment dropped = findById(id);
        publish(EnrollmentEvent.Type.STUDENT_DROPPED, dropped);
        log.info("Enrollment {} dropped", id);
        return EnrollmentDTO.Response.fromEntity(dropped);
    }

    @Override
    public EnrollmentDTO.Response complete(Long id) {
        Enrollment enrollment = findById(id);
        int updated = enrollmentRepository.complete(id,
                Enrollment.Status.ENROLLED, Enrollment.Status.COMPLETED, LocalDateTime.now());
        if (updated == 0) {
            throw new BusinessRuleException(
                    "Enrollment " + id + " is not active (status: " + enrollment.getStatus() + ")");
        }
        // The seat stays counted: a completed enrollment still occupied the course.
        Enrollment completed = findById(id);
        publish(EnrollmentEvent.Type.ENROLLMENT_COMPLETED, completed);
        return EnrollmentDTO.Response.fromEntity(completed);
    }

    // ---------------------------------------------------------------- queries

    @Override
    public EnrollmentDTO.Response getById(Long id) {
        return EnrollmentDTO.Response.fromEntity(findById(id));
    }

    @Override
    public Page<EnrollmentDTO.Response> list(Long studentId, Long courseId, Enrollment.Status status,
                                             Pageable pageable) {
        Page<EnrollmentDTO.Response> response = enrollmentRepository
                .findAll(EnrollmentSpecifications.withFilters(studentId, courseId, status), pageable)
                .map(EnrollmentDTO.Response::fromEntity);
        List<EnrollmentDTO.Response> enrollmentList = response.getContent();
        System.out.println("enrollmentList : "+enrollmentList);
        if(enrollmentList.isEmpty()){
            throw new BusinessRuleException("You don't have any enrolled courses");
        }
        return enrollmentRepository
                .findAll(EnrollmentSpecifications.withFilters(studentId, courseId, status), pageable)
                .map(EnrollmentDTO.Response::fromEntity);
    }

    @Override
    public EnrollmentDTO.CheckResponse check(Long studentId, Long courseId) {
        return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .map(e -> EnrollmentDTO.CheckResponse.builder()
                        .enrolled(e.getStatus() == Enrollment.Status.ENROLLED)
                        .enrollmentId(e.getId())
                        .status(e.getStatus())
                        .build())
                .orElseGet(() -> EnrollmentDTO.CheckResponse.builder().enrolled(false).build());
    }

    @Override
    public List<EnrollmentDTO.EnrolledCourse> getCoursesOfStudent(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findAll(
                EnrollmentSpecifications.withFilters(studentId, null, Enrollment.Status.ENROLLED),
                Sort.by("enrolledAt"));
        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<Long> courseIds = enrollments.stream().map(Enrollment::getCourseId).distinct().toList();
        Map<Long, CourseDto> coursesById = fetchCourses(courseIds).stream()
                .collect(Collectors.toMap(CourseDto::id, Function.identity(), (a, b) -> a));

        return enrollments.stream()
                .map(e -> EnrollmentDTO.EnrolledCourse.builder()
                        .enrollmentId(e.getId())
                        .status(e.getStatus())
                        .enrolledAt(e.getEnrolledAt())
                        .course(coursesById.get(e.getCourseId()))
                        .build())
                .toList();
    }

    // ---------------------------------------------------------------- remote calls

    private StudentDto fetchStudent(Long studentId) {
        try {
            RemoteResponse<StudentDto> response = studentClient.getStudent(studentId);
            if (response == null || response.data() == null) {
                throw new ServiceUnavailableException("Student service returned an empty response", null);
            }
            return response.data();
        } catch (RemoteNotFoundException e) {
            throw new StudentNotFoundException(studentId);
        }
    }

    private void reserveSeat(Long courseId) {
        try {
            courseClient.reserveSeat(courseId);
        } catch (RemoteNotFoundException e) {
            throw new CourseNotFoundException(courseId);
        } catch (RemoteConflictException e) {
            throw new BusinessRuleException(e.getMessage());   // "Course CS499 is full", "... not open ..."
        }
    }

    private List<CourseDto> fetchCourses(List<Long> courseIds) {
        RemoteResponse<List<CourseDto>> response = courseClient.getBatch(courseIds);
        return response == null || response.data() == null ? List.of() : response.data();
    }

    /**
     * Compensation must never mask the original outcome, so failures are logged instead of thrown.
     * A failed release leaves the course counter one too high until it is reconciled.
     */
    private void releaseSeatQuietly(Long courseId) {
        try {
            courseClient.releaseSeat(courseId);
        } catch (RuntimeException e) {
            log.error("Could not release seat on course {}. The seat counter needs reconciliation", courseId, e);
        }
    }

    private void publish(EnrollmentEvent.Type type, Enrollment enrollment) {
        try {
            eventPublisher.publish(new EnrollmentEvent(type, enrollment.getId(), enrollment.getStudentId(),
                    enrollment.getCourseId(), LocalDateTime.now()));
        } catch (RuntimeException e) {
            log.error("Failed to publish {} for enrollment {}", type, enrollment.getId(), e);
        }
    }

    private Enrollment findById(Long id) {
        return enrollmentRepository.findById(id).orElseThrow(() -> new EnrollmentNotFoundException(id));
    }
}
