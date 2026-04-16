package com.realestate.emi.specification;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DateRangeSpec {

    public static <T> Specification<T> dateRange(String fieldName, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return cb.conjunction();

            Path<?> path = root.get(fieldName);

            // Handle both LocalDate and LocalDateTime fields
            if (from != null && to != null) {
                if (path.getJavaType().equals(LocalDateTime.class)) {
                    return cb.between(root.get(fieldName), from.atStartOfDay(), to.plusDays(1).atStartOfDay());
                }
                return cb.between(root.get(fieldName), from, to);
            } else if (from != null) {
                if (path.getJavaType().equals(LocalDateTime.class)) {
                    return cb.greaterThanOrEqualTo(root.get(fieldName), from.atStartOfDay());
                }
                return cb.greaterThanOrEqualTo(root.get(fieldName), from);
            } else {
                if (path.getJavaType().equals(LocalDateTime.class)) {
                    return cb.lessThanOrEqualTo(root.get(fieldName), to.plusDays(1).atStartOfDay());
                }
                return cb.lessThanOrEqualTo(root.get(fieldName), to);
            }
        };
    }

    public static <T> Specification<T> orgEquals(String fieldName, Long orgId) {
        return (root, query, cb) -> {
            if (orgId == null) return cb.conjunction();
            return cb.equal(root.get(fieldName).get("id"), orgId);
        };
    }
}
