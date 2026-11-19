package com.studentmgmt.course.repository;

import com.studentmgmt.course.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    Optional<Course> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    long countByStatus(Course.Status status);

    @Query("SELECT COALESCE(SUM(c.capacity), 0) FROM Course c")
    Long sumCapacity();

    @Query("SELECT COALESCE(SUM(c.enrolledCount), 0) FROM Course c")
    Long sumEnrolled();

    @Query("SELECT c.department, COUNT(c) FROM Course c GROUP BY c.department ORDER BY c.department")
    List<Object[]> countByDepartment();

    /**
     * Atomic seat reservation. The capacity and status checks and the increment happen in ONE SQL
     * statement, so two concurrent requests can never both take the last seat.
     * Returns the number of rows updated: 1 on success, 0 if the course is missing, not open or full.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Course c SET c.enrolledCount = c.enrolledCount + 1, c.updatedAt = :now " +
           "WHERE c.id = :id AND c.status = :status AND c.enrolledCount < c.capacity")
    int reserveSeat(@Param("id") Long id,
                    @Param("status") Course.Status status,
                    @Param("now") LocalDateTime now);

    /** Atomic seat release that never drops the count below zero. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Course c SET c.enrolledCount = c.enrolledCount - 1, c.updatedAt = :now " +
           "WHERE c.id = :id AND c.enrolledCount > 0")
    int releaseSeat(@Param("id") Long id, @Param("now") LocalDateTime now);
}
