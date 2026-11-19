package com.student.management.controller;


import com.student.management.dto.ApiResponse;
import com.student.management.dto.StudentDTO;
import com.student.management.model.Student;
import com.student.management.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StudentController {

    private final StudentService studentService;

    // ── CREATE ─────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<StudentDTO.Response>> createStudent(
            @Valid @RequestBody StudentDTO.Request request) {
        StudentDTO.Response response = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student created successfully", response));
    }

    // ── READ ───────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<Page<StudentDTO.Response>>> getAllStudents(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success("Students retrieved", studentService.getAllStudents(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentDTO.Response>> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Student retrieved", studentService.getStudentById(id)));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<StudentDTO.Response>> getByEmail(@PathVariable String email) {
        return ResponseEntity.ok(ApiResponse.success("Student retrieved", studentService.getStudentByEmail(email)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<StudentDTO.Response>>> searchStudents(
            @RequestParam String query,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("Search results", studentService.searchStudents(query, pageable)));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<StudentDTO.Response>>> filterStudents(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Student.Status status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("Filtered results",
                studentService.filterStudents(department, year, status, pageable)));
    }

    @GetMapping("/top")
    public ResponseEntity<ApiResponse<List<StudentDTO.Response>>> getTopStudents(
            @RequestParam(defaultValue = "8.0") double minGpa) {
        return ResponseEntity.ok(ApiResponse.success("Top students", studentService.getTopStudents(minGpa)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<StudentDTO.Summary>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success("Summary retrieved", studentService.getSummary()));
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentDTO.Response>> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentDTO.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Student updated", studentService.updateStudent(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<StudentDTO.Response>> updateStatus(
            @PathVariable Long id,
            @RequestParam Student.Status status) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", studentService.updateStudentStatus(id, status)));
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(ApiResponse.success("Student deleted", null));
    }
}
