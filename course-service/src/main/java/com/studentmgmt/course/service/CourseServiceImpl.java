package com.studentmgmt.course.service;

import com.studentmgmt.course.dto.CourseDTO;
import com.studentmgmt.course.exception.CourseNotFoundException;
import com.studentmgmt.course.exception.DuplicateCourseException;
import com.studentmgmt.course.exception.OperationNotAllowedException;
import com.studentmgmt.course.exception.SeatUnavailableException;
import com.studentmgmt.course.model.Course;
import com.studentmgmt.course.repository.CourseRepository;
import com.studentmgmt.course.specification.CourseSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    // ---------------------------------------------------------------- create / read

    @Override
    public CourseDTO.Response createCourse(CourseDTO.Request request) {
        String code = normalizeCode(request.getCode());
        log.info("Creating course {}", code);

        if (courseRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateCourseException(code);
        }
        validateDates(request);

        Course course = Course.builder()
                .code(code)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .credits(request.getCredits())
                .department(request.getDepartment().trim())
                .semester(request.getSemester().trim().toUpperCase(Locale.ROOT))
                .instructorId(request.getInstructorId())
                .capacity(request.getCapacity())
                .status(request.getStatus() != null ? request.getStatus() : Course.Status.OPEN)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        return CourseDTO.Response.fromEntity(courseRepository.save(course));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDTO.Response getCourseById(Long id) {
        return CourseDTO.Response.fromEntity(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDTO.Response getCourseByCode(String code) {
        return courseRepository.findByCodeIgnoreCase(code)
                .map(CourseDTO.Response::fromEntity)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with code: " + code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDTO.Response> getCoursesByIds(List<Long> ids) {
        return courseRepository.findAllById(ids).stream()
                .map(CourseDTO.Response::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTO.Response> getCourses(String keyword, String department, String semester,
                                               Course.Status status, boolean availableOnly, Pageable pageable) {
        return courseRepository
                .findAll(CourseSpecifications.withFilters(keyword, department, semester, status, availableOnly), pageable)
                .map(CourseDTO.Response::fromEntity);
    }

    // ---------------------------------------------------------------- update / delete

    @Override
    public CourseDTO.Response updateCourse(Long id, CourseDTO.Request request) {
        Course course = findById(id);
        String code = normalizeCode(request.getCode());

        if (!course.getCode().equalsIgnoreCase(code) && courseRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateCourseException(code);
        }
        if (request.getCapacity() < course.getEnrolledCount()) {
            throw new OperationNotAllowedException("Capacity (" + request.getCapacity()
                    + ") cannot be lower than the current enrollment (" + course.getEnrolledCount() + ")");
        }
        validateDates(request);

        course.setCode(code);
        course.setTitle(request.getTitle().trim());
        course.setDescription(request.getDescription());
        course.setCredits(request.getCredits());
        course.setDepartment(request.getDepartment().trim());
        course.setSemester(request.getSemester().trim().toUpperCase(Locale.ROOT));
        course.setInstructorId(request.getInstructorId());
        course.setCapacity(request.getCapacity());
        course.setStartDate(request.getStartDate());
        course.setEndDate(request.getEndDate());
        if (request.getStatus() != null) {
            course.setStatus(request.getStatus());
        }
        // enrolledCount is deliberately never set here. Only reserveSeat/releaseSeat change it.

        return CourseDTO.Response.fromEntity(courseRepository.save(course));
    }

    @Override
    public CourseDTO.Response updateStatus(Long id, Course.Status status) {
        Course course = findById(id);
        course.setStatus(status);
        return CourseDTO.Response.fromEntity(courseRepository.save(course));
    }

    @Override
    public void deleteCourse(Long id) {
        Course course = findById(id);
        if (course.getEnrolledCount() > 0) {
            throw new OperationNotAllowedException("Cannot delete a course with "
                    + course.getEnrolledCount() + " enrolled student(s). Set its status to CANCELLED instead.");
        }
        courseRepository.delete(course);
        log.info("Deleted course {}", id);
    }

    // ---------------------------------------------------------------- seats (called by enrollment-service)

    @Override
    public CourseDTO.Response reserveSeat(Long id) {
        int updated = courseRepository.reserveSeat(id, Course.Status.OPEN, LocalDateTime.now());
        if (updated == 0) {
            // The atomic update matched nothing. Find out why so the caller gets a precise error.
            Course course = findById(id);   // throws 404 if the course does not exist
            if (course.getStatus() != Course.Status.OPEN) {
                throw new SeatUnavailableException(
                        "Course " + course.getCode() + " is not open for enrollment (status: " + course.getStatus() + ")");
            }
            throw new SeatUnavailableException("Course " + course.getCode() + " is full");
        }
        log.info("Seat reserved on course {}", id);
        return CourseDTO.Response.fromEntity(findById(id));
    }

    @Override
    public CourseDTO.Response releaseSeat(Long id) {
        int updated = courseRepository.releaseSeat(id, LocalDateTime.now());
        if (updated == 0) {
            Course course = findById(id);   // throws 404 if the course does not exist
            throw new SeatUnavailableException(
                    "Course " + course.getCode() + " has no reserved seats to release");
        }
        log.info("Seat released on course {}", id);
        return CourseDTO.Response.fromEntity(findById(id));
    }

    // ---------------------------------------------------------------- summary

    @Override
    @Transactional(readOnly = true)
    public CourseDTO.Summary getSummary() {
        long capacity = courseRepository.sumCapacity();
        long enrolled = courseRepository.sumEnrolled();

        Map<String, Long> byDepartment = new LinkedHashMap<>();
        courseRepository.countByDepartment()
                .forEach(row -> byDepartment.put((String) row[0], (Long) row[1]));

        return CourseDTO.Summary.builder()
                .totalCourses(courseRepository.count())
                .openCourses(courseRepository.countByStatus(Course.Status.OPEN))
                .totalCapacity(capacity)
                .totalEnrolled(enrolled)
                .occupancyPercent(capacity == 0 ? 0.0 : Math.round(enrolled * 1000.0 / capacity) / 10.0)
                .coursesByDepartment(byDepartment)
                .build();
    }

    // ---------------------------------------------------------------- helpers

    private Course findById(Long id) {
        return courseRepository.findById(id).orElseThrow(() -> new CourseNotFoundException(id));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void validateDates(CourseDTO.Request r) {
        if (r.getStartDate() != null && r.getEndDate() != null && r.getEndDate().isBefore(r.getStartDate())) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
    }
}
