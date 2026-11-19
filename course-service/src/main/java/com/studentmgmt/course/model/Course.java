package com.studentmgmt.course.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @DynamicUpdate makes Hibernate write only the columns that changed. Without it, editing a
 * course's title would also write back a stale enrolledCount and could overwrite a seat
 * reservation made a moment earlier by another request.
 */
@Entity
@Table(name = "courses", indexes = {
        @Index(name = "idx_courses_department", columnList = "department"),
        @Index(name = "idx_courses_semester", columnList = "semester"),
        @Index(name = "idx_courses_status", columnList = "status")
})
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private int credits;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 30)
    private String semester;

    /** Reference to a teacher in the faculty-service. Plain id, no cross-service foreign key. */
    private Long instructorId;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    @Builder.Default
    private int enrolledCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.OPEN;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Status {
        DRAFT, OPEN, CLOSED, CANCELLED
    }
}
