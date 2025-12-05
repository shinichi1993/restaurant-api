package com.restaurant.api.dto.ingredient;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO trả dữ liệu nguyên liệu ra FE.
 * Dùng cho: danh sách, chi tiết, modal xem nhanh.
 */
@Data
@Builder
public class IngredientResponse {
    private Long id;             // ID nguyên liệu
    private String name;         // Tên nguyên liệu
    private String unit;         // Đơn vị tính
    private BigDecimal stockQuantity; // Số lượng tồn kho
    private Boolean active;      // Trạng thái (true = hoạt động)
}
