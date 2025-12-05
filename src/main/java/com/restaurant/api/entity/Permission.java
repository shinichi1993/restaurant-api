package com.restaurant.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity Permission
 * -------------------------------------------------------------------
 * Đại diện cho bảng permission trong DB.
 * Dùng để quản lý CÁC QUYỀN CHI TIẾT (ORDER_VIEW, USER_MANAGE...).
 *
 * Mỗi permission có:
 *  - code: mã duy nhất để check trong code
 *  - name: tên hiển thị
 *  - description: mô tả thêm
 *
 * Bảng permission được tạo bởi Flyway (Module 13).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(nullable = false, unique = true, length = 100)
    private String code; // Mã quyền (ORDER_VIEW, ORDER_CREATE, REPORT_VIEW...)

    @Column(nullable = false, length = 150)
    private String name; // Tên hiển thị của quyền

    @Column(length = 255)
    private String description; // Mô tả ngắn gọn

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // Thời điểm tạo

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // Thời điểm cập nhật

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
