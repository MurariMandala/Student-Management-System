package com.studentmgmt.enrollment.repository;

import com.studentmgmt.enrollment.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long>, JpaSpecificationExecutor<Enrollment> {

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    boolean existsByStudentIdAndCourseIdAndStatus(Long studentId, Long courseId, Enrollment.Status status);

    long countByCourseIdAndStatus(Long courseId, Enrollment.Status status);

    /*
     * Conditional status changes. Each is ONE SQL statement that only matches a row in the expected
     * state, so when two requests race, exactly one of them sees "1 row updated" and the other sees 0.
     * @Transactional is required here: Spring Data only wraps its built-in methods in a transaction,
     * and the service's enroll()/drop() deliberately run without one because they make remote calls.
     */

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Enrollment e SET e.status = :to, e.enrolledAt = :now, e.droppedAt = NULL, e.completedAt = NULL " +
           "WHERE e.id = :id AND e.status = :from")
    int reactivate(@Param("id") Long id, @Param("from") Enrollment.Status from,
                   @Param("to") Enrollment.Status to, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Enrollment e SET e.status = :to, e.droppedAt = :now " +
           "WHERE e.id = :id AND e.status = :from")
    int drop(@Param("id") Long id, @Param("from") Enrollment.Status from,
             @Param("to") Enrollment.Status to, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Enrollment e SET e.status = :to, e.completedAt = :now " +
           "WHERE e.id = :id AND e.status = :from")
    int complete(@Param("id") Long id, @Param("from") Enrollment.Status from,
                 @Param("to") Enrollment.Status to, @Param("now") LocalDateTime now);
}
