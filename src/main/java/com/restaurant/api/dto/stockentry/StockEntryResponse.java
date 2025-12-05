package com.restaurant.api.dto.stockentry;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO trả dữ liệu lịch sử nhập kho / điều chỉnh kho cho FE.
 */
@Data
@Builder
public class StockEntryResponse {

    private Long id;                // ID phiếu nhập / điều chỉnh
    private Long ingredientId;      // ID nguyên liệu
    private String ingredientName;  // Tên nguyên liệu
    private BigDecimal quantity;        // Số lượng (dương/âm)
    private String note;            // Ghi chú
    private LocalDateTime createdAt; // Thời gian tạo phiếu
}
