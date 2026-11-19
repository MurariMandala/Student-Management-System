package com.student.management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String departmentName;

    @Column(nullable = false)
    private String description;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt = LocalDateTime.now();
}
