package com.restaurant.api.dto.invoice;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * InvoiceResponse
 * ------------------------------------------------------------
 * DTO trả chi tiết hóa đơn cho FE.
 *
 * Dùng cho:
 *  - Xem hóa đơn theo orderId
 *  - Xem chi tiết hóa đơn theo invoiceId
 *  - Trả về sau khi thanh toán thành công (Payment sẽ dùng sau)
 *
 * Các trường chính:
 *  - id            : ID hóa đơn
 *  - orderId       : ID order gốc
 *  - totalAmount   : Tổng tiền hóa đơn
 *  - paymentMethod : Phương thức thanh toán (CASH, CARD, MOMO...)
 *  - paidAt        : Thời điểm thanh toán
 *  - createdAt     : Thời điểm tạo hóa đơn
 *  - updatedAt     : Lần cập nhật gần nhất
 *  - items         : Danh sách chi tiết món trong hóa đơn
 *
 * Lưu ý:
 *  - Kiểu thời gian dùng LocalDateTime, khi trả về FE sẽ format
 *    theo chuẩn chung (dd/MM/yyyy HH:mm) ở phía FE.
 * ------------------------------------------------------------
 */
@Data
@Builder
public class InvoiceResponse {

    /**
     * ID hóa đơn.
     */
    private Long id;

    /**
     * ID của đơn hàng gốc dùng để tạo hóa đơn.
     */
    private Long orderId;

    /**
     * Tổng tiền hóa đơn (sau giảm giá, nếu có).
     */
    private BigDecimal totalAmount;

    /**
     * discountAmount – Số tiền giảm nhờ voucher (nếu có).
     */
    private BigDecimal discountAmount;

    /**
     * Tổng tiền gốc (trước giảm giá).
     * - Snapshot từ Invoice.originalTotalAmount
     * - Dùng cho hiển thị "Tổng gốc" trên FE.
     */
    private BigDecimal originalTotalAmount;

    /**
     * Số tiền giảm do voucher.
     */
    private BigDecimal voucherDiscountAmount;

    /**
     * Số tiền giảm do discount mặc định.
     */
    private BigDecimal defaultDiscountAmount;

    /**
     * Tiền khách trả thực tế cho hóa đơn.
     */
    private BigDecimal customerPaid;

    /**
     * Tiền thừa trả lại khách.
     */
    private BigDecimal changeAmount;

    /**
     * voucherCode – Mã voucher đã áp dụng (nếu có).
     */
    private String voucherCode;

    /**
     * Phương thức thanh toán (CASH, CARD, MOMO, BANK_TRANSFER...).
     * Có thể null nếu chưa cập nhật phương thức.
     */
    private String paymentMethod;

    /**
     * Thời điểm thanh toán hóa đơn.
     * Thường là lúc Invoice được tạo ra từ Order.
     */
    private LocalDateTime paidAt;

    /**
     * Thời điểm tạo bản ghi hóa đơn.
     */
    private LocalDateTime createdAt;

    /**
     * Lần cập nhật gần nhất của hóa đơn.
     */
    private LocalDateTime updatedAt;

    /**
     * Danh sách các dòng chi tiết món trên hóa đơn.
     */
    private List<InvoiceItemResponse> items;

    /**
     * Tỷ lệ VAT áp dụng cho hóa đơn (theo %).
     * Ví dụ: 10 = 10% VAT.
     * Giá trị này chỉ để hiển thị, đọc từ cấu hình hệ thống tại thời điểm tính.
     */
    private BigDecimal vatPercent;

    /**
     * Số tiền VAT (tiền thuế) trên hóa đơn.
     * Được tính trên số tiền sau khi trừ giảm giá nhưng trước VAT.
     */
    private BigDecimal vatAmount;

    /**
     * Số tiền hàng hóa sau khi trừ giảm giá, chưa cộng VAT.
     * Dùng cho việc hiển thị chi tiết trên FE (Subtotal).
     */
    private BigDecimal amountBeforeVat;

    // ⭐ Điểm tích lũy khách nhận được cho hóa đơn này
    private Integer loyaltyEarnedPoint;

}
