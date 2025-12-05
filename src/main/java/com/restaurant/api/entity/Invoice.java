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
    private BigDecimal totalAmount; // Tổng tiền hóa đơn

    /**
     * discountAmount – Số tiền được giảm nhờ voucher (nếu có).
     * ------------------------------------------------------------
     * - Nếu không áp dụng voucher → có thể để null hoặc 0.
     * - Được dùng để hiển thị chi tiết hóa đơn & báo cáo.
     */
    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

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
