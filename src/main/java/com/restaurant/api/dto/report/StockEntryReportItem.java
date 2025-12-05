package com.restaurant.api.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * StockEntryReportItem
 * ------------------------------------------------------------
 * DTO dùng trong báo cáo "Nhập kho nguyên liệu".
 *
 * Trường dữ liệu:
 *  - ingredientId        : ID nguyên liệu
 *  - ingredientName      : Tên nguyên liệu
 *  - unit                : Đơn vị tính
 *  - totalImportedAmount : Tổng số lượng đã nhập (chỉ tính quantity dương)
 *
 * Lưu ý:
 *  - Dữ liệu lấy từ bảng StockEntry (Module 05).
 *  - Nếu sau này cần chi tiết từng phiếu nhập, ta có thể tạo thêm DTO khác,
 *    còn DTO này ưu tiên dạng tổng hợp để vẽ báo cáo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockEntryReportItem {

    private Long ingredientId;            // ID nguyên liệu
    private String ingredientName;        // Tên nguyên liệu
    private String unit;                  // Đơn vị tính
    private BigDecimal totalImportedAmount; // Tổng số lượng đã nhập
}
