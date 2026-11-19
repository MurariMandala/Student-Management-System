package com.student.management.service;

import com.student.management.dto.DepartmentDTO;
import com.student.management.exception.DuplicateDepartmentException;
import com.student.management.model.Department;
import com.student.management.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService{

    private final DepartmentRepository departmentRepository;

    @Override
    public DepartmentDTO.Response saveDepartment(DepartmentDTO.Request request) {
         log.info("Creating department {} ",request.getDepartmentName());
        Department department = toEntity(request);
        if(departmentRepository.existsByDepartmentName(request.getDepartmentName())){
            log.error("Error while creating department {}",request.getDepartmentName());
            throw new DuplicateDepartmentException(department.getDepartmentName());
        }
        Department saved = departmentRepository.save(department);
        return DepartmentDTO.Response.fromEntity(saved);
    }

    private Department toEntity(DepartmentDTO.Request request){
        return Department.builder()
                .departmentName(request.getDepartmentName())
                .description(request.getDescription())
                .build();
    }
}
