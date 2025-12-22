package com.restaurant.api.dto.orderslip;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderSlipExportData
 * ------------------------------------------------------------
 * DTO chuẩn hóa dữ liệu để export PHIẾU ORDER (không phải invoice)
 * Chỉ chứa dữ liệu – KHÔNG xử lý logic.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OrderSlipExportData {

    // Thông tin cửa hàng
    private String restaurantName;
    private String restaurantAddress;
    private String restaurantPhone;

    // Thông tin order
    private Long orderId;
    private String orderCode;
    private String tableName;
    private LocalDateTime createdAt;
    private String note;

    // Danh sách món
    private List<Item> items;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Item {
        private String dishName;
        private Integer quantity;
        private BigDecimal dishPrice;
        private BigDecimal subtotal;
        private String note;
    }
}
