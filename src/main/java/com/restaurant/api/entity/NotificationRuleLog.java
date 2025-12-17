package com.restaurant.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity NotificationRuleLog
 * ----------------------------------------------------
 * Lưu lịch sử trigger của các Notification Rule
 * để tránh spam notification.
 */
@Entity
@Table(name = "notification_rule_log")
@Getter
@Setter
public class NotificationRuleLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Key định danh rule (LOW_STOCK, REVENUE_ZERO...)
     */
    @Column(name = "rule_key", nullable = false, unique = true, length = 100)
    private String ruleKey;

    /**
     * Thời điểm gần nhất rule đã trigger notification
     */
    @Column(name = "last_triggered_at", nullable = false)
    private LocalDateTime lastTriggeredAt;

    /**
     * Hash của payload (threshold, days...)
     * Dùng để phát hiện điều kiện thay đổi.
     */
    @Column(name = "last_payload_hash")
    private String lastPayloadHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
