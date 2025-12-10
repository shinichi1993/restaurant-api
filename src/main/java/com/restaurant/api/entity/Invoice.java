package com.restaurant.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Invoice – Entity bảng invoice
 * ------------------------------------------------------------
 * Lưu thông tin hóa đơn được tạo khi thanh toán Order.
 *
 * Cấu trúc:
 *  - Mỗi Order chỉ có 1 Invoice (quan hệ 1–1)
 *  - Một Invoice có nhiều InvoiceItem (1–n)
 *
 * Các trường quan trọng:
 *  - orderId        : ID order gốc
 *  - totalAmount    : Tổng tiền hóa đơn
 *  - paymentMethod  : Phương thức thanh toán
 *  - paidAt         : Thời điểm thanh toán
 *
 * createdAt / updatedAt:
 *  - Tự gán tại thời điểm tạo và cập nhật
 *
 * Tuân thủ Rule 13: toàn bộ comment bằng tiếng Việt.
 * ------------------------------------------------------------
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "order_id", nullable = false)
    private Long orderId; // ID đơn hàng gốc

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount; // Tổng tiền hóa đơn (sau giảm, đã VAT) – finalAmount snapshot

    /**
     * discountAmount – Tổng số tiền giảm (voucher + discount mặc định).
     * ------------------------------------------------------------
     * - Đây là TỔNG giảm giá trên hóa đơn.
     * - Chi tiết từng loại giảm được tách riêng:
     *      + voucherDiscountAmount
     *      + defaultDiscountAmount
     */
    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    /**
     * originalTotalAmount – Tổng tiền gốc TRƯỚC khi áp dụng bất kỳ giảm giá nào.
     * ------------------------------------------------------------
     * - Thường là tổng tiền món (sum dishPrice * quantity).
     * - Dùng để hiển thị "Tổng gốc (trước giảm)" trên hóa đơn / báo cáo.
     */
    @Column(name = "original_total_amount")
    private BigDecimal originalTotalAmount;

    /**
     * voucherDiscountAmount – Số tiền giảm do voucher.
     * ------------------------------------------------------------
     * - Nếu không có voucher → 0.
     * - Nếu có nhiều loại voucher → tổng hợp lại.
     */
    @Column(name = "voucher_discount_amount")
    private BigDecimal voucherDiscountAmount;

    /**
     * defaultDiscountAmount – Số tiền giảm do discount mặc định (SystemSetting).
     * ------------------------------------------------------------
     * - Ví dụ: giảm 5% cho tất cả hóa đơn.
     */
    @Column(name = "default_discount_amount")
    private BigDecimal defaultDiscountAmount;

    /**
     * amountBeforeVat – Số tiền sau khi trừ toàn bộ giảm giá nhưng CHƯA cộng VAT.
     * ------------------------------------------------------------
     * - Đây là cơ sở để tính VAT: vatAmount = amountBeforeVat * vatRate.
     */
    @Column(name = "amount_before_vat")
    private BigDecimal amountBeforeVat;

    /**
     * vatRate – Tỷ lệ VAT snapshot (%).
     * ------------------------------------------------------------
     * - Ví dụ: 10 = 10%.
     * - Lưu lại tại thời điểm xuất hóa đơn, không phụ thuộc vào setting thay đổi sau này.
     */
    @Column(name = "vat_rate")
    private BigDecimal vatRate;

    /**
     * vatAmount – Số tiền VAT snapshot.
     * ------------------------------------------------------------
     * - Được tính tại PaymentService, lưu cố định trong invoice.
     */
    @Column(name = "vat_amount")
    private BigDecimal vatAmount;

    /**
     * voucherCode – Mã voucher đã áp dụng cho hóa đơn (nếu có).
     * ------------------------------------------------------------
     * - Lưu để tra cứu lại thông tin voucher sau này.
     */
    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Column(name = "payment_method")
    private String paymentMethod; // Phương thức thanh toán: CASH, CARD, MOMO...

    @Column(name = "paid_at")
    private LocalDateTime paidAt; // Thời điểm thanh toán

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // Thời điểm tạo hóa đơn

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // Lần cập nhật gần nhất

    @Column(name = "loyalty_earned_point")
    private Integer loyaltyEarnedPoint; // ⭐ Số điểm tích được cho hóa đơn này

    /**
     * customerPaid – Số tiền khách hàng thực tế đã trả cho hóa đơn này.
     * ------------------------------------------------------------
     * - Snapshot từ Payment.customerPaid.
     * - Dùng để hiển thị lại trên hóa đơn / báo cáo.
     */
    @Column(name = "customer_paid")
    private BigDecimal customerPaid;

    /**
     * changeAmount – Tiền thừa đã trả lại khách.
     * ------------------------------------------------------------
     * - Snapshot từ Payment.changeAmount.
     */
    @Column(name = "change_amount")
    private BigDecimal changeAmount;

    /**
     * Quan hệ 1 invoice → n invoice_item
     */
    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    private List<InvoiceItem> items;

    /**
     * Gán createdAt và updatedAt trước khi insert
     */
    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    /**
     * Gán updatedAt mỗi lần update
     */
    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
