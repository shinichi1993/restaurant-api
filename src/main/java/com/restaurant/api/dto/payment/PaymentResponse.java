package com.restaurant.api.dto.payment;

import com.restaurant.api.enums.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PaymentResponse – DTO trả về cho FE khi hiển thị Payment.
 * --------------------------------------------------------------------
 * BE trả về thông tin:
 *  - id           : mã payment
 *  - orderId      : ID order đã thanh toán
 *  - invoiceId    : ID hóa đơn liên quan
 *  - amount       : số tiền thanh toán
 *  - method       : phương thức thanh toán
 *  - note         : ghi chú thêm
 *  - paidAt       : thời gian thanh toán
 *  - createdBy    : ai thực hiện thanh toán
 *  - createdAt    : thời gian tạo bản ghi
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long id;              // Mã payment
    private Long orderId;         // ID đơn hàng
    private Long invoiceId;       // ID hóa đơn liên quan
    private BigDecimal amount;    // Số tiền thanh toán
    private PaymentMethod method; // Phương thức thanh toán
    private String note;          // Ghi chú
    private LocalDateTime paidAt; // Thời điểm thanh toán
    private Long createdBy;       // User thực hiện thanh toán
    private LocalDateTime createdAt; // Ngày tạo bản ghi
    private Integer loyaltyEarnedPoint; // Điểm nhận được từ hóa đơn
    private BigDecimal customerPaid;
    private BigDecimal changeAmount;
}
