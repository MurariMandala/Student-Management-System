package com.studentmgmt.enrollment.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CourseClientFallbackFactory implements FallbackFactory<CourseClient> {

    private static final String SERVICE = "Course service";

    @Override
    public CourseClient create(Throwable cause) {
        return new CourseClient() {
            @Override
            public RemoteResponse<CourseDto> getCourse(Long id) {
                throw FallbackSupport.translate(SERVICE, cause);
            }

            @Override
            public RemoteResponse<CourseDto> reserveSeat(Long id) {
                throw FallbackSupport.translate(SERVICE, cause);
            }

            @Override
            public RemoteResponse<CourseDto> releaseSeat(Long id) {
                throw FallbackSupport.translate(SERVICE, cause);
            }

            @Override
            public RemoteResponse<List<CourseDto>> getBatch(List<Long> ids) {
                throw FallbackSupport.translate(SERVICE, cause);
            }
        };
    }
}
