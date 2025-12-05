package com.restaurant.api.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity UserRole
 * -------------------------------------------------------------------
 * Đại diện cho bảng trung gian user_role trong DB, dùng để
 * map quan hệ N-N giữa:
 *
 *  - Bảng app_user  (entity: User, @Table(name = "app_user"))
 *  - Bảng role      (entity: Role,  @Table(name = "role"))
 *
 * Ý nghĩa:
 *  - Một User (app_user) có thể có nhiều Role
 *  - Một Role có thể được gán cho nhiều User
 *
 * Bảng user_role được tạo bởi Flyway trong Module 13.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_role") // 🟢 Bảng trung gian user_role trong DB
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    /**
     * User được gán role.
     * -----------------------------------------------------------
     * Lưu ý:
     *  - Entity User hiện tại đang map với bảng app_user
     *    (@Table(name = "app_user")).
     *  - Cột khóa ngoại trong bảng user_role là user_id,
     *    trỏ tới app_user.id.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // FK -> app_user.id
    private User user;

    /**
     * Role được gán cho User.
     * -----------------------------------------------------------
     *  - Cột role_id trong user_role trỏ tới role.id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false) // FK -> role.id
    private Role role;
}
