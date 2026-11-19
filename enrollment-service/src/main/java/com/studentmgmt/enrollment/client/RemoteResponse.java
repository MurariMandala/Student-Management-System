package com.studentmgmt.enrollment.client;

/** The { success, message, data } envelope used by the student and course services. */
public record RemoteResponse<T>(boolean success, String message, T data) {
}
