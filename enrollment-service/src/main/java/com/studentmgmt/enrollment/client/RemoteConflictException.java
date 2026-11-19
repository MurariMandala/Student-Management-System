package com.studentmgmt.enrollment.client;

/** The remote service answered 409, for example "course is full". A normal business answer, not an outage. */
public class RemoteConflictException extends RuntimeException {
    public RemoteConflictException(String message) {
        super(message);
    }
}
