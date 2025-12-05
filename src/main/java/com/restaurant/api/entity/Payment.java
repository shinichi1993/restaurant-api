package com.restaurant.api.entity;

import com.restaurant.api.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity Payment – Lưu thông tin thanh toán của 1 Order
 * --------------------------------------------------------------------
 * Mapping theo bảng payment (Flyway V11):
 *
 *  - id             : khóa chính
 *  - order          : đơn hàng được thanh toán
 *  - invoice        : hóa đơn tương ứng
 *  - amount         : số tiền thanh toán (BigDecimal – theo Rule 26)
 *  - method         : phương thức thanh toán (enum PaymentMethod)
 *  - note           : ghi chú thêm (optional)
 *  - paidAt         : thời gian thanh toán
 *  - createdBy      : user thực hiện thanh toán
 *  - createdAt      : thời gian tạo bản ghi
 *  - updatedAt      : thời gian cập nhật
 *
 * QUY TẮC NGHIỆP VỤ:
 *  - Mỗi order khi thanh toán sẽ tạo đúng 1 payment (phiên bản v1).
 *  - Hóa đơn (invoice) phải được tạo trước khi payment được ghi nhận.
 *
 * Tất cả comment tuân thủ Rule 13 (viết tiếng Việt đầy đủ).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    // ---------------------------------------------------------
    // Liên kết Order
    // ---------------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order; // Đơn hàng được thanh toán

    // ---------------------------------------------------------
    // Liên kết Invoice
    // ---------------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice; // Hóa đơn tương ứng

    // ---------------------------------------------------------
    // Số tiền thanh toán
    // Dùng BigDecimal theo Rule 26 để tránh lỗi số thực
    // ---------------------------------------------------------
    @Column(nullable = false)
    private BigDecimal amount;

    // ---------------------------------------------------------
    // Phương thức thanh toán (enum)
    // Lưu dạng string trong DB
    // ---------------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentMethod method;

    // Ghi chú thêm
    @Column(columnDefinition = "TEXT")
    private String note;

    // Thời gian thanh toán
    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    // User ID thực hiện thanh toán (thu ngân)
    @Column(name = "created_by")
    private Long createdBy;

    // Thời gian tạo
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Thời gian cập nhật
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ---------------------------------------------------------
    // Tự động gán thời gian khi tạo mới hoặc cập nhật
    // ---------------------------------------------------------
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (paidAt == null) paidAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
