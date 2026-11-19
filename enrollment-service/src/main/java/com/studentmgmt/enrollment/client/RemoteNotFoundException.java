package com.studentmgmt.enrollment.client;

/** The remote service answered 404. A normal business answer, not an outage. */
public class RemoteNotFoundException extends RuntimeException {
    public RemoteNotFoundException(String message) {
        super(message);
    }
}
