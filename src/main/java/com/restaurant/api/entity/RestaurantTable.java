package com.restaurant.api.entity;

import com.restaurant.api.enums.TableStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity RestaurantTable – Quản lý thông tin bàn trong nhà hàng.
 * Khớp với bảng restaurant_table (MODULE 16).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "restaurant_table")
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(nullable = false, unique = true, length = 50)
    private String name; // Tên bàn (VD: B1, B2, T1-B3)

    @Column(nullable = false)
    private Integer capacity; // Số lượng khách tối đa

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableStatus status; // Trạng thái hiện tại của bàn

    @Column(name = "merged_root_id")
    private Long mergedRootId; // Nếu bàn này đã gộp vào bàn khác → lưu id bàn gốc

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // Thời gian tạo bản ghi

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // Thời gian cập nhật gần nhất

    /**
     * Hàm tiện ích kiểm tra bàn có đang ở trạng thái trống hay không.
     */
    public boolean isAvailable() {
        return TableStatus.AVAILABLE.equals(this.status);
    }

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
