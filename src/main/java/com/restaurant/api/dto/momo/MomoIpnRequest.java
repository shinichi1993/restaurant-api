package com.restaurant.api.dto.momo;

import lombok.*;

import java.math.BigDecimal;

/**
 * MomoIpnRequest
 * ------------------------------------------------------------
 * Payload IPN MoMo POST về ipnUrl.
 * Theo tài liệu MoMo: Content-Type application/json, đối tác trả 204.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomoIpnRequest {

    private String orderType;
    private BigDecimal amount;
    private String partnerCode;
    private String orderId;
    private String extraData;
    private String signature;

    private Long transId;
    private Long responseTime;
    private Integer resultCode;
    private String message;
    private String payType;

    private String requestId;
    private String orderInfo;
}
