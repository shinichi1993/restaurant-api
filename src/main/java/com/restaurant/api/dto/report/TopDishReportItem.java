package com.restaurant.api.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * TopDishReportItem
 * ------------------------------------------------------------
 * DTO dùng cho báo cáo "Món bán chạy".
 *
 * Trường dữ liệu:
 *  - dishId        : ID món ăn
 *  - dishName      : Tên món ăn
 *  - totalQuantity : Tổng số lượng đã bán
 *  - totalRevenue  : Tổng doanh thu mang lại từ món đó
 *
 * API sử dụng:
 *  - GET /api/reports/top-dishes?limit=10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopDishReportItem {

    private Long dishId;               // ID món
    private String dishName;           // Tên món
    private Long totalQuantity;        // Tổng số lượng bán
    private BigDecimal totalRevenue;   // Tổng doanh thu từ món này
}
