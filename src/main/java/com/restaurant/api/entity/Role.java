package com.restaurant.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity Role
 * -------------------------------------------------------------------
 * Đại diện cho bảng role trong DB.
 * Dùng để quản lý CÁC VAI TRÒ (ADMIN, STAFF, CASHIER...) trong hệ thống.
 *
 * Mỗi role có:
 *  - code: mã duy nhất, dùng trong code / phân quyền
 *  - name: tên hiển thị cho người dùng
 *  - description: mô tả thêm (optional)
 *
 * Bảng role được tạo bởi Flyway (Module 13 – V12__create_role_permission_tables.sql)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(nullable = false, unique = true, length = 50)
    private String code; // Mã role (ADMIN, STAFF, CASHIER...)

    @Column(nullable = false, length = 100)
    private String name; // Tên hiển thị của role

    @Column(length = 255)
    private String description; // Mô tả ngắn gọn về role

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // Thời điểm tạo

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // Thời điểm cập nhật gần nhất

    // ----------------------------------------------------------------
    // Hàm lifecycle để auto set thời gian tạo / cập nhật
    // ----------------------------------------------------------------
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
