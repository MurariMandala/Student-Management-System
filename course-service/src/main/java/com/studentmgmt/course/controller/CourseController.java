package com.studentmgmt.course.controller;

import com.studentmgmt.course.dto.ApiResponse;
import com.studentmgmt.course.dto.CourseDTO;
import com.studentmgmt.course.model.Course;
import com.studentmgmt.course.service.CourseService;
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

/**
 * No @CrossOrigin here on purpose. CORS is configured once, at the API gateway.
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private static final Set<String> SORTABLE = Set.of(
            "id", "code", "title", "credits", "department", "semester",
            "capacity", "enrolledCount", "startDate", "createdAt");

    private final CourseService courseService;

    // ---------------------------------------------------------------- CRUD

    @PostMapping
    public ResponseEntity<ApiResponse<CourseDTO.Response>> create(@Valid @RequestBody CourseDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Course created", courseService.createCourse(request)));
    }

    /** One flexible list endpoint. Every filter is optional. */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CourseDTO.Response>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) Course.Status status,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        String safeSort = SORTABLE.contains(sortBy) ? sortBy : "id";
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(safeSort).descending() : Sort.by(safeSort).ascending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);

        return ResponseEntity.ok(ApiResponse.success("Courses retrieved",
                courseService.getCourses(keyword, department, semester, status, availableOnly, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseDTO.Response>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Course retrieved", courseService.getCourseById(id)));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<CourseDTO.Response>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success("Course retrieved", courseService.getCourseByCode(code)));
    }

    /** Bulk lookup for other services, e.g. GET /api/courses/batch?ids=1,2,3 */
    @GetMapping("/batch")
    public ResponseEntity<ApiResponse<List<CourseDTO.Response>>> getBatch(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved", courseService.getCoursesByIds(ids)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<CourseDTO.Summary>> summary() {
        return ResponseEntity.ok(ApiResponse.success("Summary retrieved", courseService.getSummary()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseDTO.Response>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody CourseDTO.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Course updated", courseService.updateCourse(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CourseDTO.Response>> updateStatus(@PathVariable Long id,
                                                                        @RequestParam Course.Status status) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", courseService.updateStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.success("Course deleted", null));
    }

    // ---------------------------------------------------------------- seats (used by enrollment-service)

    @PostMapping("/{id}/reserve-seat")
    public ResponseEntity<ApiResponse<CourseDTO.Response>> reserveSeat(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Seat reserved", courseService.reserveSeat(id)));
    }

    @PostMapping("/{id}/release-seat")
    public ResponseEntity<ApiResponse<CourseDTO.Response>> releaseSeat(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Seat released", courseService.releaseSeat(id)));
    }
}
