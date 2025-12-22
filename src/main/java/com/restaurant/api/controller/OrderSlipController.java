package com.restaurant.api.controller;

import com.restaurant.api.service.OrderSlipExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OrderSlipController
 * =====================================================================
 * API xuất PHIẾU GỌI MÓN (Order Slip)
 *
 * Dùng cho:
 *  - POS Tablet
 *  - POS Simple
 *
 * KHÔNG phải hóa đơn thanh toán
 * =====================================================================
 */
@RestController
@RequestMapping("/api/order-slips")
@RequiredArgsConstructor
public class OrderSlipController {

    private final OrderSlipExportService orderSlipExportService;

    /**
     * Xuất phiếu gọi món theo orderId
     *
     * @param orderId id của order
     * @return PDF (A5 hoặc Thermal tuỳ setting)
     */
    @GetMapping("/{orderId}/export-pdf")
    public ResponseEntity<byte[]> exportOrderSlip(@PathVariable Long orderId) {

        byte[] pdfBytes = orderSlipExportService.exportOrderSlip(orderId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=order-slip-" + orderId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
