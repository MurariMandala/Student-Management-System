package com.studentmgmt.enrollment;

import com.studentmgmt.enrollment.model.Enrollment;
import com.studentmgmt.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class EnrollmentRepositoryTest {

    @Autowired private EnrollmentRepository repository;

    @Test
    void uniqueConstraint_rejectsTheSameStudentAndCourseTwice() {
        repository.saveAndFlush(new Enrollment(1L, 1L));

        assertThatThrownBy(() -> repository.saveAndFlush(new Enrollment(1L, 1L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniqueConstraint_allowsSameStudentInADifferentCourse_andViceVersa() {
        repository.saveAndFlush(new Enrollment(1L, 1L));
        repository.saveAndFlush(new Enrollment(1L, 2L));
        repository.saveAndFlush(new Enrollment(2L, 1L));

        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    void drop_flipsOnlyAnEnrolledRow_andOnlyOnce() {
        Enrollment saved = repository.saveAndFlush(new Enrollment(1L, 1L));
        LocalDateTime now = LocalDateTime.now();

        assertThat(repository.drop(saved.getId(), Enrollment.Status.ENROLLED, Enrollment.Status.DROPPED, now)).isEqualTo(1);
        assertThat(repository.drop(saved.getId(), Enrollment.Status.ENROLLED, Enrollment.Status.DROPPED, now)).isZero();

        Enrollment reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Enrollment.Status.DROPPED);
        assertThat(reloaded.getDroppedAt()).isNotNull();
    }

    @Test
    void reactivate_onlyWorksFromDropped_andClearsDroppedAt() {
        Enrollment saved = repository.saveAndFlush(new Enrollment(1L, 1L));
        LocalDateTime now = LocalDateTime.now();

        assertThat(repository.reactivate(saved.getId(), Enrollment.Status.DROPPED, Enrollment.Status.ENROLLED, now)).isZero();

        repository.drop(saved.getId(), Enrollment.Status.ENROLLED, Enrollment.Status.DROPPED, now);
        assertThat(repository.reactivate(saved.getId(), Enrollment.Status.DROPPED, Enrollment.Status.ENROLLED, now)).isEqualTo(1);

        Enrollment reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Enrollment.Status.ENROLLED);
        assertThat(reloaded.getDroppedAt()).isNull();
    }

    @Test
    void complete_onlyWorksFromEnrolled() {
        Enrollment saved = repository.saveAndFlush(new Enrollment(1L, 1L));
        LocalDateTime now = LocalDateTime.now();

        assertThat(repository.complete(saved.getId(), Enrollment.Status.ENROLLED, Enrollment.Status.COMPLETED, now)).isEqualTo(1);
        assertThat(repository.complete(saved.getId(), Enrollment.Status.ENROLLED, Enrollment.Status.COMPLETED, now)).isZero();
        assertThat(repository.existsByStudentIdAndCourseIdAndStatus(1L, 1L, Enrollment.Status.COMPLETED)).isTrue();
    }
}
