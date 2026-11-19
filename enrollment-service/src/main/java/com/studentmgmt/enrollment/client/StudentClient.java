package com.studentmgmt.enrollment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * name is the Eureka service name. url is empty by default, so the call is load-balanced through
 * Eureka. Set STUDENT_SERVICE_URL to call a fixed address instead, for example when running without Eureka.
 */
@FeignClient(name = "student-service",
             url = "${clients.student-service.url:}",
             path = "/api/students",
             fallbackFactory = StudentClientFallbackFactory.class)
public interface StudentClient {

    @GetMapping("/{id}")
    RemoteResponse<StudentDto> getStudent(@PathVariable("id") Long id);
}
