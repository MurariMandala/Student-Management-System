package com.studentmgmt.enrollment;

import com.studentmgmt.enrollment.client.CourseClient;
import com.studentmgmt.enrollment.client.RemoteResponse;
import com.studentmgmt.enrollment.client.StudentClient;
import com.studentmgmt.enrollment.client.StudentDto;
import com.studentmgmt.enrollment.exception.DuplicateEnrollmentException;
import com.studentmgmt.enrollment.repository.EnrollmentRepository;
import com.studentmgmt.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Proves the design: eight simultaneous enrolls of the SAME student in the SAME course produce exactly
 * one row, and every seat that was reserved but not used is given back.
 */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.openfeign.circuitbreaker.enabled=false"
})
class EnrollmentConcurrencyTest {

    @Autowired private EnrollmentService service;
    @Autowired private EnrollmentRepository repository;
    @MockBean private StudentClient studentClient;
    @MockBean private CourseClient courseClient;

    private long count(String methodName) {
        return Mockito.mockingDetails(courseClient).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals(methodName))
                .count();
    }

    @Test
    void parallelEnrollOfTheSamePair_createsOneRow_andLeaksNoSeats() throws Exception {
        when(studentClient.getStudent(5L)).thenReturn(new RemoteResponse<>(true, "ok",
                new StudentDto(5L, "Asha", "Rao", "asha@uni.edu", "ACTIVE")));

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                startGun.await();
                try {
                    service.enroll(5L, 3L);
                    return true;
                } catch (DuplicateEnrollmentException e) {
                    return false;
                }
            }));
        }
        startGun.countDown();

        long winners = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                winners++;
            }
        }
        pool.shutdown();

        assertThat(winners).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);
        // The number of reserves varies from run to run (late threads are stopped by the cheap duplicate
        // check before they reserve anything), but reserves minus releases must always be exactly one.
        assertThat(count("reserveSeat") - count("releaseSeat")).isEqualTo(1);
    }
}
