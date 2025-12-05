package com.restaurant.api.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * IngredientUsageReportItem
 * ------------------------------------------------------------
 * DTO dùng trong báo cáo "Nguyên liệu tiêu hao".
 *
 * Trường dữ liệu:
 *  - ingredientId   : ID nguyên liệu
 *  - ingredientName : Tên nguyên liệu
 *  - unit           : Đơn vị tính (gram, ml, cái...)
 *  - totalUsed      : Tổng số lượng đã tiêu hao trong khoảng thời gian được chọn
 *
 * Ghi chú:
 *  - Dữ liệu thường lấy từ bảng StockEntry (quantity âm).
 *  - totalUsed luôn là số dương (đã được quy đổi khi query).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientUsageReportItem {

    private Long ingredientId;      // ID nguyên liệu
    private String ingredientName;  // Tên nguyên liệu
    private String unit;            // Đơn vị tính
    private BigDecimal totalUsed;   // Tổng số lượng tiêu hao
}
