package com.restaurant.api.dto.payment;

import com.restaurant.api.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * PaymentRequest – DTO gửi từ FE lên để tạo thanh toán mới.
 * --------------------------------------------------------------------
 * FE sẽ gửi:
 *  - orderId      : ID đơn hàng cần thanh toán
 *  - method       : phương thức thanh toán (CASH / BANK / MOMO / ZALOPAY)
 *  - amount       : số tiền thanh toán (BigDecimal)
 *  - note         : ghi chú thêm (optional)
 *
 * Quy tắc:
 *  - orderId và amount luôn cần thiết
 *  - method phải là giá trị hợp lệ trong enum PaymentMethod
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotNull(message = "orderId không được để trống")
    private Long orderId; // ID của đơn hàng

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod method; // Phương thức thanh toán

    @NotNull(message = "Số tiền thanh toán không được để trống")
    private BigDecimal amount; // Số tiền thanh toán

    private String note; // Ghi chú (optional)

    /**
     * voucherCode – Mã voucher mà người dùng áp dụng cho đơn hàng.
     * ------------------------------------------------------------
     * - Có thể null nếu đơn không dùng voucher.
     * - FE sẽ gửi giá trị này khi người dùng đã áp dụng mã giảm giá.
     */
    private String voucherCode;

}
