package com.student.management.controller;

import com.student.management.dto.ApiResponse;
import com.student.management.dto.DepartmentDTO;
import com.student.management.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/department")
@RequiredArgsConstructor
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentDTO.Response>> saveDepartment(@RequestBody @Valid DepartmentDTO.Request request){
        DepartmentDTO.Response response = departmentService.saveDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Department Created Successfully",response));
    }



}
