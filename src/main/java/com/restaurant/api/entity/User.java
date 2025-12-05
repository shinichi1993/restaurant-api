package com.restaurant.api.entity;

import com.restaurant.api.enums.UserRole;
import com.restaurant.api.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity User – Quản lý tài khoản người dùng
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "app_user") // hoặc "user" giống Flyway của bạn
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(nullable = false, unique = true, length = 100)
    private String username; // Tên đăng nhập (unique)

    @Column(nullable = false)
    private String password; // Mật khẩu đã mã hóa

    @Column(nullable = false, length = 150)
    private String fullName; // Họ tên hiển thị

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role; // Vai trò (ADMIN / STAFF)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status; // Trạng thái (ACTIVE / INACTIVE)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // Thời điểm tạo

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // Thời điểm cập nhật gần nhất

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
