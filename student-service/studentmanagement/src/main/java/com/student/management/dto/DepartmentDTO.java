package com.student.management.dto;

import com.student.management.model.Department;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


public class DepartmentDTO {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Request{
        @NotBlank(message = "DepartmentName is required")
        private String departmentName;
        @NotBlank(message = "Description is required")
        private String description;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Response{
        private Long id;
        private String departmentName;
        private String description;

        public static Response fromEntity(Department department){
            return Response.builder()
                    .id(department.getId())
                    .departmentName(department.getDepartmentName())
                    .description(department.getDescription())
                    .build();
        }

    }
}
