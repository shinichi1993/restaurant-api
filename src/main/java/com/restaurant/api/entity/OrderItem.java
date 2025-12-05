package com.restaurant.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity OrderItem – Lưu chi tiết từng món trong order
 * ---------------------------------------------------------------
 * - Tương ứng bảng "order_item" trong Flyway V9
 * - 1 Order có nhiều OrderItem
 * - Mỗi OrderItem liên kết tới 1 món (Dish)
 * ---------------------------------------------------------------
 * quantity: số lượng món gọi
 */
@Entity
@Table(name = "order_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    // FK tới order
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    // FK tới dish (Module 06)
    @Column(name = "dish_id", nullable = false)
    private Long dishId;

    @Column(nullable = false)
    private Integer quantity; // Số phần khách gọi

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // Thời gian tạo dòng này

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // Thời gian cập nhật

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
