package com.studentmgmt.course.exception;

/** Thrown when a change would break a business rule. Mapped to HTTP 409. */
public class OperationNotAllowedException extends RuntimeException {
    public OperationNotAllowedException(String message) {
        super(message);
    }
}
