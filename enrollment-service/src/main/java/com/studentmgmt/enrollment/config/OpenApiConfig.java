package com.studentmgmt.enrollment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI enrollmentServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Enrollment Service API")
                .version("1.0.0")
                .description("""
                        Owns the student-course link. Enrolling checks the student with the student-service
                        and reserves a seat with the course-service.

                        Every response uses the envelope: { success, message, data, timestamp }.
                        On errors success is false and data is absent, except validation errors (400),
                        where data maps each invalid field to its message.

                        Status codes: 200 OK, 201 Created, 400 bad input, 404 not found,
                        405 wrong method, 409 business-rule conflict, 415 wrong content type,
                        503 a downstream service is unavailable, 500 unexpected error.
                        """));
    }
}
