package com.restaurant.api.dto.audit;

import com.restaurant.api.enums.AuditAction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AuditLogResponse – DTO trả ra FE
 * --------------------------------------------------------------
 * Chỉ trả dữ liệu cần thiết để hiển thị:
 *  - id            : Id log
 *  - userId        : User thực hiện
 *  - username      : Username người thực hiện
 *  - action        : Hành động (CREATE, UPDATE...)
 *  - entity        : Tên bảng/entity
 *  - entityId      : Id của entity bị tác động
 *  - description   : Mô tả ngắn (sau này nâng cấp)
 *  - createdAt     : thời gian ghi log
 *
 * Không trả beforeData / afterData để tránh nặng FE.
 */
@Data
@Builder
public class AuditLogResponse {

    private Long id;

    private Long userId;
    private String username;

    private AuditAction action;   // ENUM
    private String entity;        // VD: "order", "user", "ingredient"
    private Long entityId;

    private String beforeData;
    private String afterData;

    private LocalDateTime createdAt;
}
