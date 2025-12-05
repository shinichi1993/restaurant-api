package com.restaurant.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity StockEntry – Lưu lịch sử nhập kho / điều chỉnh kho nguyên liệu.
 * ------------------------------------------------------------
 * - Mỗi lần nhập/điều chỉnh sẽ tạo 1 bản ghi mới.
 * - quantity có thể dương (nhập) hoặc âm (điều chỉnh giảm tồn).
 * - Không cho sửa/xóa để giữ lịch sử chuẩn Audit (xử lý ở Service).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stock_entry")
public class StockEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient; // Nguyên liệu được nhập kho / điều chỉnh

    @Column(nullable = false)
    private BigDecimal quantity; // Số lượng nhập (dương: nhập, âm: điều chỉnh giảm)

    @Column(columnDefinition = "TEXT")
    private String note; // Ghi chú

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // Thời gian tạo phiếu

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
