package com.restaurant.api.dto.backup;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceDto {
    private Long id;
    private Long orderId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal originalTotalAmount;
    private BigDecimal voucherDiscountAmount;
    private BigDecimal defaultDiscountAmount;
    private BigDecimal amountBeforeVat;
    private BigDecimal vatRate;
    private BigDecimal vatAmount;
    private String voucherCode;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer loyaltyEarnedPoint;
    private BigDecimal customerPaid;
    private BigDecimal changeAmount;
}
