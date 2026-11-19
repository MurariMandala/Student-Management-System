package com.studentmgmt.enrollment.controller;

import com.studentmgmt.enrollment.dto.ApiResponse;
import com.studentmgmt.enrollment.dto.EnrollmentDTO;
import com.studentmgmt.enrollment.model.Enrollment;
import com.studentmgmt.enrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/** No @CrossOrigin on purpose. CORS is configured once, at the API gateway. */
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Who is enrolled in which course")
public class EnrollmentController {

    private static final Set<String> SORTABLE = Set.of("id", "studentId", "courseId", "status", "enrolledAt");

    private final EnrollmentService enrollmentService;

    @Operation(summary = "Enroll a student in a course",
            description = "Checks the student is ACTIVE (student-service), rejects duplicates, reserves a seat "
                    + "(course-service), then stores the link. A previously DROPPED pair is re-enrolled. Returns 201. "
                    + "Errors: 400 validation, 404 student or course not found, 409 duplicate / student not active / "
                    + "course full or not open, 503 a downstream service is unavailable.")
    @PostMapping
    public ResponseEntity<ApiResponse<EnrollmentDTO.Response>> enroll(@Valid @RequestBody EnrollmentDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Enrollment created",
                        enrollmentService.enroll(request.getStudentId(), request.getCourseId())));
    }

    @Operation(summary = "List enrollments",
            description = "All filters optional. size is capped at 100. An unknown sortBy falls back to id. "
                    + "Errors: 400 for an invalid status value.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<EnrollmentDTO.Response>>> list(
            @Parameter(description = "Only this student") @RequestParam(required = false) Long studentId,
            @Parameter(description = "Only this course (the roster)") @RequestParam(required = false) Long courseId,
            @Parameter(description = "ENROLLED, DROPPED or COMPLETED") @RequestParam(required = false) Enrollment.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "id, studentId, courseId, status, enrolledAt") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "asc") String direction) {

        String safeSort = SORTABLE.contains(sortBy) ? sortBy : "id";
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(safeSort).descending() : Sort.by(safeSort).ascending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);

        return ResponseEntity.ok(ApiResponse.success("Enrollments retrieved",
                enrollmentService.list(studentId, courseId, status, pageable)));
    }

    @Operation(summary = "Is this student enrolled in this course?",
            description = "enrolled is true only when the status is ENROLLED. enrollmentId and status are present "
                    + "whenever a row exists, including DROPPED and COMPLETED. Errors: 400 missing parameter.")
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<EnrollmentDTO.CheckResponse>> check(@RequestParam Long studentId,
                                                                          @RequestParam Long courseId) {
        return ResponseEntity.ok(ApiResponse.success("Check complete", enrollmentService.check(studentId, courseId)));
    }

    @Operation(summary = "A student's current courses, with course details",
            description = "ENROLLED rows joined with course details from the course-service (one batch call). "
                    + "course is null if the course-service no longer knows that id. Errors: 503 if the course "
                    + "service is unavailable.")
    @GetMapping("/student/{studentId}/courses")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO.EnrolledCourse>>> coursesOfStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved", enrollmentService.getCoursesOfStudent(studentId)));
    }

    @Operation(summary = "Get an enrollment by id", description = "Errors: 400 id not a number, 404 not found.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EnrollmentDTO.Response>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Enrollment retrieved", enrollmentService.getById(id)));
    }

    @Operation(summary = "Drop an enrollment",
            description = "Soft drop: the row stays with status DROPPED and the seat is released. Only an ENROLLED "
                    + "row can be dropped, and concurrent drops free exactly one seat. Errors: 404 not found, "
                    + "409 not active.")
    @PostMapping("/{id}/drop")
    public ResponseEntity<ApiResponse<EnrollmentDTO.Response>> drop(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Enrollment dropped", enrollmentService.drop(id)));
    }

    @Operation(summary = "Mark an enrollment completed",
            description = "ENROLLED to COMPLETED. The seat stays counted. Errors: 404 not found, 409 not active.")
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<EnrollmentDTO.Response>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Enrollment completed", enrollmentService.complete(id)));
    }
}
