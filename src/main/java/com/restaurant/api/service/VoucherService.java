package com.restaurant.api.service;

import com.restaurant.api.dto.voucher.VoucherApplyRequest;
import com.restaurant.api.dto.voucher.VoucherApplyResponse;
import com.restaurant.api.dto.voucher.VoucherRequest;
import com.restaurant.api.dto.voucher.VoucherResponse;
import com.restaurant.api.entity.Order;
import com.restaurant.api.entity.Voucher;
import com.restaurant.api.enums.DiscountType;
import com.restaurant.api.enums.VoucherStatus;
import com.restaurant.api.repository.OrderRepository;
import com.restaurant.api.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * VoucherService – Xử lý toàn bộ nghiệp vụ của Module 17: Voucher (Mã giảm giá)
 * -----------------------------------------------------------------------------
 * Các chức năng chính:
 *  - CRUD Voucher: tạo, cập nhật, lấy danh sách, vô hiệu hóa
 *  - Áp dụng voucher lên 1 đơn hàng (Order) để tính số tiền được giảm
 *
 * Lưu ý:
 *  - Không tăng usedCount trong hàm applyVoucher.
 *    usedCount chỉ tăng khi thanh toán thành công (ở Payment/Invoice).
 *  - Tất cả lỗi nghiệp vụ đều dùng RuntimeException với message tiếng Việt
 *    để thống nhất với các Service khác hiện tại.
 */
@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final OrderRepository orderRepository;

    // =====================================================================
    // 1. LẤY DANH SÁCH VOUCHER
    // =====================================================================

    /**
     * Lấy toàn bộ danh sách voucher.
     * (Bản đơn giản, không filter – sau này có thể mở rộng filter theo code, status, ngày...)
     */
    public List<VoucherResponse> getAll() {
        return voucherRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // =====================================================================
    // 2. TẠO VOUCHER MỚI
    // =====================================================================

    /**
     * Tạo mới 1 voucher.
     * ------------------------------------------------------------
     * Validate chính:
     *  - Không trùng code
     *  - discountValue > 0
     *  - startDate < endDate
     */
    public VoucherResponse create(VoucherRequest req) {

        // Kiểm tra trùng code
        if (voucherRepository.findByCode(req.getCode()).isPresent()) {
            throw new RuntimeException("Mã voucher đã tồn tại trong hệ thống");
        }

        validateVoucherRequest(req);

        // Nếu minOrderAmount null → set = 0
        BigDecimal minOrderAmount = req.getMinOrderAmount() != null
                ? req.getMinOrderAmount()
                : BigDecimal.ZERO;

        // Nếu usageLimit null → set = 0 (hiểu là không giới hạn hoặc cấu hình riêng ở BE)
        Integer usageLimit = req.getUsageLimit() != null ? req.getUsageLimit() : 0;

        Voucher voucher = Voucher.builder()
                .code(req.getCode())
                .description(req.getDescription())
                .discountType(req.getDiscountType())
                .discountValue(req.getDiscountValue())
                .minOrderAmount(minOrderAmount)
                .maxDiscountAmount(req.getMaxDiscountAmount())
                .usageLimit(usageLimit)
                .usedCount(0)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                // Nếu FE không truyền status → mặc định ACTIVE
                .status(req.getStatus() != null ? req.getStatus() : VoucherStatus.ACTIVE)
                .build();

        Voucher saved = voucherRepository.save(voucher);
        return toResponse(saved);
    }

    // =====================================================================
    // 3. CẬP NHẬT VOUCHER
    // =====================================================================

    /**
     * Cập nhật thông tin voucher.
     * --------------------------------------------------------------
     * Quy tắc:
     *  - Không cho phép đổi code (tránh rối dữ liệu)
     *  - Có thể cho đổi description, discount, thời gian, limit...
     */
    public VoucherResponse update(Long id, VoucherRequest req) {

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));

        // Không cho phép đổi code
        if (!voucher.getCode().equals(req.getCode())) {
            throw new RuntimeException("Không được phép thay đổi mã voucher");
        }

        validateVoucherRequest(req);

        BigDecimal minOrderAmount = req.getMinOrderAmount() != null
                ? req.getMinOrderAmount()
                : BigDecimal.ZERO;

        Integer usageLimit = req.getUsageLimit() != null ? req.getUsageLimit() : 0;

        voucher.setDescription(req.getDescription());
        voucher.setDiscountType(req.getDiscountType());
        voucher.setDiscountValue(req.getDiscountValue());
        voucher.setMinOrderAmount(minOrderAmount);
        voucher.setMaxDiscountAmount(req.getMaxDiscountAmount());
        voucher.setUsageLimit(usageLimit);
        voucher.setStartDate(req.getStartDate());
        voucher.setEndDate(req.getEndDate());

        if (req.getStatus() != null) {
            voucher.setStatus(req.getStatus());
        }

        Voucher saved = voucherRepository.save(voucher);
        return toResponse(saved);
    }

    // =====================================================================
    // 4. VÔ HIỆU HÓA VOUCHER (DEACTIVATE)
    // =====================================================================

    /**
     * Vô hiệu hóa voucher (chuyển status → INACTIVE).
     * Không xóa vật lý.
     */
    public void deactivate(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));

        voucher.setStatus(VoucherStatus.INACTIVE);
        voucherRepository.save(voucher);
    }

    // =====================================================================
    // 5. ÁP DỤNG VOUCHER LÊN ĐƠN HÀNG
    // =====================================================================

    /**
     * Áp dụng voucher lên 1 order cụ thể.
     * ------------------------------------------------------------------
     * Bước xử lý:
     *  - B1: Lấy order từ DB
     *  - B2: Tìm voucher theo code + ACTIVE
     *  - B3: Kiểm tra thời gian hiệu lực (startDate <= now <= endDate)
     *  - B4: Kiểm tra usageLimit/usedCount
     *  - B5: Kiểm tra minOrderAmount
     *  - B6: Tính discountAmount theo loại PERCENT / FIXED
     *  - B7: Trả về VoucherApplyResponse cho FE
     *
     * Lưu ý:
     *  - Hàm này CHỈ tính toán, KHÔNG tăng usedCount.
     *    usedCount chỉ tăng sau khi thanh toán thành công.
     */
    public VoucherApplyResponse applyVoucher(VoucherApplyRequest req) {

        if (req.getOrderId() == null || req.getVoucherCode() == null) {
            throw new RuntimeException("Thiếu thông tin orderId hoặc voucherCode");
        }

        // B1: Lấy order
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order để áp dụng voucher"));

        BigDecimal orderTotal = order.getTotalPrice();
        if (orderTotal == null) {
            throw new RuntimeException("Order không có tổng tiền hợp lệ");
        }

        // B2: Tìm voucher đang ACTIVE theo code
        Voucher voucher = voucherRepository.findByCodeAndStatus(
                        req.getVoucherCode(),
                        VoucherStatus.ACTIVE
                )
                .orElseThrow(() -> new RuntimeException("Mã voucher không tồn tại hoặc không hoạt động"));

        LocalDateTime now = LocalDateTime.now();

        // B3: Kiểm tra thời gian hiệu lực
        if (now.isBefore(voucher.getStartDate()) || now.isAfter(voucher.getEndDate())) {
            throw new RuntimeException("Voucher đã hết hạn hoặc chưa đến thời gian sử dụng");
        }

        // B4: Kiểm tra usageLimit
        // Nếu usageLimit > 0 thì mới check usedCount (0 hiểu là không giới hạn)
        if (voucher.getUsageLimit() != null
                && voucher.getUsageLimit() > 0
                && voucher.getUsedCount() != null
                && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new RuntimeException("Voucher đã sử dụng vượt quá số lần cho phép");
        }

        // B5: Kiểm tra minOrderAmount
        BigDecimal minOrderAmount = voucher.getMinOrderAmount() != null
                ? voucher.getMinOrderAmount()
                : BigDecimal.ZERO;
        if (orderTotal.compareTo(minOrderAmount) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher");
        }

        // B6: Tính số tiền giảm
        BigDecimal discountAmount = calculateDiscountAmount(voucher, orderTotal);

        // Đảm bảo discount không âm và không vượt quá orderTotal
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }
        if (discountAmount.compareTo(orderTotal) > 0) {
            discountAmount = orderTotal;
        }

        BigDecimal finalAmount = orderTotal.subtract(discountAmount);

        // B7: Trả về kết quả
        return VoucherApplyResponse.builder()
                .orderId(order.getId())
                .voucherCode(voucher.getCode())
                .originalAmount(orderTotal)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .message("Áp dụng voucher thành công")
                .build();
    }

    // =====================================================================
    // 6. HÀM HỖ TRỢ – VALIDATE VOUCHER REQUEST
    // =====================================================================

    /**
     * Hàm validate chung cho create/update voucher.
     * ------------------------------------------------------------
     * Kiểm tra:
     *  - discountValue > 0
     *  - startDate < endDate
     */
    private void validateVoucherRequest(VoucherRequest req) {
        if (req.getDiscountValue() == null || req.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giá trị giảm giá phải lớn hơn 0");
        }

        if (req.getStartDate() == null || req.getEndDate() == null) {
            throw new RuntimeException("Thời gian bắt đầu/kết thúc không được để trống");
        }

        if (!req.getEndDate().isAfter(req.getStartDate())) {
            throw new RuntimeException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        if (req.getDiscountType() == null) {
            throw new RuntimeException("Loại giảm giá không được để trống");
        }
    }

    // =====================================================================
    // 7. HÀM HỖ TRỢ – TÍNH SỐ TIỀN GIẢM
    // =====================================================================

    /**
     * Tính số tiền giảm theo cấu hình voucher.
     * ------------------------------------------------------------
     *  - Nếu PERCENT:
     *      discount = orderTotal * (discountValue / 100)
     *      Nếu có maxDiscountAmount → áp dụng trần
     *  - Nếu FIXED:
     *      discount = discountValue
     */
    private BigDecimal calculateDiscountAmount(Voucher voucher, BigDecimal orderTotal) {
        BigDecimal discount = BigDecimal.ZERO;

        if (voucher.getDiscountType() == DiscountType.PERCENT) {
            // discount = total * (percent / 100)
            BigDecimal percent = voucher.getDiscountValue()
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            discount = orderTotal.multiply(percent);

            // Nếu có cấu hình giảm tối đa → giới hạn lại
            if (voucher.getMaxDiscountAmount() != null
                    && voucher.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                    && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discount = voucher.getMaxDiscountAmount();
            }

        } else if (voucher.getDiscountType() == DiscountType.FIXED) {
            discount = voucher.getDiscountValue();
        }

        return discount;
    }

    // =====================================================================
    // 8. HÀM HỖ TRỢ – MAP ENTITY → DTO RESPONSE
    // =====================================================================

    /**
     * Convert Entity Voucher → DTO VoucherResponse
     * ------------------------------------------------------------
     * Đồng thời tính thêm:
     *  - isExpired: hết hạn chưa
     *  - remainingUsage: số lần sử dụng còn lại
     */
    private VoucherResponse toResponse(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();

        boolean isExpired = false;
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            isExpired = true;
        }

        Integer usageLimit = voucher.getUsageLimit() != null ? voucher.getUsageLimit() : 0;
        Integer usedCount = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;

        Integer remainingUsage = null;
        if (usageLimit > 0) {
            remainingUsage = Math.max(usageLimit - usedCount, 0);
        }

        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .description(voucher.getDescription())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .minOrderAmount(voucher.getMinOrderAmount())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .usageLimit(usageLimit)
                .usedCount(usedCount)
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .status(voucher.getStatus())
                .createdAt(voucher.getCreatedAt())
                .updatedAt(voucher.getUpdatedAt())
                .isExpired(isExpired)
                .remainingUsage(remainingUsage)
                .build();
    }

    /**
     * Tăng số lần sử dụng (usedCount) cho voucher sau khi thanh toán thành công.
     * ------------------------------------------------------------------------
     * - Được gọi từ PaymentService.createPayment(...)
     * - Chỉ tăng 1 lần cho mỗi lần thanh toán thành công.
     */
    public void increaseUsedCount(String voucherCode) {
        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            return;
        }

        Voucher voucher = voucherRepository.findByCode(voucherCode.trim())
                .orElse(null);

        if (voucher == null) {
            // Không tìm thấy voucher tương ứng → bỏ qua, không cần ném lỗi
            return;
        }

        Integer usedCount = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        voucher.setUsedCount(usedCount + 1);

        voucherRepository.save(voucher);
    }
}
