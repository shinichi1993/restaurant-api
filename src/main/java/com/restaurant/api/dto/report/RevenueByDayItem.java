package com.restaurant.api.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * RevenueByDayItem
 * ------------------------------------------------------------
 * DTO dùng để biểu diễn doanh thu của 1 ngày cụ thể.
 *
 * Sẽ được dùng trong:
 *  - Báo cáo doanh thu 7 ngày gần nhất
 *  - Báo cáo doanh thu theo khoảng ngày
 *
 * Trường dữ liệu:
 *  - date         : Ngày (yyyy-MM-dd)
 *  - revenue      : Tổng doanh thu trong ngày
 *  - orderCount   : Số lượng đơn hàng trong ngày
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueByDayItem {

    private LocalDate date;          // Ngày
    private BigDecimal revenue;      // Doanh thu của ngày
    private Long orderCount;         // Số order trong ngày
}
