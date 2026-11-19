package com.studentmgmt.enrollment.exception;

/** The course-service answered 404 for this id. */
public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(Long id) {
        super("Course not found with id: " + id);
    }
}
