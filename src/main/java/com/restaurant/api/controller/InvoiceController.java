package com.restaurant.api.controller;

import com.restaurant.api.dto.invoice.InvoiceResponse;
import com.restaurant.api.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * InvoiceController
 * =====================================================================
 * Controller cung cấp API để lấy thông tin HÓA ĐƠN (Invoice).
 *
 * Lưu ý quan trọng:
 *  - KHÔNG tạo invoice trực tiếp từ controller (Invoice do Payment tạo)
 *  - Controller này chỉ phục vụ việc HIỂN THỊ hóa đơn ra FE
 *
 * API cung cấp:
 *  1. GET /api/invoices/order/{orderId}   → Lấy hóa đơn theo Order
 *  2. GET /api/invoices/{invoiceId}       → Lấy chi tiết hóa đơn
 *
 * Các API đều yêu cầu đăng nhập (phụ thuộc SecurityConfig).
 *
 * Tất cả comment theo Rule 13: tiếng Việt, chi tiết, rõ ràng.
 * =====================================================================
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    // =====================================================================
    // 1. LẤY HÓA ĐƠN THEO ORDER ID
    // =====================================================================

    /**
     * API: Lấy hóa đơn theo orderId
     * ------------------------------------------------------------
     * Dùng trong FE khi:
     *  - Từ OrderPage bấm xem "Hóa đơn"
     *  - Hoặc sau Payment, FE redirect sang trang xem hóa đơn
     *
     * @param orderId ID đơn hàng cần lấy hóa đơn
     * @return InvoiceResponse đầy đủ danh sách món
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<InvoiceResponse> getInvoiceByOrderId(@PathVariable Long orderId) {
        InvoiceResponse res = invoiceService.getInvoiceByOrderId(orderId);
        return ResponseEntity.ok(res);
    }

    // =====================================================================
    // 2. LẤY CHI TIẾT HÓA ĐƠN THEO INVOICE ID
    // =====================================================================

    /**
     * API: Lấy chi tiết hóa đơn theo invoiceId
     * ------------------------------------------------------------
     * Dùng trong FE khi muốn xem chi tiết hóa đơn độc lập
     * (không thông qua order).
     *
     * @param invoiceId ID hóa đơn cần xem
     * @return InvoiceResponse đầy đủ
     */
    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getInvoiceDetail(@PathVariable Long invoiceId) {
        InvoiceResponse res = invoiceService.getInvoiceDetail(invoiceId);
        return ResponseEntity.ok(res);
    }
}
