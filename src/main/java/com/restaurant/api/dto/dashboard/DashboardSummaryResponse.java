package com.restaurant.api.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

/**
 * DashboardSummaryResponse
 * -------------------------------------------------------------
 * DTO tóm tắt nhanh số liệu tổng quan cho màn hình Dashboard.
 *
 * Dùng cho API:
 *  - /api/dashboard/summary
 *
 * Mục đích:
 *  - Giảm số lượng request từ FE (thay vì gọi nhiều API lẻ)
 *  - Gom các số liệu "thẻ nhỏ" (statistic cards) về 1 API.
 *
 * Các trường chính:
 *  - revenueToday        : Doanh thu trong ngày hôm nay
 *  - ordersToday         : Số lượng order trong ngày hôm nay
 *  - totalOrders         : Tổng số order trong hệ thống (optional)
 *  - averageRevenue7Days : Doanh thu trung bình 7 ngày gần nhất
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    /**
     * Doanh thu trong NGÀY HÔM NAY.
     */
    private BigDecimal revenueToday;

    /**
     * Số lượng order được tạo trong NGÀY HÔM NAY.
     */
    private Long ordersToday;

    /**
     * Tổng số order trong hệ thống.
     * Có thể dùng cho thống kê tổng quan (không bắt buộc).
     */
    private Long totalOrders;

    /**
     * Doanh thu TRUNG BÌNH trong 7 ngày gần nhất.
     * Giúp người quản lý so sánh hôm nay với mức trung bình.
     */
    private BigDecimal averageRevenue7Days;
}
