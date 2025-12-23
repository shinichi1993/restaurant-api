package com.restaurant.api.dto.momo;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomoCreatePaymentResponse {
    private Long paymentId;
    private String momoOrderId;
    private String momoRequestId;

    private String payUrl;
    private String qrCodeUrl;
    private String deeplink;

    private String message;
}
