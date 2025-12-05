package com.restaurant.api.enums;

/**
 * DiscountType – Kiểu giảm giá cho Voucher
 * ---------------------------------------------------
 * PERCENT:  Giảm theo % trên tổng giá trị đơn hàng
 * FIXED:    Giảm theo số tiền cố định
 */
public enum DiscountType {

    /**
     * Giảm theo phần trăm (VD: 10%, 20% ...)
     */
    PERCENT,

    /**
     * Giảm theo số tiền cố định (VD: giảm 50,000 đồng)
     */
    FIXED
}
