package com.restaurant.api.dto.kitchen;

import com.restaurant.api.enums.OrderItemStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * KitchenItemRealtimeDto – Phase 5.2.4
 * ------------------------------------------------------------
 * DTO dùng CHỈ cho realtime Kitchen.
 *
 * Nguyên tắc:
 *  - KHÔNG dùng Entity
 *  - Chỉ chứa field cần cho bếp
 *  - Không dính Hibernate proxy
 */
@Getter
@Builder
public class KitchenItemRealtimeDto {

    private Long orderItemId;

    private Long orderId;
    private String orderCode;

    private Long tableId;
    private String tableName;

    private Long dishId;
    private String dishName;

    private Integer quantity;

    private OrderItemStatus status;

    private String note;

    private LocalDateTime createdAt;
}
