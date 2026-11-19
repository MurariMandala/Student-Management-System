package com.studentmgmt.enrollment;

import com.studentmgmt.enrollment.controller.EnrollmentController;
import com.studentmgmt.enrollment.dto.EnrollmentDTO;
import com.studentmgmt.enrollment.exception.BusinessRuleException;
import com.studentmgmt.enrollment.exception.DuplicateEnrollmentException;
import com.studentmgmt.enrollment.exception.ServiceUnavailableException;
import com.studentmgmt.enrollment.exception.StudentNotFoundException;
import com.studentmgmt.enrollment.model.Enrollment;
import com.studentmgmt.enrollment.service.EnrollmentService;
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

@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

    private static final String BODY = "{\"studentId\":1,\"courseId\":2}";

    @Autowired private MockMvc mockMvc;
    @MockBean private EnrollmentService enrollmentService;

    private EnrollmentDTO.Response created() {
        return EnrollmentDTO.Response.builder().id(10L).studentId(1L).courseId(2L)
                .status(Enrollment.Status.ENROLLED).build();
    }

    @Test
    void enroll_success_returns201() throws Exception {
        when(enrollmentService.enroll(1L, 2L)).thenReturn(created());

        mockMvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("ENROLLED"));
    }

    @Test
    void enroll_missingFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.studentId").exists())
                .andExpect(jsonPath("$.data.courseId").exists());
    }

    @Test
    void enroll_duplicate_returns409() throws Exception {
        when(enrollmentService.enroll(1L, 2L)).thenThrow(new DuplicateEnrollmentException(1L, 2L));

        mockMvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Student 1 is already enrolled in course 2"));
    }

    @Test
    void enroll_courseFull_returns409WithRemoteMessage() throws Exception {
        when(enrollmentService.enroll(1L, 2L)).thenThrow(new BusinessRuleException("Course CS499 is full"));

        mockMvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Course CS499 is full"));
    }

    @Test
    void enroll_unknownStudent_returns404() throws Exception {
        when(enrollmentService.enroll(1L, 2L)).thenThrow(new StudentNotFoundException(1L));

        mockMvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void enroll_downstreamDown_returns503() throws Exception {
        when(enrollmentService.enroll(1L, 2L))
                .thenThrow(new ServiceUnavailableException("Course service is currently unavailable", null));

        mockMvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void check_returnsEnrolledFlag() throws Exception {
        when(enrollmentService.check(1L, 2L)).thenReturn(EnrollmentDTO.CheckResponse.builder()
                .enrolled(true).enrollmentId(10L).status(Enrollment.Status.ENROLLED).build());

        mockMvc.perform(get("/api/enrollments/check").param("studentId", "1").param("courseId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enrolled").value(true))
                .andExpect(jsonPath("$.data.enrollmentId").value(10));
    }

    @Test
    void check_missingParameter_returns400() throws Exception {
        mockMvc.perform(get("/api/enrollments/check").param("studentId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'courseId' is missing"));
    }

    @Test
    void drop_wrongHttpMethod_returns405() throws Exception {
        mockMvc.perform(get("/api/enrollments/10/drop"))
                .andExpect(status().isMethodNotAllowed());
    }
}
