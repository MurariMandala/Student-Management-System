package com.studentmgmt.enrollment.dto;

import com.studentmgmt.enrollment.client.CourseDto;
import com.studentmgmt.enrollment.model.Enrollment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;

public class EnrollmentDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotNull(message = "studentId is required")
        @Positive(message = "studentId must be positive")
        private Long studentId;

        @NotNull(message = "courseId is required")
        @Positive(message = "courseId must be positive")
        private Long courseId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long studentId;
        private Long courseId;
        private Enrollment.Status status;
        private LocalDateTime enrolledAt;
        private LocalDateTime droppedAt;
        private LocalDateTime completedAt;

        public static Response fromEntity(Enrollment e) {
            return Response.builder()
                    .id(e.getId())
                    .studentId(e.getStudentId())
                    .courseId(e.getCourseId())
                    .status(e.getStatus())
                    .enrolledAt(e.getEnrolledAt())
                    .droppedAt(e.getDroppedAt())
                    .completedAt(e.getCompletedAt())
                    .build();
        }
    }

    /** Answer to "is this student enrolled in this course?". enrolled is true only for status ENROLLED. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CheckResponse {
        private boolean enrolled;
        private Long enrollmentId;
        private Enrollment.Status status;
    }

    /** An enrollment joined with the course details fetched from the course-service. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrolledCourse {
        private Long enrollmentId;
        private Enrollment.Status status;
        private LocalDateTime enrolledAt;
        /** Null if the course-service no longer knows this course id. */
        private CourseDto course;
    }
}
