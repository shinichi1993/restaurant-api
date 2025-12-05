package com.restaurant.api.dto.stockentry;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO request tạo phiếu nhập kho / điều chỉnh kho.
 * -------------------------------------------------
 * - Với nhập kho bình thường: quantity phải > 0 (validate ở Service).
 * - Với điều chỉnh kho: quantity có thể âm hoặc dương, != 0.
 */
@Data
public class StockEntryCreateRequest {

    @NotNull(message = "Nguyên liệu không được để trống")
    private Long ingredientId; // ID nguyên liệu

    @NotNull(message = "Số lượng không được để trống")
    private BigDecimal quantity;   // Số lượng nhập / điều chỉnh

    private String note;       // Ghi chú (optional)
}
