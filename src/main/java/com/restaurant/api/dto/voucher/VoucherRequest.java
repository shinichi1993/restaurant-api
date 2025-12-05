package com.restaurant.api.dto.voucher;

import com.restaurant.api.enums.DiscountType;
import com.restaurant.api.enums.VoucherStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VoucherRequest
 * ------------------------------------------------------------
 * DTO dùng cho các API:
 *  - Tạo mới voucher
 *  - Cập nhật thông tin voucher
 *
 * Lưu ý:
 *  - Các field thời gian dùng LocalDateTime theo Rule 26.
 *  - Các field tiền tệ dùng BigDecimal theo Rule 26.
 */
@Data
public class VoucherRequest {

    /**
     * code – Mã voucher (duy nhất)
     * Ví dụ: "KM10", "TET2025"
     */
    private String code;

    /**
     * description – Mô tả thông tin voucher
     * Ví dụ: "Giảm 10% cho đơn từ 200K"
     */
    private String description;

    /**
     * discountType – Loại giảm giá
     *  - PERCENT: giảm theo %
     *  - FIXED:   giảm theo số tiền cố định
     */
    private DiscountType discountType;

    /**
     * discountValue – Giá trị giảm
     *  - Nếu PERCENT: lưu % (VD: 10, 20)
     *  - Nếu FIXED:   lưu số tiền (VD: 50000)
     */
    private BigDecimal discountValue;

    /**
     * minOrderAmount – Giá trị đơn hàng tối thiểu để được áp dụng voucher
     *  - Nếu null hoặc 0: hiểu là không yêu cầu tối thiểu
     */
    private BigDecimal minOrderAmount;

    /**
     * maxDiscountAmount – Số tiền giảm tối đa (thường dùng cho loại PERCENT)
     *  - Có thể null đối với loại FIXED
     */
    private BigDecimal maxDiscountAmount;

    /**
     * usageLimit – Số lần được phép sử dụng voucher
     *  - Nếu = 0: có thể hiểu là không giới hạn (tùy theo logic nghiệp vụ)
     */
    private Integer usageLimit;

    /**
     * startDate – Thời điểm bắt đầu hiệu lực voucher
     */
    private LocalDateTime startDate;

    /**
     * endDate – Thời điểm kết thúc hiệu lực voucher
     */
    private LocalDateTime endDate;

    /**
     * status – Trạng thái hoạt động của voucher
     *  - ACTIVE   : cho phép áp dụng
     *  - INACTIVE : tạm khóa, không cho áp dụng
     */
    private VoucherStatus status;
}
