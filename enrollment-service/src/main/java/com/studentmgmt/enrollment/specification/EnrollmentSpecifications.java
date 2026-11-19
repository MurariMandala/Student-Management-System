package com.studentmgmt.enrollment.specification;

import com.studentmgmt.enrollment.model.Enrollment;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class EnrollmentSpecifications {

    private EnrollmentSpecifications() {
    }

    /** Every argument is optional. Only the ones provided are added to the query. */
    public static Specification<Enrollment> withFilters(Long studentId, Long courseId, Enrollment.Status status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (studentId != null) {
                predicates.add(cb.equal(root.get("studentId"), studentId));
            }
            if (courseId != null) {
                predicates.add(cb.equal(root.get("courseId"), courseId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
