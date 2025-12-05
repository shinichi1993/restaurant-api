package com.restaurant.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity Dish – Món ăn trong hệ thống
 * -----------------------------------------------------
 * - Thuộc về Category (N-1)
 * - Giá bán dùng BigDecimal theo Rule 26
 * - Trạng thái ACTIVE / INACTIVE
 * - Có ảnh (imageUrl) dạng URL
 * - Tự động set createdAt / updatedAt
 * -----------------------------------------------------
 * Áp dụng Rule 13: toàn bộ comment tiếng Việt.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "dish")
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(nullable = false)
    private String name; // Tên món ăn

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category; // Danh mục món ăn

    @Column(nullable = false)
    private BigDecimal price; // Giá bán (BigDecimal theo Rule 26)

    @Column(name = "image_url")
    private String imageUrl; // URL ảnh món ăn

    @Column(nullable = false)
    private String status; // ACTIVE / INACTIVE

    private LocalDateTime createdAt; // Ngày tạo
    private LocalDateTime updatedAt; // Ngày chỉnh sửa

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
