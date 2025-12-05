package com.restaurant.api.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;

/**
 * TopDishResponse
 * -------------------------------------------------------------
 * DTO dùng để trả về danh sách MÓN BÁN CHẠY trên Dashboard.
 *
 * Ví dụ:
 *  - Dùng cho API: /api/dashboard/top-dishes
 *  - FE hiển thị dạng bảng: Tên món + Số lượng đã bán + Doanh thu.
 *
 * Quy ước:
 *  - dishId: ID món ăn (để sau này có thể link sang màn hình Dish)
 *  - dishName: tên món
 *  - totalQuantity: tổng số lượng đã bán
 *  - totalRevenue: tổng doanh thu thu được từ món này
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopDishResponse {

    /**
     * ID của món ăn.
     * Có thể dùng để điều hướng sang màn hình chi tiết món sau này.
     */
    private Long dishId;

    /**
     * Tên món ăn hiển thị trên Dashboard.
     */
    private String dishName;

    /**
     * Tổng số lượng món này đã được bán ra.
     */
    private Long totalQuantity;

    /**
     * Tổng doanh thu thu được từ món này.
     * Dùng BigDecimal theo Rule 26.
     */
    private BigDecimal totalRevenue;
}
