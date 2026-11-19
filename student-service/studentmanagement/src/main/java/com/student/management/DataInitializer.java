package com.student.management;

import com.student.management.model.Student;
import com.student.management.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final StudentRepository studentRepository;

    @Override
    public void run(String... args) {
        if (studentRepository.count() == 0) {
            log.info("Seeding sample student data...");
            studentRepository.saveAll(List.of(
                Student.builder().firstName("Aarav").lastName("Sharma").email("aarav.sharma@uni.edu")
                    .phone("9876543210").dateOfBirth(LocalDate.of(2002, 3, 15))
                    .course("B.Tech Computer Science").department("Engineering").year(3)
                    .gpa(9.2).status(Student.Status.ACTIVE).address("Hyderabad, Telangana").build(),

                Student.builder().firstName("Priya").lastName("Reddy").email("priya.reddy@uni.edu")
                    .phone("9876543211").dateOfBirth(LocalDate.of(2001, 7, 22))
                    .course("B.Sc Data Science").department("Science").year(4)
                    .gpa(8.7).status(Student.Status.ACTIVE).address("Bangalore, Karnataka").build(),

                Student.builder().firstName("Rahul").lastName("Mehta").email("rahul.mehta@uni.edu")
                    .phone("9876543212").dateOfBirth(LocalDate.of(2003, 1, 10))
                    .course("BBA").department("Business").year(2)
                    .gpa(7.5).status(Student.Status.ACTIVE).address("Mumbai, Maharashtra").build(),

                Student.builder().firstName("Sneha").lastName("Iyer").email("sneha.iyer@uni.edu")
                    .phone("9876543213").dateOfBirth(LocalDate.of(2000, 11, 5))
                    .course("M.Tech AI").department("Engineering").year(1)
                    .gpa(9.5).status(Student.Status.ACTIVE).address("Chennai, Tamil Nadu").build(),

                Student.builder().firstName("Karan").lastName("Singh").email("karan.singh@uni.edu")
                    .phone("9876543214").dateOfBirth(LocalDate.of(2002, 6, 30))
                    .course("B.Tech Electronics").department("Engineering").year(3)
                    .gpa(6.8).status(Student.Status.INACTIVE).address("Delhi, NCR").build(),

                Student.builder().firstName("Ananya").lastName("Patel").email("ananya.patel@uni.edu")
                    .phone("9876543215").dateOfBirth(LocalDate.of(1999, 4, 18))
                    .course("MBA").department("Business").year(2)
                    .gpa(8.1).status(Student.Status.GRADUATED).address("Ahmedabad, Gujarat").build(),

                Student.builder().firstName("Vikram").lastName("Nair").email("vikram.nair@uni.edu")
                    .phone("9876543216").dateOfBirth(LocalDate.of(2003, 9, 25))
                    .course("B.Sc Physics").department("Science").year(1)
                    .gpa(7.9).status(Student.Status.ACTIVE).address("Kochi, Kerala").build(),

                Student.builder().firstName("Meera").lastName("Gupta").email("meera.gupta@uni.edu")
                    .phone("9876543217").dateOfBirth(LocalDate.of(2001, 2, 14))
                    .course("B.Tech IT").department("Engineering").year(4)
                    .gpa(8.4).status(Student.Status.ACTIVE).address("Pune, Maharashtra").build(),

                Student.builder().firstName("Arjun").lastName("Verma").email("arjun.verma@uni.edu")
                    .phone("9876543218").dateOfBirth(LocalDate.of(2002, 12, 3))
                    .course("B.Com").department("Commerce").year(2)
                    .gpa(5.9).status(Student.Status.SUSPENDED).address("Jaipur, Rajasthan").build(),

                Student.builder().firstName("Deepa").lastName("Kumar").email("deepa.kumar@uni.edu")
                    .phone("9876543219").dateOfBirth(LocalDate.of(2000, 8, 7))
                    .course("M.Sc Chemistry").department("Science").year(2)
                    .gpa(9.0).status(Student.Status.ACTIVE).address("Hyderabad, Telangana").build()
            ));
            log.info("Sample data seeded: {} students", studentRepository.count());
        }
    }
}
