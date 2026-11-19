package com.studentmgmt.course;

import com.studentmgmt.course.dto.CourseDTO;
import com.studentmgmt.course.exception.CourseNotFoundException;
import com.studentmgmt.course.exception.DuplicateCourseException;
import com.studentmgmt.course.exception.OperationNotAllowedException;
import com.studentmgmt.course.exception.SeatUnavailableException;
import com.studentmgmt.course.model.Course;
import com.studentmgmt.course.repository.CourseRepository;
import com.studentmgmt.course.service.CourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseRepository courseRepository;
    @InjectMocks private CourseServiceImpl courseService;

    private CourseDTO.Request request;
    private Course course;

    @BeforeEach
    void setUp() {
        request = CourseDTO.Request.builder()
                .code("cs101").title("Intro to Programming").credits(4)
                .department("Engineering").semester("2026-fall")
                .capacity(60).build();

        course = Course.builder()
                .id(1L).code("CS101").title("Intro to Programming").credits(4)
                .department("Engineering").semester("2026-FALL")
                .capacity(60).enrolledCount(30).status(Course.Status.OPEN).build();
    }

    @Test
    void createCourse_normalizesCodeAndDefaultsToOpen() {
        when(courseRepository.existsByCodeIgnoreCase(anyString())).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CourseDTO.Response response = courseService.createCourse(request);

        assertThat(response.getCode()).isEqualTo("CS101");
        assertThat(response.getSemester()).isEqualTo("2026-FALL");
        assertThat(response.getStatus()).isEqualTo(Course.Status.OPEN);
        assertThat(response.getEnrolledCount()).isZero();
        assertThat(response.getAvailableSeats()).isEqualTo(60);
    }

    @Test
    void createCourse_duplicateCode_throws() {
        when(courseRepository.existsByCodeIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> courseService.createCourse(request))
                .isInstanceOf(DuplicateCourseException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void createCourse_endBeforeStart_throws() {
        when(courseRepository.existsByCodeIgnoreCase(anyString())).thenReturn(false);
        request.setStartDate(LocalDate.of(2026, 12, 1));
        request.setEndDate(LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> courseService.createCourse(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getCourseById_notFound_throws() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourseById(99L))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void updateCourse_capacityBelowEnrollment_throws() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        request.setCode("CS101");
        request.setCapacity(10);   // 30 students are already enrolled

        assertThatThrownBy(() -> courseService.updateCourse(1L, request))
                .isInstanceOf(OperationNotAllowedException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void deleteCourse_withEnrollments_throws() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.deleteCourse(1L))
                .isInstanceOf(OperationNotAllowedException.class);
        verify(courseRepository, never()).delete(any());
    }

    @Test
    void deleteCourse_empty_deletes() {
        course.setEnrolledCount(0);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        courseService.deleteCourse(1L);

        verify(courseRepository).delete(course);
    }

    @Test
    void reserveSeat_success() {
        when(courseRepository.reserveSeat(eq(1L), eq(Course.Status.OPEN), any(LocalDateTime.class))).thenReturn(1);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        CourseDTO.Response response = courseService.reserveSeat(1L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void reserveSeat_courseFull_throwsConflict() {
        course.setEnrolledCount(course.getCapacity());
        when(courseRepository.reserveSeat(eq(1L), eq(Course.Status.OPEN), any(LocalDateTime.class))).thenReturn(0);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.reserveSeat(1L))
                .isInstanceOf(SeatUnavailableException.class)
                .hasMessageContaining("full");
    }

    @Test
    void reserveSeat_courseNotOpen_throwsConflict() {
        course.setStatus(Course.Status.CLOSED);
        when(courseRepository.reserveSeat(eq(1L), eq(Course.Status.OPEN), any(LocalDateTime.class))).thenReturn(0);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.reserveSeat(1L))
                .isInstanceOf(SeatUnavailableException.class)
                .hasMessageContaining("not open");
    }

    @Test
    void reserveSeat_courseMissing_throwsNotFound() {
        when(courseRepository.reserveSeat(eq(99L), eq(Course.Status.OPEN), any(LocalDateTime.class))).thenReturn(0);
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.reserveSeat(99L))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void releaseSeat_nothingReserved_throwsConflict() {
        course.setEnrolledCount(0);
        when(courseRepository.releaseSeat(eq(1L), any(LocalDateTime.class))).thenReturn(0);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.releaseSeat(1L))
                .isInstanceOf(SeatUnavailableException.class);
    }
}
