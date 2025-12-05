package com.restaurant.api.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * RevenueByDateResponse
 * -------------------------------------------------------------
 * DTO dùng để trả về doanh thu theo NGÀY cho màn hình Dashboard.
 *
 * Ví dụ:
 *  - Dùng cho API: /api/dashboard/revenue-last-7-days
 *  - Mỗi bản ghi tương ứng với 1 ngày + tổng doanh thu của ngày đó.
 *
 * Quy ước:
 *  - date: ngày thống kê (LocalDate, không kèm giờ)
 *  - totalRevenue: tổng tiền trong ngày (BigDecimal theo Rule 26)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueByDateResponse {

    /**
     * Ngày thống kê doanh thu.
     * Ví dụ: 2025-12-01
     */
    private LocalDate date;

    /**
     * Tổng doanh thu trong ngày tương ứng.
     * Sử dụng BigDecimal để tránh lỗi làm tròn số (Rule 26).
     */
    private BigDecimal totalRevenue;
}
