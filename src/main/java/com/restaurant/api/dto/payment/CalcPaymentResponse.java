package com.restaurant.api.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * CalcPaymentResponse
 * --------------------------------------------------------
 * DTO trả về kết quả tính toán từ API /api/payments/calc.
 *
 * Dùng để FE hiển thị các dòng:
 *  - Tổng tiền gốc
 *  - Giảm giá từ voucher
 *  - Giảm giá mặc định (default discount)
 *  - Tổng giảm giá
 *  - Tạm tính (sau giảm, trước VAT)
 *  - VAT (% và số tiền)
 *  - Số tiền cuối cùng cần thanh toán (finalAmount)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalcPaymentResponse {

    /**
     * Tổng tiền gốc của order (trước khi giảm giá, chưa VAT).
     */
    private BigDecimal orderTotal;

    /**
     * Số tiền giảm do voucher (nếu có).
     */
    private BigDecimal voucherDiscount;

    /**
     * Số tiền giảm do chính sách giảm mặc định (default discount).
     */
    private BigDecimal defaultDiscount;

    /**
     * Tổng số tiền giảm (voucher + default discount).
     */
    private BigDecimal totalDiscount;

    private BigDecimal amountBeforeRedeem;

    /**
     * Số tiền sau khi trừ toàn bộ giảm giá, chưa cộng VAT.
     */
    private BigDecimal amountAfterDiscount;

    /**
     * Phần trăm VAT áp dụng (ví dụ: 10 = 10%).
     */
    private BigDecimal vatPercent;

    /**
     * Số tiền VAT (tiền thuế) được tính trên amountAfterDiscount.
     */
    private BigDecimal vatAmount;

    /**
     * Số tiền cuối cùng cần thanh toán (amountAfterDiscount + vatAmount).
     * FE phải dùng giá trị này để gửi vào PaymentRequest.amount.
     */
    private BigDecimal finalAmount;

    // ⭐ THÊM FIELD MỚI
    // -------------------------------------------------
    // appliedVoucherCode: mã voucher thực sự được áp dụng
    // Nếu BE không áp dụng voucher → = null
    // -------------------------------------------------
    private String appliedVoucherCode;
    private Integer loyaltyEarnedPoint; // Điểm nhận được từ hóa đơn

    /**
     * Số tiền giảm từ việc dùng điểm hội viên.
     */
    private BigDecimal redeemDiscount;

    /**
     * Số điểm hội viên đã dùng (preview).
     */
    private Integer redeemedPoint;
}
