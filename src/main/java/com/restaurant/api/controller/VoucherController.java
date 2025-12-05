package com.restaurant.api.controller;

import com.restaurant.api.dto.voucher.VoucherApplyRequest;
import com.restaurant.api.dto.voucher.VoucherApplyResponse;
import com.restaurant.api.dto.voucher.VoucherRequest;
import com.restaurant.api.dto.voucher.VoucherResponse;
import com.restaurant.api.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * VoucherController – REST API cho Module 17: Voucher (Mã giảm giá)
 * -------------------------------------------------------------------
 * Chức năng chính:
 *  - Quản lý danh sách voucher (CRUD đơn giản)
 *  - API áp dụng voucher lên 1 đơn hàng cụ thể
 *
 * Đường dẫn chung: /api/vouchers
 *
 * Lưu ý:
 *  - Đây là bản cơ bản, chưa có filter/search nâng cao hoặc phân trang.
 *    Sau này nếu cần có thể mở rộng thêm theo Rule 27 (UI/UX list + filter).
 */
@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    // =====================================================================
    // 1. LẤY DANH SÁCH VOUCHER
    // =====================================================================

    /**
     * API: GET /api/vouchers
     * ------------------------------------------------------------
     * Mục đích:
     *  - Lấy toàn bộ danh sách voucher hiện có trong hệ thống.
     *  - FE dùng để hiển thị bảng danh sách voucher (VoucherPage).
     *
     * Ghi chú:
     *  - Hiện tại trả về danh sách đầy đủ (List<VoucherResponse>).
     *  - Sau này có thể đổi sang Page<VoucherResponse> nếu cần phân trang.
     */
    @GetMapping
    public ResponseEntity<List<VoucherResponse>> getAllVouchers() {
        List<VoucherResponse> vouchers = voucherService.getAll();
        return ResponseEntity.ok(vouchers);
    }

    // =====================================================================
    // 2. TẠO VOUCHER MỚI
    // =====================================================================

    /**
     * API: POST /api/vouchers
     * ------------------------------------------------------------
     * Mục đích:
     *  - Tạo mới một voucher.
     *
     * Request body:
     *  - VoucherRequest (code, description, discountType, discountValue, ...)
     *
     * Response:
     *  - VoucherResponse: thông tin voucher sau khi tạo thành công.
     */
    @PostMapping
    public ResponseEntity<VoucherResponse> createVoucher(@RequestBody VoucherRequest request) {
        VoucherResponse created = voucherService.create(request);
        return ResponseEntity.ok(created);
    }

    // =====================================================================
    // 3. CẬP NHẬT VOUCHER
    // =====================================================================

    /**
     * API: PUT /api/vouchers/{id}
     * ------------------------------------------------------------
     * Mục đích:
     *  - Cập nhật thông tin 1 voucher.
     *
     * Quy tắc:
     *  - Không được phép thay đổi code trong khi update (VoucherService đã kiểm tra).
     *
     * Path variable:
     *  - id: ID của voucher cần cập nhật.
     *
     * Request body:
     *  - VoucherRequest: thông tin mới.
     *
     * Response:
     *  - VoucherResponse sau khi cập nhật.
     */
    @PutMapping("/{id}")
    public ResponseEntity<VoucherResponse> updateVoucher(
            @PathVariable Long id,
            @RequestBody VoucherRequest request
    ) {
        VoucherResponse updated = voucherService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    // =====================================================================
    // 4. VÔ HIỆU HÓA VOUCHER (DEACTIVATE)
    // =====================================================================

    /**
     * API: PUT /api/vouchers/{id}/deactivate
     * ------------------------------------------------------------
     * Mục đích:
     *  - Vô hiệu hóa voucher (không cho sử dụng nữa).
     *
     * Ghi chú:
     *  - Không xóa vật lý voucher khỏi DB.
     *  - Chỉ đổi status sang INACTIVE.
     *
     * Path variable:
     *  - id: ID của voucher cần vô hiệu hóa.
     *
     * Response:
     *  - 200 OK nếu xử lý thành công (không cần body).
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateVoucher(@PathVariable Long id) {
        voucherService.deactivate(id);
        return ResponseEntity.ok().build();
    }

    // =====================================================================
    // 5. ÁP DỤNG VOUCHER LÊN ĐƠN HÀNG
    // =====================================================================

    /**
     * API: POST /api/vouchers/apply
     * ------------------------------------------------------------
     * Mục đích:
     *  - Áp dụng voucher lên 1 đơn hàng cụ thể.
     *
     * Request body:
     *  - VoucherApplyRequest:
     *      + orderId:     ID của order cần áp dụng voucher
     *      + voucherCode: mã voucher người dùng nhập
     *
     * Response:
     *  - VoucherApplyResponse:
     *      + orderId
     *      + voucherCode
     *      + originalAmount (tổng tiền ban đầu)
     *      + discountAmount (số tiền được giảm)
     *      + finalAmount (tổng tiền sau giảm)
     *      + message (thông tin mô tả)
     *
     * Lưu ý:
     *  - Hàm này CHỈ tính toán và trả kết quả cho FE.
     *  - Không tăng usedCount trong bước này.
     *    usedCount sẽ được cộng sau khi thanh toán thành công.
     */
    @PostMapping("/apply")
    public ResponseEntity<VoucherApplyResponse> applyVoucher(@RequestBody VoucherApplyRequest request) {
        VoucherApplyResponse response = voucherService.applyVoucher(request);
        return ResponseEntity.ok(response);
    }
}
