package com.studentmgmt.enrollment.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One row per (student, course) pair. The UNIQUE constraint is what declares
 * "this student is enrolled in this course" and what stops duplicates under concurrency.
 *
 * studentId and courseId are plain ids, not @ManyToOne: the students and courses tables live in
 * other services' databases. The link is logical only, enforced in code by calling those services.
 */
@Entity
@Table(name = "enrollments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_enrollment_student_course",
                columnNames = {"student_id", "course_id"}),
        indexes = {
                @Index(name = "idx_enrollment_student", columnList = "student_id"),
                @Index(name = "idx_enrollment_course", columnList = "course_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ENROLLED;

    @Column(nullable = false)
    private LocalDateTime enrolledAt;

    private LocalDateTime droppedAt;

    private LocalDateTime completedAt;

    public Enrollment(Long studentId, Long courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
    }

    @PrePersist
    void onCreate() {
        if (enrolledAt == null) {
            enrolledAt = LocalDateTime.now();
        }
    }

    public enum Status {
        ENROLLED, DROPPED, COMPLETED
    }
}
