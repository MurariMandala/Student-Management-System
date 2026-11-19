package com.studentmgmt.enrollment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "course-service",
             url = "${clients.course-service.url:}",
             path = "/api/courses",
             fallbackFactory = CourseClientFallbackFactory.class)
public interface CourseClient {

    @GetMapping("/{id}")
    RemoteResponse<CourseDto> getCourse(@PathVariable("id") Long id);

    /** 404 if the course does not exist, 409 if it is full or not open. */
    @PostMapping("/{id}/reserve-seat")
    RemoteResponse<CourseDto> reserveSeat(@PathVariable("id") Long id);

    @PostMapping("/{id}/release-seat")
    RemoteResponse<CourseDto> releaseSeat(@PathVariable("id") Long id);

    /** Ids that do not exist are silently left out of the result. */
    @GetMapping("/batch")
    RemoteResponse<List<CourseDto>> getBatch(@RequestParam("ids") List<Long> ids);
}
