package com.restaurant.api.enums;

/**
 * VoucherStatus – Trạng thái hoạt động của Voucher
 * ---------------------------------------------------
 * ACTIVE:     Đang hoạt động, cho phép áp dụng
 * INACTIVE:   Ngừng sử dụng (không cho áp dụng nữa)
 *
 * Lưu ý:
 *  - Trạng thái hết hạn (expired) sẽ được xử lý bằng logic
 *    so sánh ngày start_date / end_date ở tầng Service,
 *    không tạo riêng enum EXPIRED để tránh phức tạp.
 */
public enum VoucherStatus {

    /**
     * Voucher đang hoạt động, đủ điều kiện để áp dụng
     * nếu thỏa các điều kiện khác (thời gian, usageLimit,...)
     */
    ACTIVE,

    /**
     * Voucher đã bị vô hiệu hóa bởi admin, không được áp dụng nữa
     */
    INACTIVE
}
