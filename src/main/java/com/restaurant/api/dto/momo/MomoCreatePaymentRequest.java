package com.restaurant.api.dto.momo;

import lombok.*;

import java.math.BigDecimal;

/**
 * MomoCreatePaymentRequest
 * ------------------------------------------------------------
 * FE gọi để tạo giao dịch MoMo (Sandbox).
 *
 * Quy ước:
 *  - 1 order = 1 payment
 *  - amount FE gửi lên phải khớp với BE calc (anti-cheat)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomoCreatePaymentRequest {
    private Long orderId;
    private BigDecimal amount;

    // Các field giống PaymentRequest để giữ logic calc thống nhất
    private String voucherCode;
    private Long memberId;
    private Integer redeemPoint;

    private String note;
}
