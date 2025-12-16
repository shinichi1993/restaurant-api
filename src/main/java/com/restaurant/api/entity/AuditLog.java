package com.restaurant.api.entity;

import com.restaurant.api.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AuditLog – Entity dùng để lưu lịch sử thao tác toàn hệ thống.
 * ----------------------------------------------------------------
 * Mỗi bản ghi gồm:
 *   - user (ai thực hiện)
 *   - action (hành động: USER_UPDATE, ORDER_CREATE…)
 *   - entity (tên bảng/đối tượng tác động)
 *   - entityId (ID đối tượng)
 *   - beforeData (JSON – dữ liệu trước)
 *   - afterData  (JSON – dữ liệu sau)
 *   - createdAt (thời gian)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User thực hiện hành động (FK đến app_user)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Mã hành động chuẩn enum (USER_UPDATE, ORDER_CREATE…)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    /**
     * Tên entity bị tác động: user, ingredient, order…
     */
    @Column(nullable = false)
    private String entity;

    /**
     * ID của entity bị tác động (có thể null cho 1 số action như login)
     */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * IP client (lấy từ request)
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * User-Agent (lấy từ request)
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Dữ liệu trước khi thay đổi (JSON)
     */
    private String beforeData;

    /**
     * Dữ liệu sau khi thay đổi (JSON)
     */
    private String afterData;

    /**
     * Thời gian ghi log
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
