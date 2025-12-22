package com.restaurant.api.enums;

/**
 * PaymentStatus
 * ----------------------------------------------------
 * Trạng thái thanh toán.
 *
 * OFFLINE:
 *  - SUCCESS: trả tiền xong ngay (tiền mặt/chuyển khoản thủ công)
 *
 * ONLINE (chuẩn bị kiến trúc):
 *  - PENDING: chờ cổng thanh toán xác nhận
 *  - SUCCESS: thanh toán thành công
 *  - FAILED: thanh toán thất bại
 *  - CANCELED: khách hủy
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELED
}
