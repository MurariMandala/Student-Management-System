package com.studentmgmt.enrollment.service;

import com.studentmgmt.enrollment.dto.EnrollmentDTO;
import com.studentmgmt.enrollment.model.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EnrollmentService {

    EnrollmentDTO.Response enroll(Long studentId, Long courseId);

    EnrollmentDTO.Response getById(Long id);

    Page<EnrollmentDTO.Response> list(Long studentId, Long courseId, Enrollment.Status status, Pageable pageable);

    EnrollmentDTO.CheckResponse check(Long studentId, Long courseId);

    List<EnrollmentDTO.EnrolledCourse> getCoursesOfStudent(Long studentId);

    EnrollmentDTO.Response drop(Long id);

    EnrollmentDTO.Response complete(Long id);
}
