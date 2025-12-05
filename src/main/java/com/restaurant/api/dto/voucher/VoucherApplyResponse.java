package com.restaurant.api.dto.voucher;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * VoucherApplyResponse
 * ------------------------------------------------------------
 * DTO trả về cho FE sau khi áp dụng voucher thành công.
 * FE dùng dữ liệu này để:
 *  - Cập nhật lại tổng tiền trên màn Order
 *  - Hiển thị số tiền được giảm
 *  - Gửi tiếp voucherCode + finalAmount sang bước thanh toán
 */
@Data
@Builder
public class VoucherApplyResponse {

    /**
     * orderId – ID đơn hàng đã áp dụng voucher
     */
    private Long orderId;

    /**
     * voucherCode – Mã voucher đã áp dụng
     */
    private String voucherCode;

    /**
     * originalAmount – Tổng tiền ban đầu của đơn hàng
     * (chưa áp dụng giảm giá)
     */
    private BigDecimal originalAmount;

    /**
     * discountAmount – Số tiền được giảm nhờ voucher
     */
    private BigDecimal discountAmount;

    /**
     * finalAmount – Tổng tiền cuối cùng sau khi giảm
     * = originalAmount - discountAmount
     */
    private BigDecimal finalAmount;

    /**
     * message – Thông tin mô tả ngắn gọn trả về cho FE
     *  - Ví dụ: "Áp dụng voucher thành công"
     *  - Hoặc mô tả thêm chi tiết về cách tính giảm giá
     */
    private String message;
}
