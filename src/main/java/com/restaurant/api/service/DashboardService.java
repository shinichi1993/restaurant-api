package com.restaurant.api.service;

import com.restaurant.api.dto.dashboard.DashboardSummaryResponse;
import com.restaurant.api.dto.dashboard.RevenueByDateResponse;
import com.restaurant.api.dto.dashboard.TopDishResponse;
import com.restaurant.api.entity.Dish;
import com.restaurant.api.entity.Invoice;
import com.restaurant.api.entity.Order;
import com.restaurant.api.entity.OrderItem;
import com.restaurant.api.repository.DishRepository;
import com.restaurant.api.repository.InvoiceRepository;
import com.restaurant.api.repository.OrderItemRepository;
import com.restaurant.api.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DashboardService
 * --------------------------------------------------------------------
 * Service xử lý toàn bộ logic THỐNG KÊ cho màn hình Dashboard.
 *
 * ⚙ Nhiệm vụ chính:
 *  - Tính doanh thu hôm nay
 *  - Đếm số order hôm nay
 *  - Tính doanh thu 7 ngày gần nhất (dùng cho biểu đồ line chart)
 *  - Lấy danh sách món bán chạy (Top Dish)
 *  - Gom các số liệu summary cho FE (DashboardSummaryResponse)
 *
 * 📌 Lưu ý thiết kế:
 *  - Chỉ đọc dữ liệu, KHÔNG ghi DB → dùng @Transactional(readOnly = true)
 *  - Sử dụng BigDecimal cho tiền theo Rule 26
 *  - Không tạo bảng mới, không cần Flyway cho module Dashboard
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DishRepository dishRepository;

    // =====================================================================
    // 1. API SUMMARY – TỔNG HỢP SỐ LIỆU CHÍNH
    // =====================================================================

    /**
     * Lấy số liệu tổng quan cho Dashboard:
     *  - Doanh thu hôm nay
     *  - Số order hôm nay
     *  - Tổng số order trong hệ thống
     *  - Doanh thu trung bình 7 ngày gần nhất
     *
     * Hàm này sẽ được sử dụng cho API:
     *  - GET /api/dashboard/summary
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {

        BigDecimal revenueToday = getRevenueTodayInternal();
        Long ordersToday = getOrdersTodayInternal();
        Long totalOrders = orderRepository.count();

        // Lấy dữ liệu 7 ngày gần nhất để tính trung bình
        List<RevenueByDateResponse> last7Days = getRevenueLast7DaysInternal();

        BigDecimal avg7Days = BigDecimal.ZERO;
        if (!last7Days.isEmpty()) {
            BigDecimal sum = last7Days.stream()
                    .map(RevenueByDateResponse::getTotalRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Chia cho số ngày có dữ liệu (hoặc 7 ngày, tùy nghiệp vụ)
            int days = last7Days.size();
            if (days > 0) {
                avg7Days = sum
                        .divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP);
            }
        }

        return DashboardSummaryResponse.builder()
                .revenueToday(revenueToday)
                .ordersToday(ordersToday)
                .totalOrders(totalOrders)
                .averageRevenue7Days(avg7Days)
                .build();
    }

    // =====================================================================
    // 2. DOANH THU HÔM NAY
    // =====================================================================

    /**
     * Hàm nội bộ tính doanh thu hôm nay.
     * - Lọc theo trường paidAt của Invoice trong ngày hiện tại.
     * - Chỉ cộng những invoice có paidAt khác null.
     */
    @Transactional(readOnly = true)
    public BigDecimal getRevenueToday() {
        return getRevenueTodayInternal();
    }

    /**
     * Hàm private để tái sử dụng ở nhiều nơi (summary + API riêng).
     */
    private BigDecimal getRevenueTodayInternal() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        // Lấy toàn bộ invoice rồi filter theo khoảng thời gian
        List<Invoice> invoices = invoiceRepository.findAll();

        return invoices.stream()
                .filter(inv -> inv.getPaidAt() != null
                        && !inv.getPaidAt().isBefore(start)
                        && inv.getPaidAt().isBefore(end))
                .map(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // =====================================================================
    // 3. SỐ ORDER HÔM NAY
    // =====================================================================

    /**
     * Đếm số ORDER được tạo trong ngày hôm nay.
     * - Dùng createdAt của Order để so sánh.
     */
    @Transactional(readOnly = true)
    public Long getOrdersToday() {
        return getOrdersTodayInternal();
    }

    private Long getOrdersTodayInternal() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<Order> orders = orderRepository.findAll();

        return orders.stream()
                .filter(o -> o.getCreatedAt() != null
                        && !o.getCreatedAt().isBefore(start)
                        && o.getCreatedAt().isBefore(end))
                .count();
    }

    // =====================================================================
    // 4. DOANH THU 7 NGÀY GẦN NHẤT (DÙNG CHO BIỂU ĐỒ)
    // =====================================================================

    /**
     * Trả về danh sách doanh thu 7 ngày gần nhất.
     * - Bao gồm cả ngày không có doanh thu (total = 0) để biểu đồ không bị đứt đoạn.
     *
     * Dùng cho API:
     *  - GET /api/dashboard/revenue-last-7-days
     */
    @Transactional(readOnly = true)
    public List<RevenueByDateResponse> getRevenueLast7Days() {
        return getRevenueLast7DaysInternal();
    }

    /**
     * Hàm nội bộ để tính doanh thu 7 ngày gần nhất.
     */
    private List<RevenueByDateResponse> getRevenueLast7DaysInternal() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6); // 6 ngày trước + hôm nay = 7 ngày

        // Lấy tất cả invoice một lần, sau đó lọc theo khoảng thời gian
        List<Invoice> allInvoices = invoiceRepository.findAll();

        // Map: LocalDate -> BigDecimal doanh thu
        Map<LocalDate, BigDecimal> revenueByDate = new HashMap<>();

        for (Invoice inv : allInvoices) {
            if (inv.getPaidAt() == null) continue;

            LocalDate d = inv.getPaidAt().toLocalDate();
            // Chỉ quan tâm trong khoảng startDate → today
            if (d.isBefore(startDate) || d.isAfter(today)) continue;

            BigDecimal amount = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;

            revenueByDate.merge(d, amount, BigDecimal::add);
        }

        // Tạo list 7 ngày liên tiếp, nếu không có dữ liệu thì cho 0
        List<RevenueByDateResponse> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = startDate.plusDays(i);
            BigDecimal total = revenueByDate.getOrDefault(d, BigDecimal.ZERO);

            result.add(
                    RevenueByDateResponse.builder()
                            .date(d)
                            .totalRevenue(total)
                            .build()
            );
        }

        return result;
    }

    // =====================================================================
    // 5. TOP MÓN BÁN CHẠY
    // =====================================================================

    /**
     * Lấy danh sách TOP món bán chạy.
     * ------------------------------------------------------------
     * Cách tính:
     *  - Lấy toàn bộ OrderItem trong hệ thống
     *  - Nhóm theo dishId và cộng quantity
     *  - Map sang Dish để lấy tên món + giá
     *  - Tính luôn tổng doanh thu của từng món (price * totalQuantity)
     *  - Sắp xếp giảm dần theo totalQuantity
     *  - Cắt top N (mặc định 5)
     *
     * Dùng cho API:
     *  - GET /api/dashboard/top-dishes
     */
    @Transactional(readOnly = true)
    public List<TopDishResponse> getTopDishes(int limit) {

        List<OrderItem> allItems = orderItemRepository.findAll();

        if (allItems.isEmpty()) {
            return Collections.emptyList();
        }

        // Nhóm theo dishId → tổng quantity
        Map<Long, Long> quantityByDishId = allItems.stream()
                .collect(Collectors.groupingBy(
                        OrderItem::getDishId,
                        Collectors.summingLong(OrderItem::getQuantity)
                ));

        // Lấy danh sách dishId để load Dish 1 lần
        Set<Long> dishIds = quantityByDishId.keySet();
        Map<Long, Dish> dishMap = dishRepository.findAllById(dishIds)
                .stream()
                .collect(Collectors.toMap(Dish::getId, d -> d));

        // Convert sang DTO
        List<TopDishResponse> responses = new ArrayList<>();

        for (Map.Entry<Long, Long> entry : quantityByDishId.entrySet()) {
            Long dishId = entry.getKey();
            Long totalQty = entry.getValue();

            Dish dish = dishMap.get(dishId);
            if (dish == null) continue; // an toàn, tránh lỗi null

            BigDecimal price = dish.getPrice() != null ? dish.getPrice() : BigDecimal.ZERO;
            BigDecimal totalRevenue = price.multiply(BigDecimal.valueOf(totalQty));

            TopDishResponse dto = TopDishResponse.builder()
                    .dishId(dish.getId())
                    .dishName(dish.getName())
                    .totalQuantity(totalQty)
                    .totalRevenue(totalRevenue)
                    .build();

            responses.add(dto);
        }

        // Sắp xếp giảm dần theo totalQuantity
        responses.sort((a, b) -> Long.compare(b.getTotalQuantity(), a.getTotalQuantity()));

        // Cắt top N (nếu N lớn hơn size thì trả hết)
        if (limit > 0 && responses.size() > limit) {
            return responses.subList(0, limit);
        }
        return responses;
    }

    /**
     * Overload: Mặc định lấy TOP 5 món bán chạy.
     */
    @Transactional(readOnly = true)
    public List<TopDishResponse> getTop5Dishes() {
        return getTopDishes(5);
    }
}
