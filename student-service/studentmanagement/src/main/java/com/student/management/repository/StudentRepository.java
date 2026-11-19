package com.student.management.repository;

import com.student.management.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Student> findByStatus(Student.Status status);

    List<Student> findByDepartment(String department);

    List<Student> findByCourse(String course);

    List<Student> findByYear(int year);

    @Query("SELECT s FROM Student s WHERE " +
           "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.course) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.department) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Student> searchStudents(@Param("query") String query, Pageable pageable);

    @Query("SELECT s FROM Student s WHERE " +
           "(:department IS NULL OR s.department = :department) AND " +
           "(:year IS NULL OR s.year = :year) AND " +
           "(:status IS NULL OR s.status = :status)")
    Page<Student> filterStudents(
            @Param("department") String department,
            @Param("year") Integer year,
            @Param("status") Student.Status status,
            Pageable pageable);

    long countByStatus(Student.Status status);

    @Query("SELECT AVG(s.gpa) FROM Student s WHERE s.gpa IS NOT NULL")
    Double findAverageGpa();

    @Query("SELECT s.department, COUNT(s) FROM Student s GROUP BY s.department")
    List<Object[]> countByDepartment();

    @Query("SELECT s.year, COUNT(s) FROM Student s GROUP BY s.year")
    List<Object[]> countByYear();

    @Query("SELECT s FROM Student s WHERE s.gpa >= :minGpa ORDER BY s.gpa DESC")
    List<Student> findTopStudents(@Param("minGpa") double minGpa);
}
