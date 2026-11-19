package com.studentmgmt.course.exception;

/** Thrown when a seat cannot be reserved or released. Mapped to HTTP 409. */
public class SeatUnavailableException extends RuntimeException {
    public SeatUnavailableException(String message) {
        super(message);
    }
}
