package com.restaurant.api.dto.backup;

import com.restaurant.api.enums.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentDto {
    private Long id;
    private Long orderId;
    private Long invoiceId;
    private BigDecimal amount;
    private BigDecimal customerPaid;
    private BigDecimal changeAmount;
    private String method;
    private String note;
    private LocalDateTime paidAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
