package com.restaurant.api.spec;

import com.restaurant.api.entity.AuditLog;
import com.restaurant.api.enums.AuditAction;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class AuditLogSpecification {

    public static Specification<AuditLog> hasEntity(String entity) {
        return (root, query, cb) -> {
            if (entity == null || entity.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("entity")), "%" + entity.toLowerCase() + "%");
        };
    }

    public static Specification<AuditLog> hasUserId(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) return cb.conjunction();
            return cb.equal(root.get("user").get("id"), userId);
        };
    }

    public static Specification<AuditLog> hasAction(AuditAction action) {
        return (root, query, cb) -> {
            if (action == null) return cb.conjunction();
            return cb.equal(root.get("action"), action);
        };
    }

    public static Specification<AuditLog> hasActions(List<AuditAction> actions) {
        return (root, query, cb) -> {
            if (actions == null || actions.isEmpty()) return cb.conjunction();
            return root.get("action").in(actions);
        };
    }
}
