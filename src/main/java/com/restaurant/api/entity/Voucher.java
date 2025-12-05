package com.restaurant.api.entity;

import com.restaurant.api.enums.DiscountType;
import com.restaurant.api.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Voucher – Entity lưu thông tin mã giảm giá (Module 17)
 * ----------------------------------------------------------------
 * - Ánh xạ với bảng "voucher" trong DB (V17__create_voucher_table.sql)
 * - Dùng cho:
 *   + Quản lý danh sách mã giảm giá (CRUD)
 *   + Áp dụng giảm giá cho đơn hàng (Order)
 *   + Ghi nhận giảm giá trong hóa đơn (Invoice)
 */
@Entity
@Table(name = "voucher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    /**
     * id – Khoá chính tự tăng
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * code – Mã voucher (duy nhất)
     * VD: "KM10", "TET2025", ...
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * description – Mô tả thông tin voucher
     * VD: "Giảm 10% cho đơn từ 200K"
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * discountType – Loại giảm giá (PERCENT hoặc FIXED)
     * Map với cột discount_type (VARCHAR) trong DB
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    /**
     * discountValue – Giá trị giảm
     * - Nếu PERCENT:  lưu % (VD: 10, 20)
     * - Nếu FIXED:    lưu số tiền (VD: 50000)
     *
     * Dùng BigDecimal theo Rule 26 để xử lý tiền tệ chính xác.
     */
    @Column(name = "discount_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue;

    /**
     * minOrderAmount – Giá trị đơn hàng tối thiểu để được áp dụng voucher
     * - Nếu để 0: không yêu cầu tối thiểu
     */
    @Column(name = "min_order_amount", precision = 18, scale = 2)
    private BigDecimal minOrderAmount;

    /**
     * maxDiscountAmount – Số tiền giảm tối đa
     * - Thường dùng cho loại PERCENT để giới hạn trần giảm giá.
     * - Có thể null đối với loại FIXED.
     */
    @Column(name = "max_discount_amount", precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;

    /**
     * usageLimit – Số lần được phép sử dụng voucher
     * - Nếu = 0 có thể hiểu là không giới hạn (tuỳ logic nghiệp vụ sau này).
     */
    @Column(name = "usage_limit", nullable = false)
    private Integer usageLimit;

    /**
     * usedCount – Số lần voucher đã được sử dụng thành công
     * - Mỗi lần thanh toán đơn hàng thành công có áp dụng voucher → +1
     */
    @Column(name = "used_count", nullable = false)
    private Integer usedCount;

    /**
     * startDate – Thời điểm bắt đầu hiệu lực voucher
     */
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    /**
     * endDate – Thời điểm kết thúc hiệu lực voucher
     */
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    /**
     * status – Trạng thái hoạt động của voucher
     * - ACTIVE   : đang cho phép áp dụng
     * - INACTIVE : bị khóa, không cho áp dụng nữa
     *
     * Trạng thái hết hạn sẽ được kiểm tra bằng
     * startDate / endDate trong tầng Service.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VoucherStatus status;

    /**
     * createdAt – Thời điểm tạo voucher
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * updatedAt – Thời điểm cập nhật gần nhất
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Hàm hook tự động set createdAt, updatedAt trước khi insert
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        // Nếu chưa set sẵn usedCount thì default = 0
        if (this.usedCount == null) {
            this.usedCount = 0;
        }
        // Nếu chưa set minOrderAmount thì cho = 0
        if (this.minOrderAmount == null) {
            this.minOrderAmount = BigDecimal.ZERO;
        }
        // Nếu chưa set usageLimit thì cho = 0 (sẽ hiểu logic ở Service)
        if (this.usageLimit == null) {
            this.usageLimit = 0;
        }
    }

    /**
     * Hàm hook tự động cập nhật updatedAt trước khi update
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
