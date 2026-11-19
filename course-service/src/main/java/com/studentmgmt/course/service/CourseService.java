package com.studentmgmt.course.service;

import com.studentmgmt.course.dto.CourseDTO;
import com.studentmgmt.course.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CourseService {
    CourseDTO.Response createCourse(CourseDTO.Request request);

    CourseDTO.Response getCourseById(Long id);

    CourseDTO.Response getCourseByCode(String code);

    List<CourseDTO.Response> getCoursesByIds(List<Long> ids);

    Page<CourseDTO.Response> getCourses(String keyword, String department, String semester,
                                        Course.Status status, boolean availableOnly, Pageable pageable);

    CourseDTO.Response updateCourse(Long id, CourseDTO.Request request);

    CourseDTO.Response updateStatus(Long id, Course.Status status);

    void deleteCourse(Long id);

    CourseDTO.Response reserveSeat(Long id);

    CourseDTO.Response releaseSeat(Long id);

    CourseDTO.Summary getSummary();
}
