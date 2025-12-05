package com.restaurant.api.dto.voucher;

import com.restaurant.api.enums.DiscountType;
import com.restaurant.api.enums.VoucherStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VoucherResponse
 * ------------------------------------------------------------
 * DTO trả về cho FE khi:
 *  - Lấy danh sách voucher (list, search)
 *  - Xem chi tiết 1 voucher
 *
 * Chứa đầy đủ thông tin cần hiển thị trên UI.
 */
@Data
@Builder
public class VoucherResponse {

    /**
     * id – Khoá chính của voucher
     */
    private Long id;

    /**
     * code – Mã voucher (duy nhất)
     */
    private String code;

    /**
     * description – Mô tả thông tin voucher
     */
    private String description;

    /**
     * discountType – Loại giảm giá: PERCENT / FIXED
     */
    private DiscountType discountType;

    /**
     * discountValue – Giá trị giảm
     *  - PERCENT: % giảm
     *  - FIXED:   số tiền giảm
     */
    private BigDecimal discountValue;

    /**
     * minOrderAmount – Giá trị đơn hàng tối thiểu để được áp dụng
     */
    private BigDecimal minOrderAmount;

    /**
     * maxDiscountAmount – Số tiền giảm tối đa (nếu có)
     */
    private BigDecimal maxDiscountAmount;

    /**
     * usageLimit – Số lần được sử dụng tối đa
     */
    private Integer usageLimit;

    /**
     * usedCount – Số lần đã sử dụng
     */
    private Integer usedCount;

    /**
     * startDate – Thời điểm bắt đầu hiệu lực
     */
    private LocalDateTime startDate;

    /**
     * endDate – Thời điểm kết thúc hiệu lực
     */
    private LocalDateTime endDate;

    /**
     * status – Trạng thái hoạt động của voucher
     */
    private VoucherStatus status;

    /**
     * createdAt – Thời điểm tạo voucher
     */
    private LocalDateTime createdAt;

    /**
     * updatedAt – Thời điểm cập nhật gần nhất
     */
    private LocalDateTime updatedAt;

    /**
     * isExpired – Flag thể hiện voucher đã quá hạn hay chưa
     * ------------------------------------------------------
     * - TRUE  : now > endDate
     * - FALSE : chưa hết hạn (hoặc chưa kiểm tra)
     *
     * FE có thể dùng field này để hiển thị nhanh mà không cần
     * tự so sánh thời gian.
     */
    private Boolean isExpired;

    /**
     * remainingUsage – Số lần còn lại có thể sử dụng
     * ------------------------------------------------
     * = usageLimit - usedCount
     * Nếu usageLimit = 0 (hiểu là không giới hạn) thì có thể
     * xử lý ở Service, FE chỉ hiển thị theo giá trị nhận được.
     */
    private Integer remainingUsage;
}
