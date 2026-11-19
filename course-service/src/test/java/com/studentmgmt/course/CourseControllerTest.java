package com.studentmgmt.course;

import com.studentmgmt.course.controller.CourseController;
import com.studentmgmt.course.dto.CourseDTO;
import com.studentmgmt.course.exception.CourseNotFoundException;
import com.studentmgmt.course.exception.SeatUnavailableException;
import com.studentmgmt.course.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
class CourseControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CourseService courseService;

    @Test
    void createCourse_invalidBody_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"!!\",\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.code").exists())
                .andExpect(jsonPath("$.data.title").exists());
    }

    @Test
    void getCourse_notFound_returns404() throws Exception {
        when(courseService.getCourseById(99L)).thenThrow(new CourseNotFoundException(99L));

        mockMvc.perform(get("/api/courses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void reserveSeat_success_returns200() throws Exception {
        CourseDTO.Response body = CourseDTO.Response.builder()
                .id(1L).code("CS101").title("Intro").capacity(10).enrolledCount(4).build();
        when(courseService.reserveSeat(1L)).thenReturn(body);

        mockMvc.perform(post("/api/courses/1/reserve-seat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enrolledCount").value(4));
    }

    @Test
    void reserveSeat_courseFull_returns409() throws Exception {
        when(courseService.reserveSeat(1L)).thenThrow(new SeatUnavailableException("Course CS101 is full"));

        mockMvc.perform(post("/api/courses/1/reserve-seat"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Course CS101 is full"));
    }

    @Test
    void list_invalidStatusValue_returns400() throws Exception {
        mockMvc.perform(get("/api/courses").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest());
    }
}
