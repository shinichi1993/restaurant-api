package com.restaurant.api.dto.recipe;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RecipeItemResponse – DTO trả định lượng món về cho FE
 * ------------------------------------------------------------------
 * Dùng cho:
 *  - GET /api/recipes/dish/{dishId}
 *
 * Trả về thông tin:
 *  - id              : ID bản ghi định lượng
 *  - ingredientId    : ID nguyên liệu
 *  - ingredientName  : Tên nguyên liệu
 *  - unit            : Đơn vị tính (gram / ml / cái...)
 *  - quantity        : Số lượng dùng cho 1 phần món
 *  - createdAt       : Ngày tạo
 *  - updatedAt       : Ngày cập nhật
 *
 * FE sẽ dùng:
 *  - Hiển thị bảng định lượng
 *  - Hiển thị đơn vị + số lượng rõ ràng
 */
@Data
@Builder
public class RecipeItemResponse {

    private Long id; // ID dòng định lượng

    private Long ingredientId;   // ID nguyên liệu
    private String ingredientName; // Tên nguyên liệu
    private String unit;           // Đơn vị tính (từ Ingredient)

    private BigDecimal quantity;   // Số lượng cho 1 phần món

    private LocalDateTime createdAt; // Ngày tạo
    private LocalDateTime updatedAt; // Ngày cập nhật
}
