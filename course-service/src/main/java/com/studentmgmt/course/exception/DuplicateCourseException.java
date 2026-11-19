package com.studentmgmt.course.exception;

public class DuplicateCourseException extends RuntimeException {
    public DuplicateCourseException(String code) {
        super("A course with code '" + code + "' already exists");
    }
}
