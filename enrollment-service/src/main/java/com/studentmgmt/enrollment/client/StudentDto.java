package com.studentmgmt.enrollment.client;

/**
 * Only the fields enrollment needs. Do not share entity classes between services. Unknown JSON fields
 * from the student-service are ignored. status stays a String so the enum need not be copied.
 */
public record StudentDto(Long id, String firstName, String lastName, String email, String status) {
}
