package com.restaurant.api.controller;

import com.restaurant.api.dto.dashboard.DashboardSummaryResponse;
import com.restaurant.api.dto.dashboard.RevenueByDateResponse;
import com.restaurant.api.dto.dashboard.TopDishResponse;
import com.restaurant.api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DashboardController
 * ------------------------------------------------------------------
 * Controller cung cấp các API THỐNG KÊ cho màn hình Dashboard.
 *
 * Các API chính:
 *  1) GET /api/dashboard/summary
 *     → Trả về số liệu tổng quan (doanh thu hôm nay, số order hôm nay...)
 *
 *  2) GET /api/dashboard/revenue-today
 *     → Doanh thu trong NGÀY HÔM NAY
 *
 *  3) GET /api/dashboard/orders-today
 *     → Số lượng order trong NGÀY HÔM NAY
 *
 *  4) GET /api/dashboard/revenue-last-7-days
 *     → Danh sách doanh thu 7 ngày gần nhất (dùng cho line chart)
 *
 *  5) GET /api/dashboard/top-dishes
 *     → Danh sách món bán chạy (mặc định TOP 5)
 *
 * Lưu ý:
 *  - Toàn bộ API này yêu cầu JWT (đã cấu hình trong SecurityConfig).
 *  - Chỉ đọc dữ liệu (không ghi DB).
 *  - Tất cả comment đều sử dụng tiếng Việt theo Rule 13.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // =====================================================================
    // 1. SUMMARY – TỔNG QUAN DASHBOARD
    // =====================================================================

    /**
     * API trả về số liệu tổng quan cho Dashboard.
     * ----------------------------------------------------------
     * Dùng ở FE cho các "thẻ nhỏ" (statistic cards) đầu màn hình:
     *  - Doanh thu hôm nay
     *  - Số order hôm nay
     *  - Tổng số order
     *  - Doanh thu trung bình 7 ngày gần nhất
     *
     * GET /api/dashboard/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        DashboardSummaryResponse dto = dashboardService.getSummary();
        return ResponseEntity.ok(dto);
    }

    // =====================================================================
    // 2. DOANH THU HÔM NAY
    // =====================================================================

    /**
     * API trả về DOANH THU của NGÀY HÔM NAY.
     * ----------------------------------------------------------
     * Dùng ở FE khi muốn hiển thị riêng doanh thu hôm nay
     * hoặc so sánh với các ngày khác.
     *
     * GET /api/dashboard/revenue-today
     */
    @GetMapping("/revenue-today")
    public ResponseEntity<BigDecimal> getRevenueToday() {
        BigDecimal revenue = dashboardService.getRevenueToday();
        return ResponseEntity.ok(revenue);
    }

    // =====================================================================
    // 3. SỐ ORDER HÔM NAY
    // =====================================================================

    /**
     * API trả về SỐ LƯỢNG ORDER được tạo trong NGÀY HÔM NAY.
     * ----------------------------------------------------------
     * GET /api/dashboard/orders-today
     */
    @GetMapping("/orders-today")
    public ResponseEntity<Long> getOrdersToday() {
        Long count = dashboardService.getOrdersToday();
        return ResponseEntity.ok(count);
    }

    // =====================================================================
    // 4. DOANH THU 7 NGÀY GẦN NHẤT
    // =====================================================================

    /**
     * API trả về danh sách DOANH THU 7 NGÀY GẦN NHẤT.
     * ----------------------------------------------------------
     * Dùng trực tiếp cho biểu đồ line chart ở màn hình Dashboard.
     *
     * Mỗi phần tử trong list:
     *  - date: ngày thống kê (LocalDate)
     *  - totalRevenue: tổng doanh thu ngày đó
     *
     * GET /api/dashboard/revenue-last-7-days
     */
    @GetMapping("/revenue-last-7-days")
    public ResponseEntity<List<RevenueByDateResponse>> getRevenueLast7Days() {
        List<RevenueByDateResponse> list = dashboardService.getRevenueLast7Days();
        return ResponseEntity.ok(list);
    }

    // =====================================================================
    // 5. TOP MÓN BÁN CHẠY
    // =====================================================================

    /**
     * API trả về danh sách MÓN BÁN CHẠY.
     * ----------------------------------------------------------
     * Query param:
     *  - limit (optional): số lượng món muốn lấy, mặc định = 5
     *
     * Mỗi phần tử trong list:
     *  - dishId        : ID món
     *  - dishName      : tên món
     *  - totalQuantity : tổng số lượng bán
     *  - totalRevenue  : tổng doanh thu từ món đó
     *
     * GET /api/dashboard/top-dishes
     * GET /api/dashboard/top-dishes?limit=10
     */
    @GetMapping("/top-dishes")
    public ResponseEntity<List<TopDishResponse>> getTopDishes(
            @RequestParam(name = "limit", required = false, defaultValue = "5") int limit
    ) {
        List<TopDishResponse> list = dashboardService.getTopDishes(limit);
        return ResponseEntity.ok(list);
    }
}
