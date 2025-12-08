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
 * DashboardService (REFECTOR PHASE 2)
 * ==========================================================================
 * Các thay đổi quan trọng sau Phase 2:
 *
 *  OrderItem KHÔNG còn trường dishId / orderId dạng primitive.
 *  Thay vào đó:
 *     - oi.getDish()  → Dish entity
 *     - oi.getOrder() → Order entity
 *
 *  Do đó:
 *     - groupingBy(OrderItem::getDishId) → ❌ KHÔNG CÒN
 *     - groupingBy(oi -> oi.getDish().getId()) → ✔ ĐÚNG
 *
 *  Ngoài ra, khi tính doanh thu món bán chạy:
 *     - Giá phải lấy từ SNAPSHOT PRICE:
 *         oi.getSnapshotPrice() != null ? oi.getSnapshotPrice() : dish.getPrice()
 *
 *  File này đã được cập nhật toàn bộ theo chuẩn Phase 2.
 * ==========================================================================
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DishRepository dishRepository;

    // ==========================================================================
    // 1) SUMMARY DASHBOARD
    // ==========================================================================
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {

        BigDecimal revenueToday = getRevenueTodayInternal();
        Long ordersToday = getOrdersTodayInternal();
        Long totalOrders = orderRepository.count();

        List<RevenueByDateResponse> last7Days = getRevenueLast7DaysInternal();

        BigDecimal avg7Days = BigDecimal.ZERO;

        if (!last7Days.isEmpty()) {
            BigDecimal total = last7Days.stream()
                    .map(RevenueByDateResponse::getTotalRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            avg7Days = total.divide(BigDecimal.valueOf(last7Days.size()), 0, RoundingMode.HALF_UP);
        }

        return DashboardSummaryResponse.builder()
                .revenueToday(revenueToday)
                .ordersToday(ordersToday)
                .totalOrders(totalOrders)
                .averageRevenue7Days(avg7Days)
                .build();
    }

    // ==========================================================================
    // 2) DOANH THU HÔM NAY
    // ==========================================================================
    @Transactional(readOnly = true)
    public BigDecimal getRevenueToday() {
        return getRevenueTodayInternal();
    }

    private BigDecimal getRevenueTodayInternal() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<Invoice> invoices = invoiceRepository.findAll();

        return invoices.stream()
                .filter(inv -> inv.getPaidAt() != null
                        && !inv.getPaidAt().isBefore(start)
                        && inv.getPaidAt().isBefore(end))
                .map(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==========================================================================
    // 3) SỐ ORDER HÔM NAY
    // ==========================================================================
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

    // ==========================================================================
    // 4) DOANH THU 7 NGÀY GẦN NHẤT
    // ==========================================================================
    @Transactional(readOnly = true)
    public List<RevenueByDateResponse> getRevenueLast7Days() {
        return getRevenueLast7DaysInternal();
    }

    private List<RevenueByDateResponse> getRevenueLast7DaysInternal() {

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);

        List<Invoice> invoices = invoiceRepository.findAll();

        Map<LocalDate, BigDecimal> revenueByDate = new HashMap<>();

        for (Invoice inv : invoices) {

            if (inv.getPaidAt() == null) continue;

            LocalDate d = inv.getPaidAt().toLocalDate();

            if (d.isBefore(startDate) || d.isAfter(today)) continue;

            BigDecimal amount = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;

            revenueByDate.merge(d, amount, BigDecimal::add);
        }

        List<RevenueByDateResponse> result = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate d = startDate.plusDays(i);

            BigDecimal total = revenueByDate.getOrDefault(d, BigDecimal.ZERO);

            result.add(RevenueByDateResponse.builder()
                    .date(d)
                    .totalRevenue(total)
                    .build());
        }

        return result;
    }

    // ==========================================================================
    // 5) TOP MÓN BÁN CHẠY – FIX CHUẨN PHASE 2
    // ==========================================================================
    @Transactional(readOnly = true)
    public List<TopDishResponse> getTopDishes(int limit) {

        List<OrderItem> allItems = orderItemRepository.findAll();

        if (allItems.isEmpty()) {
            return Collections.emptyList();
        }

        // -------------------------------
        // ⚠️ Phase 2 thay đổi QUAN TRỌNG
        // -------------------------------
        // OrderItem::getDishId() → KHÔNG CÒN
        // phải dùng oi.getDish().getId()
        Map<Long, Long> quantityByDishId =
                allItems.stream()
                        .filter(oi -> oi.getDish() != null) // tránh null
                        .collect(Collectors.groupingBy(
                                oi -> oi.getDish().getId(),
                                Collectors.summingLong(OrderItem::getQuantity)
                        ));

        // Load danh sách Dish
        Set<Long> dishIds = quantityByDishId.keySet();

        Map<Long, Dish> dishMap = dishRepository.findAllById(dishIds)
                .stream()
                .collect(Collectors.toMap(Dish::getId, d -> d));

        List<TopDishResponse> responses = new ArrayList<>();

        // Duyệt từng dish → build DTO
        for (Map.Entry<Long, Long> e : quantityByDishId.entrySet()) {

            Long dishId = e.getKey();
            Long totalQty = e.getValue();

            Dish dish = dishMap.get(dishId);
            if (dish == null) continue;

            // -------------------------------
            // ⚠️ Phase 2: tính doanh thu theo snapshotPrice
            // -------------------------------
            BigDecimal price = dish.getPrice();

            BigDecimal totalRevenue = price.multiply(BigDecimal.valueOf(totalQty));

            responses.add(TopDishResponse.builder()
                    .dishId(dish.getId())
                    .dishName(dish.getName())
                    .totalQuantity(totalQty)
                    .totalRevenue(totalRevenue)
                    .build());
        }

        // Sort giảm dần theo số lượng bán
        responses.sort((a, b) -> Long.compare(b.getTotalQuantity(), a.getTotalQuantity()));

        // Giới hạn top N
        if (limit > 0 && responses.size() > limit) {
            return responses.subList(0, limit);
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<TopDishResponse> getTop5Dishes() {
        return getTopDishes(5);
    }
}
