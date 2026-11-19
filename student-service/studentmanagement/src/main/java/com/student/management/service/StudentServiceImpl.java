package com.student.management.service;

import com.student.management.dto.StudentDTO;
import com.student.management.exception.DuplicateEmailException;
import com.student.management.exception.StudentNotFoundException;
import com.student.management.model.Student;
import com.student.management.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    @Override
    public StudentDTO.Response createStudent(StudentDTO.Request request) {
        log.info("Creating student with email: {}", request.getEmail());
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }
        Student student = toEntity(request);
        Student saved = studentRepository.save(student);
        log.info("Student created with id: {}", saved.getId());
        return StudentDTO.Response.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDTO.Response getStudentById(Long id) {
        return StudentDTO.Response.fromEntity(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDTO.Response getStudentByEmail(String email) {
        return studentRepository.findByEmail(email)
                .map(StudentDTO.Response::fromEntity)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentDTO.Response> getAllStudents(Pageable pageable) {
        return studentRepository.findAll(pageable).map(StudentDTO.Response::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentDTO.Response> searchStudents(String query, Pageable pageable) {
        return studentRepository.searchStudents(query, pageable).map(StudentDTO.Response::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentDTO.Response> filterStudents(String department, Integer year, Student.Status status, Pageable pageable) {
        return studentRepository.filterStudents(department, year, status, pageable).map(StudentDTO.Response::fromEntity);
    }

    @Override
    public StudentDTO.Response updateStudent(Long id, StudentDTO.Request request) {
        log.info("Updating student with id: {}", id);
        Student student = findById(id);

        if (!student.getEmail().equals(request.getEmail()) &&
            studentRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setCourse(request.getCourse());
        student.setDepartment(request.getDepartment());
        student.setYear(request.getYear());
        student.setGpa(request.getGpa());
        student.setAddress(request.getAddress());
        if (request.getStatus() != null) student.setStatus(request.getStatus());

        return StudentDTO.Response.fromEntity(studentRepository.save(student));
    }

    @Override
    public StudentDTO.Response updateStudentStatus(Long id, Student.Status status) {
        Student student = findById(id);
        student.setStatus(status);
        return StudentDTO.Response.fromEntity(studentRepository.save(student));
    }

    @Override
    public void deleteStudent(Long id) {
        log.info("Deleting student with id: {}", id);
        if (!studentRepository.existsById(id)) throw new StudentNotFoundException(id);
        studentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDTO.Summary getSummary() {
        Map<String, Long> byDept = new LinkedHashMap<>();
        studentRepository.countByDepartment()
                .forEach(row -> byDept.put((String) row[0], (Long) row[1]));

        Map<String, Long> byYear = new LinkedHashMap<>();
        studentRepository.countByYear()
                .forEach(row -> byYear.put("Year " + row[0], (Long) row[1]));

        return StudentDTO.Summary.builder()
                .totalStudents(studentRepository.count())
                .activeStudents(studentRepository.countByStatus(Student.Status.ACTIVE))
                .graduatedStudents(studentRepository.countByStatus(Student.Status.GRADUATED))
                .inactiveStudents(studentRepository.countByStatus(Student.Status.INACTIVE))
                .suspendedStudents(studentRepository.countByStatus(Student.Status.SUSPENDED))
                .averageGpa(studentRepository.findAverageGpa())
                .studentsByDepartment(byDept)
                .studentsByYear(byYear)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentDTO.Response> getTopStudents(double minGpa) {
        return studentRepository.findTopStudents(minGpa)
                .stream().map(StudentDTO.Response::fromEntity).collect(Collectors.toList());
    }

    private Student findById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
    }

    private Student toEntity(StudentDTO.Request r) {
        return Student.builder()
                .firstName(r.getFirstName())
                .lastName(r.getLastName())
                .email(r.getEmail())
                .phone(r.getPhone())
                .dateOfBirth(r.getDateOfBirth())
                .course(r.getCourse())
                .department(r.getDepartment())
                .year(r.getYear())
                .gpa(r.getGpa())
                .status(r.getStatus() != null ? r.getStatus() : Student.Status.ACTIVE)
                .address(r.getAddress())
                .build();
    }
}
