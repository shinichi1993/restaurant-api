package com.restaurant.api.enums;

/**
 * PaymentMethod
 * ----------------------------------------------------
 * Quy ước:
 *  - OFFLINE: thanh toán xong ngay tại quầy
 *  - ONLINE: chỉ chuẩn bị enum, CHƯA triển khai cổng thanh toán
 *
 * OFFLINE:
 *  - CASH: Tiền mặt
 *  - BANK_MANUAL: Chuyển khoản thủ công (thu ngân tự xác nhận)
 *
 * ONLINE (PREPARE ONLY):
 *  - MOMO
 *  - VNPAY
 *  - CREDIT
 */
public enum PaymentMethod {
    CASH,
    BANK_MANUAL,

    // ===== ONLINE PAYMENT (CHUẨN BỊ KIẾN TRÚC) =====
    MOMO,
    VNPAY,
    CREDIT
}
