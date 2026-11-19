package com.studentmgmt.enrollment.client;

public record CourseDto(Long id, String code, String title, String department, String semester,
                        int credits, int capacity, int enrolledCount, int availableSeats,
                        boolean full, String status) {
}
