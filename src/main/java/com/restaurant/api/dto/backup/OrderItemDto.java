package com.restaurant.api.dto.backup;

import com.restaurant.api.enums.OrderItemStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItemDto {
    private Long id;
    private Long orderId;
    private Long dishId;
    private Integer quantity;
    private BigDecimal snapshotPrice;
    private OrderItemStatus status;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
