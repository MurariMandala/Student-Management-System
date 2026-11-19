package com.studentmgmt.enrollment.exception;

/** A business rule said no: inactive student, full course, course not open, enrollment not active. Mapped to 409. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
