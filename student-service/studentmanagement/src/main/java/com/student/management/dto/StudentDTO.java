package com.student.management.dto;

import com.student.management.model.Student;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class StudentDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 50)
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 50)
        private String lastName;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[+]?[0-9]{10,15}$")
        private String phone;

        @NotNull(message = "Date of birth is required")
        @Past
        private LocalDate dateOfBirth;

        @NotBlank(message = "Course is required")
        private String course;

        @NotBlank(message = "Department is required")
        private String department;

        @Min(1) @Max(6)
        private int year;

        @DecimalMin("0.0") @DecimalMax("10.0")
        private Double gpa;

        private Student.Status status;
        private String address;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private LocalDate dateOfBirth;
        private String course;
        private String department;
        private int year;
        private Double gpa;
        private Student.Status status;
        private String address;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private int age;

        public static Response fromEntity(Student s) {
            return Response.builder()
                    .id(s.getId())
                    .firstName(s.getFirstName())
                    .lastName(s.getLastName())
                    .email(s.getEmail())
                    .phone(s.getPhone())
                    .dateOfBirth(s.getDateOfBirth())
                    .course(s.getCourse())
                    .department(s.getDepartment())
                    .year(s.getYear())
                    .gpa(s.getGpa())
                    .status(s.getStatus())
                    .address(s.getAddress())
                    .createdAt(s.getCreatedAt())
                    .updatedAt(s.getUpdatedAt())
                    .age(s.getDateOfBirth() != null
                            ? java.time.Period.between(s.getDateOfBirth(), LocalDate.now()).getYears()
                            : 0)
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private long totalStudents;
        private long activeStudents;
        private long graduatedStudents;
        private long inactiveStudents;
        private long suspendedStudents;
        private Double averageGpa;
        private java.util.Map<String, Long> studentsByDepartment;
        private java.util.Map<String, Long> studentsByYear;
    }
}
