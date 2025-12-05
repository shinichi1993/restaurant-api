package com.restaurant.api.dto.voucher;

import lombok.Data;

/**
 * VoucherApplyRequest
 * ------------------------------------------------------------
 * DTO dùng cho API áp dụng voucher lên một đơn hàng cụ thể.
 * FE sẽ gửi:
 *  - orderId:    id của Order cần áp dụng voucher
 *  - voucherCode: mã voucher mà người dùng nhập
 */
@Data
public class VoucherApplyRequest {

    /**
     * orderId – ID của đơn hàng cần áp dụng voucher
     */
    private Long orderId;

    /**
     * voucherCode – Mã voucher người dùng nhập trên màn Order
     */
    private String voucherCode;
}
