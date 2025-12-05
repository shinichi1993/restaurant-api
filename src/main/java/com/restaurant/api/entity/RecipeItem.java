package com.restaurant.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity RecipeItem – Định lượng nguyên liệu cho từng món ăn
 * ---------------------------------------------------------------
 * Mỗi bản ghi tương ứng:
 *  - 1 món ăn (dish)
 *  - 1 nguyên liệu (ingredient)
 *  - 1 số lượng nguyên liệu dùng cho 1 phần món (quantity)
 *
 * Ví dụ:
 *  - Món "Bánh đa cua"
 *      + Nguyên liệu "Bánh đa": 120.000 (gram)
 *      + Nguyên liệu "Cua":     80.000 (gram)
 *      + Nguyên liệu "Hành lá": 5.000 (gram)
 *
 * Bảng tương ứng trong DB: recipe_item (Flyway V8)
 * ---------------------------------------------------------------
 * Áp dụng:
 *  - Rule 13: Comment tiếng Việt đầy đủ
 *  - Rule 26: Dữ liệu số (quantity) dùng BigDecimal cho chính xác
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "recipe_item")
public class RecipeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    /**
     * Món ăn sử dụng định lượng này.
     * -----------------------------------------------------------
     * Quan hệ:
     *  - 1 Dish có thể có nhiều RecipeItem (N-1)
     *  - FetchType.LAZY để tránh load toàn bộ dữ liệu không cần thiết.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish; // Món ăn (FK tới bảng dish)

    /**
     * Nguyên liệu được dùng trong món.
     * -----------------------------------------------------------
     * Quan hệ:
     *  - 1 Ingredient có thể xuất hiện trong nhiều RecipeItem khác nhau.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient; // Nguyên liệu (FK tới bảng ingredient)

    /**
     * Số lượng nguyên liệu dùng cho 1 phần món.
     * -----------------------------------------------------------
     * Ví dụ:
     *  - 120.000 (gram)
     *  - 0.500 (lít)
     *  - 1.000 (cái)
     *
     * Dùng BigDecimal để:
     *  - Tránh sai số do số thực
     *  - Khớp với kiểu NUMERIC(12,3) trong DB.
     */
    @Column(nullable = false)
    private BigDecimal quantity; // Số lượng nguyên liệu cho 1 phần món

    /**
     * Thời gian tạo bản ghi.
     * Rule chuẩn: dùng LocalDateTime (Rule 26).
     */
    private LocalDateTime createdAt;

    /**
     * Thời gian cập nhật gần nhất.
     */
    private LocalDateTime updatedAt;

    /**
     * Hàm lifecycle tự động set ngày tạo & cập nhật
     * khi INSERT bản ghi lần đầu.
     */
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Hàm lifecycle tự động set ngày cập nhật
     * khi UPDATE bản ghi.
     */
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
