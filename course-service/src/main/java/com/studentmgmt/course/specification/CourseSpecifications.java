package com.studentmgmt.course.specification;

import com.studentmgmt.course.model.Course;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class CourseSpecifications {

    private CourseSpecifications() {
    }

    /** Every argument is optional. Only the ones provided are added to the query. */
    public static Specification<Course> withFilters(String keyword, String department, String semester,
                                                    Course.Status status, boolean availableOnly) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("department")), like)));
            }
            if (StringUtils.hasText(department)) {
                predicates.add(cb.equal(cb.lower(root.get("department")), department.trim().toLowerCase()));
            }
            if (StringUtils.hasText(semester)) {
                predicates.add(cb.equal(cb.lower(root.get("semester")), semester.trim().toLowerCase()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (availableOnly) {
                predicates.add(cb.equal(root.get("status"), Course.Status.OPEN));
                predicates.add(cb.lessThan(root.<Integer>get("enrolledCount"), root.<Integer>get("capacity")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
