package com.studentmgmt.enrollment;

import com.studentmgmt.enrollment.client.CourseClient;
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
import com.studentmgmt.enrollment.exception.ServiceUnavailableException;
import com.studentmgmt.enrollment.exception.StudentNotFoundException;
import com.studentmgmt.enrollment.model.Enrollment;
import com.studentmgmt.enrollment.repository.EnrollmentRepository;
import com.studentmgmt.enrollment.service.EnrollmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    private static final Long STUDENT = 5L;
    private static final Long COURSE = 3L;

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private StudentClient studentClient;
    @Mock private CourseClient courseClient;
    @Mock private EnrollmentEventPublisher eventPublisher;
    @InjectMocks private EnrollmentServiceImpl service;

    private RemoteResponse<StudentDto> student(String status) {
        return new RemoteResponse<>(true, "ok", new StudentDto(STUDENT, "Asha", "Rao", "asha@uni.edu", status));
    }

    private Enrollment row(Long id, Enrollment.Status status) {
        Enrollment e = new Enrollment(STUDENT, COURSE);
        e.setId(id);
        e.setStatus(status);
        e.setEnrolledAt(LocalDateTime.now());
        return e;
    }

    // ---------------------------------------------------------------- enroll

    @Test
    void enroll_success_savesRowAndPublishesEvent() {
        when(studentClient.getStudent(STUDENT)).thenReturn(student("ACTIVE"));
        when(enrollmentRepository.findByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(Optional.empty());
        when(enrollmentRepository.saveAndFlush(any(Enrollment.class))).thenAnswer(inv -> {
            Enrollment e = inv.getArgument(0);
            e.setId(10L);
            e.setEnrolledAt(LocalDateTime.now());
            return e;
        });

        EnrollmentDTO.Response response = service.enroll(STUDENT, COURSE);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(Enrollment.Status.ENROLLED);
        verify(courseClient).reserveSeat(COURSE);
        verify(courseClient, never()).releaseSeat(any());
        verify(eventPublisher).publish(any(EnrollmentEvent.class));
    }

    @Test
    void enroll_studentNotFound_throws404_andNeverTouchesCourse() {
        when(studentClient.getStudent(STUDENT)).thenThrow(new RemoteNotFoundException("not found"));

        assertThatThrownBy(() -> service.enroll(STUDENT, COURSE)).isInstanceOf(StudentNotFoundException.class);
        verifyNoInteractions(courseClient);
    }

    @Test
    void enroll_inactiveStudent_throwsBusinessRule_andNeverTouchesCourse() {
        when(studentClient.getStudent(STUDENT)).thenReturn(student("SUSPENDED"));

        assertThatThrownBy(() -> service.enroll(STUDENT, COURSE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");
        verifyNoInteractions(courseClient);
    }

    @Test
    void enroll_alreadyEnrolled_throwsDuplicate_andNeverReservesSeat() {
        when(studentClient.getStudent(STUDENT)).thenReturn(student("ACTIVE"));
        when(enrollmentRepository.findByStudentIdAndCourseId(STUDENT, COURSE))
                .thenReturn(Optional.of(row(10L, Enrollment.Status.ENROLLED)));

        assertThatThrownBy(() -> service.enroll(STUDENT, COURSE)).isInstanceOf(DuplicateEnrollmentException.class);
        verifyNoInteractions(courseClient);
    }

    @Test
    void enroll_courseFull_throwsBusinessRule_andSavesNothing() {
        when(studentClient.getStudent(STUDENT)).thenReturn(student("ACTIVE"));
        when(enrollmentRepository.findByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(Optional.empty());
        when(courseClient.reserveSeat(COURSE)).thenThrow(new RemoteConflictException("Course CS499 is full"));

        assertThatThrownBy(() -> service.enroll(STUDENT, COURSE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Course CS499 is full");
        verify(enrollmentRepository, never()).saveAndFlush(any(Enrollment.class));
        verify(courseClient, never()).releaseSeat(any());   // nothing was reserved, nothing to give back
    }

    @Test
    void enroll_courseNotFound_throws404() {
        when(studentClient.getStudent(STUDENT)).thenReturn(student("ACTIVE"));
        when(enrollmentRepository.findByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(Optional.empty());
        when(courseClient.reserveSeat(COURSE)).thenThrow(new RemoteNotFoundException("not found"));

        assertThatThrownBy(() -> service.enroll(STUDENT, COURSE)).isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void enroll_courseServiceDown_propagates503_andSavesNothing() {
        when(studentClient.getStudent(STUDENT)).thenReturn(student("ACTIVE"));
        when(enrollmentRepository.findByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(Optional.empty());
        when(courseClient.reserveSeat(COURSE)).thenThrow(new ServiceUnavailableException("Course service is currently unavailable", null));

        assertThatThrownBy(() -> service.enroll(STUDENT, COURSE)).isInstanceOf(ServiceUnavailableException.class);
        verify(enrollmentRepository, never()).saveAndFlush(any(Enrollment.class));
    }

    @Test
    void enroll_lostTheRace_throwsDuplicate_andReleasesTheSeat() {
        when(studentClient.getStudent(STUDENT)).thenReturn(student("ACTIVE"));
        when(enrollmentRepository.findByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(Optional.empty());
        when(enrollmentRepository.saveAndFlush(any(Enrollment.class)))
                .thenThrow(new DataIntegrityViolationException("uk_enrollment_student_course"));

        assertThatThrownBy(() -> service.enroll(STUDENT, COURSE)).isInstanceOf(DuplicateEnrollmentException.class);
        verify(courseClient).reserveSeat(COURSE);
        verify(courseClient).releaseSeat(COURSE);
    }

    @Test
    void enroll_afterDrop_reactivatesTheSameRow() {
        Enrollment dropped = row(10L, Enrollment.Status.DROPPED);
        Enrollment reactivated = row(10L, Enrollment.Status.ENROLLED);
        when(studentClient.getStudent(STUDENT)).thenReturn(student("ACTIVE"));
        when(enrollmentRepository.findByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(Optional.of(dropped));
        when(enrollmentRepository.reactivate(eq(10L), eq(Enrollment.Status.DROPPED),
                eq(Enrollment.Status.ENROLLED), any(LocalDateTime.class))).thenReturn(1);
        when(enrollmentRepository.findById(10L)).thenReturn(Optional.of(reactivated));

        EnrollmentDTO.Response response = service.enroll(STUDENT, COURSE);

        assertThat(response.getStatus()).isEqualTo(Enrollment.Status.ENROLLED);
        verify(courseClient).reserveSeat(COURSE);
        verify(enrollmentRepository, never()).saveAndFlush(any(Enrollment.class));
    }

    @Test
    void enroll_reactivateLostToAnotherRequest_throwsDuplicate_andReleasesTheSeat() {
        when(studentClient.getStudent(STUDENT)).thenReturn(student("ACTIVE"));
        when(enrollmentRepository.findByStudentIdAndCourseId(STUDENT, COURSE))
                .thenReturn(Optional.of(row(10L, Enrollment.Status.DROPPED)));
        when(enrollmentRepository.reactivate(eq(10L), eq(Enrollment.Status.DROPPED),
                eq(Enrollment.Status.ENROLLED), any(LocalDateTime.class))).thenReturn(0);

        assertThatThrownBy(() -> service.enroll(STUDENT, COURSE)).isInstanceOf(DuplicateEnrollmentException.class);
        verify(courseClient).releaseSeat(COURSE);
    }

    // ---------------------------------------------------------------- drop

    @Test
    void drop_success_releasesTheSeat() {
        Enrollment enrolled = row(10L, Enrollment.Status.ENROLLED);
        when(enrollmentRepository.findById(10L)).thenReturn(Optional.of(enrolled));
        when(enrollmentRepository.drop(eq(10L), eq(Enrollment.Status.ENROLLED),
                eq(Enrollment.Status.DROPPED), any(LocalDateTime.class))).thenReturn(1);

        service.drop(10L);

        verify(courseClient).releaseSeat(COURSE);
        verify(eventPublisher).publish(any(EnrollmentEvent.class));
    }

    @Test
    void drop_alreadyDropped_throwsBusinessRule_andDoesNotReleaseASecondSeat() {
        when(enrollmentRepository.findById(10L)).thenReturn(Optional.of(row(10L, Enrollment.Status.DROPPED)));
        when(enrollmentRepository.drop(eq(10L), eq(Enrollment.Status.ENROLLED),
                eq(Enrollment.Status.DROPPED), any(LocalDateTime.class))).thenReturn(0);

        assertThatThrownBy(() -> service.drop(10L)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(courseClient);
    }

    @Test
    void drop_seatReleaseFails_stillSucceeds() {
        when(enrollmentRepository.findById(10L)).thenReturn(Optional.of(row(10L, Enrollment.Status.ENROLLED)));
        when(enrollmentRepository.drop(eq(10L), eq(Enrollment.Status.ENROLLED),
                eq(Enrollment.Status.DROPPED), any(LocalDateTime.class))).thenReturn(1);
        when(courseClient.releaseSeat(COURSE))
                .thenThrow(new ServiceUnavailableException("Course service is currently unavailable", null));

        assertThatCode(() -> service.drop(10L)).doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------- check

    @Test
    void check_droppedRow_isNotEnrolled_butReportsTheRow() {
        when(enrollmentRepository.findByStudentIdAndCourseId(STUDENT, COURSE))
                .thenReturn(Optional.of(row(10L, Enrollment.Status.DROPPED)));

        EnrollmentDTO.CheckResponse result = service.check(STUDENT, COURSE);

        assertThat(result.isEnrolled()).isFalse();
        assertThat(result.getEnrollmentId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(Enrollment.Status.DROPPED);
    }

    @Test
    void check_noRow_isNotEnrolled() {
        when(enrollmentRepository.findByStudentIdAndCourseId(STUDENT, COURSE)).thenReturn(Optional.empty());

        assertThat(service.check(STUDENT, COURSE).isEnrolled()).isFalse();
    }
}
