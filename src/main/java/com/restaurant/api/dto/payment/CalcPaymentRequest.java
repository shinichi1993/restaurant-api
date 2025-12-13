package com.restaurant.api.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CalcPaymentRequest
 * --------------------------------------------------------
 * DTO dùng cho API /api/payments/calc
 * - Dùng để FE gửi thông tin cần thiết để BE tính thử số tiền
 *   phải thanh toán (chưa tạo payment thật).
 *
 * Trường dữ liệu:
 *  - orderId    : ID của order cần tính toán
 *  - voucherCode: Mã voucher (nếu có), có thể null/empty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalcPaymentRequest {

    /**
     * ID của order cần tính toán thanh toán.
     */
    private Long orderId;

    /**
     * Mã voucher sử dụng để giảm giá (nếu có).
     * Nếu không dùng voucher → FE có thể gửi null hoặc "".
     */
    private String voucherCode;

    /**
     * memberId – ID hội viên (optional)
     * --------------------------------------------------------
     * - FE gửi lên khi user chọn hội viên tại PaymentModal.
     * - Dùng để BE tính loyaltyEarnedPoint và redeem point chính xác.
     */
    private Long memberId;

    /**
     * redeemPoint – Số điểm hội viên muốn dùng (optional)
     * --------------------------------------------------------
     * - FE gửi lên khi user muốn dùng điểm để giảm tiền.
     * - Nếu không dùng điểm → null hoặc 0.
     */
    private Integer redeemPoint;


}
