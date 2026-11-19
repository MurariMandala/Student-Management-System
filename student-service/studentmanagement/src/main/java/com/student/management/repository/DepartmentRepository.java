package com.student.management.repository;

import com.student.management.model.Department;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByDepartmentName(String departmentName);
}
