package com.restaurant.api.dto.backup;

import com.restaurant.api.enums.AuditAction;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLogDto {
    private Long id;
    private Long userId; // AuditLog.user.id
    private AuditAction action;
    private String entity;
    private Long entityId;
    private String ipAddress;
    private String userAgent;
    private String beforeData;
    private String afterData;
    private LocalDateTime createdAt;
}
