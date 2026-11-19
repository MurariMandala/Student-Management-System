package com.studentmgmt.enrollment.event;

import java.time.LocalDateTime;

public record EnrollmentEvent(Type type, Long enrollmentId, Long studentId, Long courseId,
                              LocalDateTime occurredAt) {

    public enum Type {
        STUDENT_ENROLLED, STUDENT_DROPPED, ENROLLMENT_COMPLETED
    }
}
