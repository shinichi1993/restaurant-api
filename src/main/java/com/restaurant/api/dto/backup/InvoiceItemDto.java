package com.restaurant.api.dto.backup;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceItemDto {
    private Long id;
    private Long invoiceId;
    private Long dishId;
    private String dishName;
    private BigDecimal dishPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private LocalDateTime createdAt;
}
