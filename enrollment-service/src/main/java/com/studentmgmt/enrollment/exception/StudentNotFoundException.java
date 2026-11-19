package com.studentmgmt.enrollment.exception;

/** The student-service answered 404 for this id. */
public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(Long id) {
        super("Student not found with id: " + id);
    }
}
