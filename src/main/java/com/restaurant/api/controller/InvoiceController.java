package com.restaurant.api.controller;

import com.restaurant.api.dto.invoice.InvoiceResponse;
import com.restaurant.api.export.invoice.InvoiceHtmlRenderer;
import com.restaurant.api.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.restaurant.api.dto.invoice.InvoiceExportData;
import com.restaurant.api.export.invoice.InvoicePdfExporterFactory;
import com.restaurant.api.export.invoice.InvoicePrintLayout;
import com.restaurant.api.service.SystemSettingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
    /**
     * Service đọc cấu hình hệ thống (bảng system_setting).
     * ----------------------------------------------------
     * Step A5 dùng để đọc key:
     *  - invoice.print_layout → A5 / THERMAL_80
     * Dùng layout này để chọn exporter tương ứng.
     */
    private final SystemSettingService systemSettingService;

    /**
     * Factory chọn lớp export PDF phù hợp với layout:
     *  - A5          → InvoicePdfExporterA5
     *  - THERMAL_80  → InvoicePdfExporterThermal
     *
     * Controller không gọi trực tiếp từng exporter,
     * mà luôn đi qua factory để giữ code gọn và dễ mở rộng.
     */
    private final InvoicePdfExporterFactory invoicePdfExporterFactory;
    /**
     * Renderer HTML cho hóa đơn, dùng cho chức năng "IN HÓA ĐƠN" POS.
     * --------------------------------------------------------------
     * - KHÁC với PDF exporter (A5 / THERMAL_80).
     * - Renderer này trả về HTML đơn giản để FE dùng window.print().
     */
    private final InvoiceHtmlRenderer invoiceHtmlRenderer;


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

    // =====================================================================
    // 3. EXPORT HÓA ĐƠN RA PDF
    // =====================================================================

    /**
     * API: Xuất PDF hóa đơn theo invoiceId.
     * ------------------------------------------------------------
     * URL:
     *  - GET /api/invoices/{id}/export-pdf
     *
     * Quy trình xử lý:
     *  1) Gọi InvoiceService.buildInvoiceExportData(id)
     *     - Lấy invoice + order + items
     *     - Tính lại totalBeforeDiscount, discount, VAT, finalAmount...
     *     - Lấy thông tin nhà hàng từ SystemSetting (tên, địa chỉ, sđt...)
     *  2) Đọc layout in từ SystemSetting:
     *     - invoice.print_layout = "A5" hoặc "THERMAL_80"
     *  3) Convert String → enum InvoicePrintLayout
     *     - Nếu cấu hình sai → fallback về A5
     *  4) Gọi InvoicePdfExporterFactory.export(layout, data)
     *  5) Trả về file PDF cho FE download.
     *
     * Lưu ý:
     *  - Toàn bộ logic tính tiền nằm ở InvoiceService.buildInvoiceExportData,
     *    exporter chỉ có nhiệm vụ "vẽ" PDF.
     */
    @GetMapping("/{id}/export-pdf")
    public ResponseEntity<byte[]> exportInvoicePdf(@PathVariable Long id) {

        // ------------------------------------------------------------
        // 1. Build dữ liệu export từ InvoiceService
        //    (đã được chuẩn hóa ở Step A1 + A2)
        // ------------------------------------------------------------
        InvoiceExportData data = invoiceService.buildInvoiceExportData(id);

        // ------------------------------------------------------------
        // 2. Đọc layout từ SystemSetting
        //    - Key:  invoice.print_layout
        //    - Value: "A5" hoặc "THERMAL_80"
        //    - Nếu chưa cấu hình → mặc định "A5"
        // ------------------------------------------------------------
        String layoutSetting = systemSettingService
                .getStringSetting("invoice.print_layout", "A5");

        // Convert String → Enum, có xử lý lỗi cấu hình
        InvoicePrintLayout layout;
        try {
            // Chuyển về upper-case để tránh sai khác hoa/thường
            layout = InvoicePrintLayout.valueOf(layoutSetting.toUpperCase());
        } catch (Exception ex) {
            // Nếu cấu hình sai (ví dụ ghi nhầm "A5_PDF"...) → fallback về A5
            layout = InvoicePrintLayout.A5;
        }

        // ------------------------------------------------------------
        // 3. Gọi Factory để export PDF theo layout hợp lệ
        // ------------------------------------------------------------
        byte[] pdfBytes = invoicePdfExporterFactory.export(layout, data);

        // ------------------------------------------------------------
        // 4. Tạo tên file theo chuẩn:
        //    invoice-<id>-yyyyMMdd_HHmmss.pdf
        // ------------------------------------------------------------
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        String fileName = "invoice-" + id + "-" + timestamp + ".pdf";

        // ------------------------------------------------------------
        // 5. Trả file PDF về cho FE bằng ResponseEntity<byte[]>
        // ------------------------------------------------------------
        return ResponseEntity.ok()
                // Header báo cho browser tải file xuống
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                // Content-Type = application/pdf
                .contentType(MediaType.APPLICATION_PDF)
                // Nội dung PDF
                .body(pdfBytes);
    }

    // =====================================================================
    // 4. EXPORT HÓA ĐƠN RA HTML (DÙNG ĐỂ IN TRỰC TIẾP POS)
    // =====================================================================

    /**
     * API: Xuất HTML hóa đơn theo invoiceId.
     * ------------------------------------------------------------
     * URL:
     *  - GET /api/invoices/{id}/export-html
     *
     * Luồng xử lý:
     *  1) Gọi InvoiceService.buildInvoiceExportData(id)
     *     → Lấy dữ liệu hóa đơn đầy đủ (tổng tiền, VAT, voucher, items...)
     *  2) Gọi InvoiceHtmlRenderer.render(data) để ghép HTML theo layout bill 80mm
     *  3) Trả về String HTML cho FE
     *
     * FE sẽ:
     *  - Mở HTML này trong window mới
     *  - Gọi window.print() để in ra máy POS
     */
    @GetMapping("/{id}/export-html")
    public ResponseEntity<String> exportInvoiceHtml(@PathVariable Long id) {
        // Build dữ liệu export (DÙNG CHUNG với PDF)
        InvoiceExportData data = invoiceService.buildInvoiceExportData(id);

        // Render HTML từ renderer
        String html = invoiceHtmlRenderer.render(data);

        // Trả về HTML (Content-Type: text/html)
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                .body(html);
    }
}
