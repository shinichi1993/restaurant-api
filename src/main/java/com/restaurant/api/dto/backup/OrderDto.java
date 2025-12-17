package com.restaurant.api.dto.backup;

import com.restaurant.api.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderDto {
    private Long id;
    private String orderCode;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private String note;
    private Long createdBy;
    private Long memberId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long tableId; // chỉ lưu id bàn để restore
}
