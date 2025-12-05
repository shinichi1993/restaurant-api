package com.restaurant.api.controller;

import com.restaurant.api.dto.payment.CalcPaymentRequest;
import com.restaurant.api.dto.payment.CalcPaymentResponse;
import com.restaurant.api.dto.payment.PaymentRequest;
import com.restaurant.api.dto.payment.PaymentResponse;
import com.restaurant.api.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PaymentController
 * --------------------------------------------------------------------
 * Controller xử lý toàn bộ API liên quan đến THANH TOÁN ORDER.
 *
 * API chính:
 *  1) POST /api/payments
 *      → Tạo payment + tự động sinh Invoice + chuyển order → PAID
 *
 *  2) GET /api/payments/{id}
 *      → Lấy chi tiết 1 payment
 *
 *  3) GET /api/payments?from=...&to=...
 *      → Lọc danh sách payment theo khoảng thời gian
 *
 * Ghi chú:
 *  - Principal cung cấp username người đang đăng nhập
 *  - Mọi comment tuân theo Rule 13: viết tiếng Việt đầy đủ
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // =====================================================================
    // 1. TẠO PAYMENT CHO ORDER
    // =====================================================================

    /**
     * API tạo thanh toán cho order.
     * --------------------------------------------------------------
     * FE gửi PaymentRequest:
     *  - orderId      : ID order cần thanh toán
     *  - amount       : số tiền thanh toán (phải khớp totalPrice)
     *  - method       : CASH / BANK / MOMO / ZALOPAY
     *  - note         : optional
     *
     * Vị trí sử dụng:
     *  - FE PaymentPage
     *  - Hoặc FE OrderDetailModal (nếu thanh toán trực tiếp)
     *
     * Hậu quả khi gọi API:
     *  - Tạo Payment
     *  - Tự động sinh Invoice + InvoiceItem
     *  - Chuyển order → PAID
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest req,
            Principal principal
    ) {
        String username = principal.getName();  // Lấy username user đang đăng nhập

        PaymentResponse res = paymentService.createPayment(req, username);
        return ResponseEntity.ok(res);
    }

    // =====================================================================
    // 2. LẤY CHI TIẾT PAYMENT THEO ID
    // =====================================================================

    /**
     * Lấy thông tin đầy đủ của 1 payment.
     * Dùng khi FE muốn truy ngược từ invoice → payment.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    // =====================================================================
    // 3. LỌC PAYMENT THEO NGÀY
    // =====================================================================

    /**
     * Lọc danh sách payment theo khoảng thời gian.
     *
     * Query params:
     *  - from  : ISO datetime, ví dụ 2025-12-01T00:00:00
     *  - to    : ISO datetime
     *
     * Nếu from hoặc to null → trả về toàn bộ payment.
     */
    /*
    @GetMapping
    public ResponseEntity<?> getPayments(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        LocalDateTime fromDate = (from != null ? LocalDateTime.parse(from) : null);
        LocalDateTime toDate = (to != null ? LocalDateTime.parse(to) : null);

        return ResponseEntity.ok(paymentService.getPayments(fromDate, toDate));
    }

     */

    /**
     * API: Lấy danh sách payment theo khoảng ngày
     * --------------------------------------------------------
     * Query param (optional):
     *   from=2025-12-01T00:00:00
     *   to=2025-12-01T23:59:59
     *
     * Nếu không truyền → trả về toàn bộ payment.
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getPayments(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to
    ) {
        // Nếu FE không truyền from/to thì load all
        return ResponseEntity.ok(paymentService.getPayments(from, to));
    }

    /**
     * API tính toán thử số tiền cần thanh toán cho 1 order.
     * -------------------------------------------------------------------
     * POST /api/payments/calc
     *
     * Dùng cho FE PaymentModal:
     *  - Gửi orderId + voucherCode (nếu có)
     *  - BE trả về:
     *      + orderTotal          : Tổng tiền gốc
     *      + voucherDiscount     : Giảm từ voucher
     *      + defaultDiscount     : Giảm mặc định
     *      + totalDiscount       : Tổng giảm
     *      + amountAfterDiscount : Tạm tính (sau giảm, trước VAT)
     *      + vatPercent, vatAmount
     *      + finalAmount         : Số tiền cuối cùng cần thanh toán
     *
     * FE sẽ:
     *  - Dùng dữ liệu này để hiển thị chi tiết
     *  - Khi bấm thanh toán → gửi finalAmount vào PaymentRequest.amount
     */
    @PostMapping("/calc")
    public ResponseEntity<CalcPaymentResponse> calcPayment(@RequestBody CalcPaymentRequest request) {
        CalcPaymentResponse result = paymentService.calcPayment(request);
        return ResponseEntity.ok(result);
    }

}
