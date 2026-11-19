package com.studentmgmt.enrollment.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Active unless app.events.publisher is set to something else (for example "kafka"). */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.events.publisher", havingValue = "log", matchIfMissing = true)
public class LoggingEnrollmentEventPublisher implements EnrollmentEventPublisher {

    @Override
    public void publish(EnrollmentEvent event) {
        log.info("EVENT {}", event);
    }
}
