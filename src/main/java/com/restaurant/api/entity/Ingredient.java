package com.restaurant.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity Ingredient – quản lý nguyên liệu trong hệ thống.
 * Khớp 100% với cấu trúc bảng ingredient trong Flyway V4.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ingredient")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(nullable = false)
    private String name; // Tên nguyên liệu

    @Column(nullable = false)
    private String unit; // Đơn vị tính (gram, ml, cái...)

    @Column(name = "stock_quantity", nullable = false)
    private BigDecimal stockQuantity; // Tồn kho hiện tại, mapping từ NUMERIC

    @Column(nullable = false)
    private Boolean active; // Trạng thái nguyên liệu (true = hoạt động)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // Thời gian tạo

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // Thời gian cập nhật

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
