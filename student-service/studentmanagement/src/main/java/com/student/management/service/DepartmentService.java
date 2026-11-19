package com.student.management.service;

import com.student.management.dto.DepartmentDTO;

public interface DepartmentService {
    DepartmentDTO.Response saveDepartment(DepartmentDTO.Request request);
}
