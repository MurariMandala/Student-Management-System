package com.studentmgmt.course;

import com.studentmgmt.course.model.Course;
import com.studentmgmt.course.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CourseRepositoryTest {

    @Autowired private CourseRepository courseRepository;

    private Course saveCourse(String code, int capacity, Course.Status status) {
        return courseRepository.save(Course.builder()
                .code(code).title("Test " + code).credits(3)
                .department("Engineering").semester("2026-FALL")
                .capacity(capacity).status(status).build());
    }

    @Test
    void reserveSeat_neverExceedsCapacity() {
        Course course = saveCourse("TS101", 2, Course.Status.OPEN);
        LocalDateTime now = LocalDateTime.now();

        assertThat(courseRepository.reserveSeat(course.getId(), Course.Status.OPEN, now)).isEqualTo(1);
        assertThat(courseRepository.reserveSeat(course.getId(), Course.Status.OPEN, now)).isEqualTo(1);
        assertThat(courseRepository.reserveSeat(course.getId(), Course.Status.OPEN, now)).isZero();   // full

        assertThat(courseRepository.findById(course.getId()).orElseThrow().getEnrolledCount()).isEqualTo(2);
    }

    @Test
    void reserveSeat_rejectsCourseThatIsNotOpen() {
        Course course = saveCourse("TS102", 10, Course.Status.CLOSED);

        assertThat(courseRepository.reserveSeat(course.getId(), Course.Status.OPEN, LocalDateTime.now())).isZero();
    }

    @Test
    void releaseSeat_neverGoesBelowZero() {
        Course course = saveCourse("TS103", 5, Course.Status.OPEN);
        LocalDateTime now = LocalDateTime.now();

        courseRepository.reserveSeat(course.getId(), Course.Status.OPEN, now);
        assertThat(courseRepository.releaseSeat(course.getId(), now)).isEqualTo(1);
        assertThat(courseRepository.releaseSeat(course.getId(), now)).isZero();

        assertThat(courseRepository.findById(course.getId()).orElseThrow().getEnrolledCount()).isZero();
    }

    @Test
    void findByCodeIgnoreCase_matchesAnyCase() {
        saveCourse("TS104", 5, Course.Status.OPEN);

        assertThat(courseRepository.findByCodeIgnoreCase("ts104")).isPresent();
        assertThat(courseRepository.existsByCodeIgnoreCase("Ts104")).isTrue();
    }
}
