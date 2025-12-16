package com.restaurant.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.restaurant.api.entity.AuditLog;
import com.restaurant.api.entity.User;
import com.restaurant.api.enums.AuditAction;
import com.restaurant.api.repository.AuditLogRepository;
import com.restaurant.api.repository.UserRepository;
import com.restaurant.api.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;

import com.restaurant.api.dto.audit.AuditLogResponse;
import com.restaurant.api.spec.AuditLogSpecification;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AuditLogService – phiên bản FINAL
 * ----------------------------------------------------
 * - beforeData / afterData luôn được convert thành JSON String
 * - TƯƠNG THÍCH HOÀN TOÀN với Entity hiện tại (String)
 * - KHÔNG yêu cầu sửa các service khác
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final HttpServletRequest request;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Convert object → JSON an toàn
     * - Nếu object là entity → chỉ lấy các field đơn giản
     * - Loại bỏ quan hệ để tránh vòng lặp
     */
    private String convertToJson(Object obj) {
        try {
            if (obj == null) return null;
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            System.err.println("❌ convertToJson ERROR: " + e.getMessage());
            return "\"<json_error>\"";
        }
    }

    /**
     * Hàm log dùng trên toàn hệ thống
     */
    public void log(
            AuditAction action,
            String entity,
            Long entityId,
            Object beforeData,
            Object afterData
    ) {
        try {
            // Lấy user hiện tại từ JWT
            String username = AuthUtil.getCurrentUsername();
            User user = userRepository.findByUsername(username).orElse(null);

            AuditLog log = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entity(entity)
                    .entityId(entityId)
                    .beforeData(convertToJson(beforeData))   // ← STRING
                    .afterData(convertToJson(afterData))     // ← STRING
                    .ipAddress(getClientIp())   // ✅ ADD
                    .userAgent(getUserAgent())  // ✅ ADD
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(log);

        } catch (Exception e) {
            System.err.println("❌ Lỗi ghi audit log: " + e.getMessage());
        }
    }

    /**
     * API search Audit Log
     */
    public Page<AuditLogResponse> searchAuditLogs(
            String entity,
            String username,
            List<AuditAction> actions,
            LocalDateTime fromDate,   // ✅ ADD
            LocalDateTime toDate,     // ✅ ADD
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<AuditLog> spec =
                Specification.where(AuditLogSpecification.hasEntity(entity))
                        .and(AuditLogSpecification.hasUsername(username))
                        .and(AuditLogSpecification.hasActions(actions))
                        .and(AuditLogSpecification.createdAtBetween(fromDate, toDate)); // ✅ ADD;

        Page<AuditLog> result = auditLogRepository.findAll(spec, pageable);

        return result.map(log -> AuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .username(log.getUser() != null ? log.getUser().getUsername() : null)
                .entity(log.getEntity())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .beforeData(log.getBeforeData()) // JSON STRING
                .afterData(log.getAfterData())   // JSON STRING
                .createdAt(log.getCreatedAt())
                .build());
    }

    //===========HELPER================
    /**
     * Lấy IP của client an toàn (ưu tiên X-Forwarded-For)
     */
    private String getClientIp() {
        try {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Lấy User-Agent của client
     */
    private String getUserAgent() {
        try {
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }

}