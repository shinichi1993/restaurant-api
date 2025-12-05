package com.restaurant.api.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * RevenueReportResponse
 * ------------------------------------------------------------
 * DTO trả về cho FE khi gọi các API báo cáo doanh thu:
 *  - /api/reports/revenue/daily
 *  - /api/reports/revenue/monthly
 *  - /api/reports/revenue/range
 *
 * Trường dữ liệu:
 *  - totalRevenue         : Tổng doanh thu trong khoảng filter
 *  - totalOrders          : Tổng số order trong khoảng đó
 *  - averageRevenuePerDay : Doanh thu trung bình / ngày
 *  - items                : Danh sách doanh thu theo từng ngày
 *
 * Lưu ý:
 *  - FE có thể dùng "items" để vẽ biểu đồ cột / line chart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportResponse {

    private BigDecimal totalRevenue;             // Tổng doanh thu trong khoảng
    private Long totalOrders;                    // Tổng số order
    private BigDecimal averageRevenuePerDay;     // Doanh thu TB/ngày
    private List<RevenueByDayItem> items;        // Chi tiết theo từng ngày
}
