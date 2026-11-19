package com.student.management.service;

import com.student.management.dto.StudentDTO;
import com.student.management.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;

public interface StudentService {
    StudentDTO.Response createStudent(StudentDTO.Request request);
    StudentDTO.Response getStudentById(Long id);
    StudentDTO.Response getStudentByEmail(String email);
    Page<StudentDTO.Response> getAllStudents(Pageable pageable);
    Page<StudentDTO.Response> searchStudents(String query, Pageable pageable);
    Page<StudentDTO.Response> filterStudents(String department, Integer year, Student.Status status, Pageable pageable);
    StudentDTO.Response updateStudent(Long id, StudentDTO.Request request);
    StudentDTO.Response updateStudentStatus(Long id, Student.Status status);
    void deleteStudent(Long id);
    StudentDTO.Summary getSummary();
    List<StudentDTO.Response> getTopStudents(double minGpa);
}
