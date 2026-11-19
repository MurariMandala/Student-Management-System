package com.studentmgmt.enrollment.event;

/** Port for publishing domain events. The default implementation logs. Swap in Kafka or RabbitMQ later. */
public interface EnrollmentEventPublisher {
    void publish(EnrollmentEvent event);
}
