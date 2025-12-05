package com.restaurant.api.dto.ingredient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO tạo mới nguyên liệu.
 * FE gửi vào khi thêm nguyên liệu.
 */
@Data
public class IngredientCreateRequest {

    @NotBlank(message = "Tên nguyên liệu không được để trống")
    private String name; // Tên nguyên liệu

    @NotBlank(message = "Đơn vị tính không được để trống")
    private String unit; // gram, ml, cái...

    @NotNull(message = "Số lượng tồn ban đầu không được để trống")
    private BigDecimal stockQuantity; // tồn kho ban đầu
}
