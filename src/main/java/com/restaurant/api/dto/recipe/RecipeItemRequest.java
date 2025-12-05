package com.restaurant.api.dto.recipe;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * RecipeItemRequest – DTO dùng cho tạo / cập nhật định lượng món
 * ------------------------------------------------------------------
 * Dùng cho các API:
 *  - POST /api/recipes/add          → thêm nguyên liệu vào món
 *  - PUT  /api/recipes/update/{id}  → cập nhật số lượng
 *
 * Trường dữ liệu:
 *  - dishId        : ID món ăn (bắt buộc)
 *  - ingredientId  : ID nguyên liệu (bắt buộc)
 *  - quantity      : số lượng nguyên liệu dùng cho 1 phần món
 *
 * Lưu ý:
 *  - Dùng BigDecimal theo Rule 26 để khớp NUMERIC trong DB
 *  - Validate số lượng > 0
 */
@Data
public class RecipeItemRequest {

    @NotNull(message = "Món ăn không được để trống")
    private Long dishId; // ID món ăn

    @NotNull(message = "Nguyên liệu không được để trống")
    private Long ingredientId; // ID nguyên liệu

    @NotNull(message = "Số lượng không được để trống")
    @DecimalMin(value = "0.0001", message = "Số lượng phải lớn hơn 0")
    private BigDecimal quantity; // Số lượng nguyên liệu cho 1 phần món
}
