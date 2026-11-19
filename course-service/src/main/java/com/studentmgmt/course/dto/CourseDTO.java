package com.studentmgmt.course.dto;

import com.studentmgmt.course.model.Course;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class CourseDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotBlank(message = "Course code is required")
        @Pattern(regexp = "^[A-Za-z]{2,6}-?[0-9]{2,4}$",
                 message = "Course code must look like CS101 or CS-101")
        private String code;

        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        private String title;

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        private String description;

        @Min(value = 1, message = "Credits must be at least 1")
        @Max(value = 10, message = "Credits must be at most 10")
        private int credits;

        @NotBlank(message = "Department is required")
        @Size(max = 100)
        private String department;

        @NotBlank(message = "Semester is required, e.g. 2026-FALL")
        @Size(max = 30)
        private String semester;

        @Positive(message = "Instructor id must be positive")
        private Long instructorId;

        @Min(value = 1, message = "Capacity must be at least 1")
        @Max(value = 1000, message = "Capacity must be at most 1000")
        private int capacity;

        /** Optional. Defaults to OPEN on create and keeps the current value on update. */
        private Course.Status status;

        private LocalDate startDate;
        private LocalDate endDate;
    }

    /**
     * Field names id, code, title, capacity and enrolledCount match the CourseDto
     * record used by the enrollment-service Feign client.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String code;
        private String title;
        private String description;
        private int credits;
        private String department;
        private String semester;
        private Long instructorId;
        private int capacity;
        private int enrolledCount;
        private int availableSeats;
        private boolean full;
        private Course.Status status;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response fromEntity(Course c) {
            return Response.builder()
                    .id(c.getId())
                    .code(c.getCode())
                    .title(c.getTitle())
                    .description(c.getDescription())
                    .credits(c.getCredits())
                    .department(c.getDepartment())
                    .semester(c.getSemester())
                    .instructorId(c.getInstructorId())
                    .capacity(c.getCapacity())
                    .enrolledCount(c.getEnrolledCount())
                    .availableSeats(Math.max(0, c.getCapacity() - c.getEnrolledCount()))
                    .full(c.getEnrolledCount() >= c.getCapacity())
                    .status(c.getStatus())
                    .startDate(c.getStartDate())
                    .endDate(c.getEndDate())
                    .createdAt(c.getCreatedAt())
                    .updatedAt(c.getUpdatedAt())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private long totalCourses;
        private long openCourses;
        private long totalCapacity;
        private long totalEnrolled;
        private double occupancyPercent;
        private Map<String, Long> coursesByDepartment;
    }
}
