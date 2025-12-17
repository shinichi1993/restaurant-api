package com.restaurant.api.dto.backup;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DishDto – DTO backup / restore cho bảng dish
 * ---------------------------------------------------
 * Dùng trong Phase 4.4 – Backup / Restore Database
 *
 * Lưu snapshot dữ liệu món ăn tại thời điểm backup
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishDto {

    private Long id;
    private String name;
    private BigDecimal price;
    private String status;     // ACTIVE / INACTIVE
    private String imageUrl;
    private Long categoryId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // getter / setter
}
