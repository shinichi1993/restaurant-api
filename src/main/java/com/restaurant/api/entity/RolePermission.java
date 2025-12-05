package com.restaurant.api.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity RolePermission
 * -------------------------------------------------------------------
 * Đại diện cho bảng role_permission (quan hệ N-N giữa role và permission).
 *
 * Ý nghĩa:
 *  - Một role có thể có nhiều permission
 *  - Một permission có thể thuộc nhiều role
 *
 * Ở đây ta model bảng trung gian thành Entity riêng để sau này
 * có thể mở rộng (thêm cột metadata nếu cần).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "role_permission")
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    /**
     * Role được gán quyền.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /**
     * Permission được gán cho role.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;
}
