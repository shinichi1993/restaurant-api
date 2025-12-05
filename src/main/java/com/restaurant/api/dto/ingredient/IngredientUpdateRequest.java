package com.restaurant.api.dto.ingredient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO cập nhật nguyên liệu.
 * FE gửi vào khi sửa nguyên liệu.
 */
@Data
public class IngredientUpdateRequest {

    @NotBlank(message = "Tên nguyên liệu không được để trống")
    private String name;

    @NotBlank(message = "Đơn vị tính không được để trống")
    private String unit;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    private BigDecimal stockQuantity;

    @NotNull(message = "Trạng thái không được để trống")
    private Boolean active; // true / false
}
