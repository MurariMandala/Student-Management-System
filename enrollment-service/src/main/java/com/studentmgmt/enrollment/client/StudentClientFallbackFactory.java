package com.studentmgmt.enrollment.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class StudentClientFallbackFactory implements FallbackFactory<StudentClient> {

    @Override
    public StudentClient create(Throwable cause) {
        return new StudentClient() {
            @Override
            public RemoteResponse<StudentDto> getStudent(Long id) {
                throw FallbackSupport.translate("Student service", cause);
            }
        };
    }
}
