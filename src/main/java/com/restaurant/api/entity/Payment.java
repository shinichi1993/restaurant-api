package com.restaurant.api.entity;

import com.restaurant.api.enums.PaymentMethod;
import com.restaurant.api.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity Payment – Lưu thông tin thanh toán của 1 Order
 * --------------------------------------------------------------------
 * BỔ SUNG (ONLINE – MOMO):
 *  - momoOrderId / momoRequestId: định danh giao dịch phía MoMo
 *  - momoTransId / momoResultCode / momoMessage / momoPayType / momoResponseTime: kết quả IPN
 *  - momoPayUrl / momoQrCodeUrl / momoDeeplink: URL trả về từ API create
 *
 * Lưu ý:
 *  - OFFLINE: status = SUCCESS ngay
 *  - ONLINE: status = PENDING lúc tạo, sau IPN mới SUCCESS/FAILED/CANCELED
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
    private Long id;

    // ---------------------------------------------------------
    // Liên kết Order
    // ---------------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // ---------------------------------------------------------
    // Liên kết Invoice (ONLINE có thể null cho đến khi SUCCESS)
    // ---------------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "customer_paid", nullable = false)
    private BigDecimal customerPaid;

    @Column(name = "change_amount", nullable = false)
    private BigDecimal changeAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentMethod method;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ======================================================================
    // ONLINE PAYMENT – MOMO FIELDS
    // ======================================================================

    @Column(name = "momo_order_id", length = 200)
    private String momoOrderId;

    @Column(name = "momo_request_id", length = 200)
    private String momoRequestId;

    @Column(name = "momo_trans_id")
    private Long momoTransId;

    @Column(name = "momo_result_code")
    private Integer momoResultCode;

    @Column(name = "momo_message", columnDefinition = "TEXT")
    private String momoMessage;

    @Column(name = "momo_pay_type", length = 50)
    private String momoPayType;

    @Column(name = "momo_response_time")
    private Long momoResponseTime;

    @Column(name = "momo_extra_data", columnDefinition = "TEXT")
    private String momoExtraData;

    @Column(name = "momo_pay_url", columnDefinition = "TEXT")
    private String momoPayUrl;

    @Column(name = "momo_qr_code_url", columnDefinition = "TEXT")
    private String momoQrCodeUrl;

    @Column(name = "momo_deeplink", columnDefinition = "TEXT")
    private String momoDeeplink;

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
