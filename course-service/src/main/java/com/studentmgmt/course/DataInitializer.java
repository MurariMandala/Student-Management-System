package com.studentmgmt.course;

import com.studentmgmt.course.model.Course;
import com.studentmgmt.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** Seeds sample data on startup, only when the table is empty. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CourseRepository courseRepository;

    @Override
    public void run(String... args) {
        if (courseRepository.count() > 0) {
            return;
        }
        LocalDate start = LocalDate.of(2026, 8, 17);
        LocalDate end = LocalDate.of(2026, 12, 11);

        courseRepository.saveAll(List.of(
                course("CS101", "Introduction to Programming", "Engineering", 4, 1L, 60, 0, Course.Status.OPEN, start, end),
                course("CS201", "Data Structures and Algorithms", "Engineering", 4, 1L, 50, 0, Course.Status.OPEN, start, end),
                course("CS305", "Database Systems", "Engineering", 3, 2L, 40, 0, Course.Status.OPEN, start, end),
                course("CS410", "Distributed Systems", "Engineering", 3, 2L, 30, 0, Course.Status.OPEN, start, end),
                course("DS220", "Machine Learning Fundamentals", "Science", 4, 3L, 45, 0, Course.Status.OPEN, start, end),
                course("MB101", "Principles of Management", "Business", 3, 4L, 80, 0, Course.Status.OPEN, start, end),
                // Full course, handy for testing the 409 "course is full" path.
                course("CS499", "Capstone Seminar", "Engineering", 2, 1L, 2, 2, Course.Status.OPEN, start, end),
                // Not open, handy for testing the "not open for enrollment" path.
                course("PH310", "Quantum Mechanics", "Science", 4, 5L, 35, 0, Course.Status.DRAFT, start, end),
                course("MB330", "Marketing Analytics", "Business", 3, 4L, 40, 0, Course.Status.CLOSED, start, end)
        ));
        log.info("Seeded {} sample courses", courseRepository.count());
    }

    private Course course(String code, String title, String dept, int credits, Long instructor,
                          int capacity, int enrolled, Course.Status status, LocalDate start, LocalDate end) {
        return Course.builder()
                .code(code).title(title).description(title + " (sample data)")
                .department(dept).credits(credits).semester("2026-FALL")
                .instructorId(instructor).capacity(capacity).enrolledCount(enrolled)
                .status(status).startDate(start).endDate(end)
                .build();
    }
}
