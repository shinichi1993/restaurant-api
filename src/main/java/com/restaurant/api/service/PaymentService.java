package com.restaurant.api.service;

import com.restaurant.api.dto.notification.CreateNotificationRequest;
import com.restaurant.api.dto.payment.CalcPaymentRequest;
import com.restaurant.api.dto.payment.CalcPaymentResponse;
import com.restaurant.api.dto.payment.PaymentRequest;
import com.restaurant.api.dto.payment.PaymentResponse;
import com.restaurant.api.entity.*;
import com.restaurant.api.enums.AuditAction;
import com.restaurant.api.enums.NotificationType;
import com.restaurant.api.enums.OrderStatus;
import com.restaurant.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant.api.service.VoucherService;
import com.restaurant.api.dto.voucher.VoucherApplyRequest;
import com.restaurant.api.dto.voucher.VoucherApplyResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.math.RoundingMode;


/**
 * PaymentService
 * ------------------------------------------------------------------------
 * Service xử lý toàn bộ nghiệp vụ THANH TOÁN:
 *
 * 1) Tạo payment cho một order:
 *    - Kiểm tra order tồn tại
 *    - Kiểm tra order đã ở trạng thái SERVING hay chưa
 *    - Kiểm tra số tiền thanh toán có khớp với totalPrice của order
 *    - Tạo record Payment
 *    - Tạo Invoice + InvoiceItem (gọi InvoiceService)
 *    - Cập nhật trạng thái order → PAID
 *
 * 2) Lấy thông tin payment theo ID
 *
 * 3) Lấy danh sách payment theo khoảng ngày
 *
 * Ghi chú quan trọng:
 * - Một order chỉ được thanh toán duy nhất một lần
 * - Khi thanh toán xong → Invoice phải được sinh tự động
 * - Mọi comment tuân theo Rule 13: viết tiếng Việt đầy đủ
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InvoiceService invoiceService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final RestaurantTableService restaurantTableService;
    private final VoucherService voucherService;
    private final SystemSettingService systemSettingService;

    // =====================================================================
    // 1. TẠO PAYMENT CHO ORDER
    // =====================================================================

    /**
     * Tạo payment cho 1 order.
     * ------------------------------------------------------------
     * Quy trình:
     *  - B1: Lấy order từ DB
     *  - B2: Kiểm tra order chưa thanh toán trước đó
     *  - B3: Kiểm tra trạng thái order = SERVING (đang phục vụ)
     *  - B4: Kiểm tra số tiền hợp lệ
     *  - B5: Tạo Payment
     *  - B6: Gọi InvoiceService để tạo hóa đơn
     *  - B7: Cập nhật trạng thái order → PAID
     */
    @Transactional
        public PaymentResponse createPayment(PaymentRequest req, String username) {

        // B1: Tìm order
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

        // B2: Kiểm tra trạng thái
        if (order.getStatus() == OrderStatus.PAID) {
            throw new RuntimeException("Order này đã thanh toán trước đó");
        }
        if (order.getStatus() != OrderStatus.SERVING) {
            throw new RuntimeException("Chỉ order đang phục vụ mới được thanh toán");
        }

        // =====================================================================
        // B3: TÍNH TOÁN SỐ TIỀN CẦN THANH TOÁN (DÙNG HÀM CHUNG)
        // =====================================================================

        CalcPaymentResponse calc = calculateAmountForOrder(order, req.getVoucherCode());

        BigDecimal expectedAmountWithVat = calc.getFinalAmount();

        if (expectedAmountWithVat == null) {
            expectedAmountWithVat = BigDecimal.ZERO;
        }

        // =====================================================================
        // TÍNH TIỀN KHÁCH TRẢ & TIỀN THỪA (snapshot sang Invoice + Payment)
        // =====================================================================

        // Số tiền khách đưa (FE gửi lên)
        BigDecimal customerPaid = req.getCustomerPaid();
        if (customerPaid == null) customerPaid = BigDecimal.ZERO;

        // Số tiền phải trả thực tế
        BigDecimal mustPay = expectedAmountWithVat != null ? expectedAmountWithVat : BigDecimal.ZERO;

        // Tiền thừa trả khách
        BigDecimal changeAmount = customerPaid.subtract(mustPay);
        if (changeAmount.compareTo(BigDecimal.ZERO) < 0) {
            // Không cho âm – FE validation đảm bảo khách phải trả ≥ finalAmount,
            // nhưng vẫn để chặn cho chắc.
            changeAmount = BigDecimal.ZERO;
        }

        // Anti-cheat: số tiền FE gửi phải khớp với số tiền BE tính
        if (req.getAmount() == null || req.getAmount().compareTo(expectedAmountWithVat) != 0) {
            throw new RuntimeException("Số tiền thanh toán không khớp với số tiền cần thanh toán");
        }

        // Lấy thông tin voucher + discount + loyalty từ calc
        BigDecimal discountAmount = calc.getTotalDiscount() != null ? calc.getTotalDiscount() : BigDecimal.ZERO;
        BigDecimal voucherDiscount = calc.getVoucherDiscount() != null ? calc.getVoucherDiscount() : BigDecimal.ZERO;
        BigDecimal defaultDiscount = calc.getDefaultDiscount() != null ? calc.getDefaultDiscount() : BigDecimal.ZERO;
        BigDecimal amountBeforeVat = calc.getAmountAfterDiscount() != null ? calc.getAmountAfterDiscount() : BigDecimal.ZERO;
        BigDecimal vatPercent = calc.getVatPercent() != null ? calc.getVatPercent() : BigDecimal.ZERO;
        BigDecimal vatAmount = calc.getVatAmount() != null ? calc.getVatAmount() : BigDecimal.ZERO;
        String appliedVoucherCode = calc.getAppliedVoucherCode();
        int loyaltyEarnedPoint = calc.getLoyaltyEarnedPoint() != null ? calc.getLoyaltyEarnedPoint() : 0;

        // B4: Lấy user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // B5: Lấy danh sách món order
        var orderItems = orderItemRepository.findByOrder_Id(order.getId());

        // =====================================================================
        // 🟢 B6: TẠO HÓA ĐƠN TRƯỚC (KHẮC PHỤC LỖI invoice_id = null)
        // =====================================================================
        Invoice invoice = invoiceService.createInvoiceFromOrder(
                order.getId(),
                req.getMethod(),
                appliedVoucherCode,                 // mã voucher thực tế
                calc.getOrderTotal(),               // originalTotal
                voucherDiscount,                    // voucherDiscount
                defaultDiscount,                    // defaultDiscount
                discountAmount,                     // tổng giảm
                amountBeforeVat,                    // amountBeforeVat
                vatPercent,                         // vatRate
                vatAmount,                          // vatAmount
                expectedAmountWithVat,              // finalAmount
                loyaltyEarnedPoint,                 // điểm loyalty
                customerPaid,                       // tiền khách trả
                changeAmount                        // tiền thừa
        );


        // =====================================================================
        // 🟢 B7: TẠO PAYMENT (gắn invoice ngay lập tức)
        // =====================================================================

        // B7.1: Lấy và validate số tiền khách trả
        if (customerPaid == null) {
            throw new RuntimeException("Số tiền khách trả không hợp lệ");
        }

        // Không cho thanh toán nếu khách trả < số tiền phải thanh toán
        if (customerPaid.compareTo(expectedAmountWithVat) < 0) {
            throw new RuntimeException("Số tiền khách trả không hợp lệ");
        }

        // B7.2: Tạo Payment
        Payment payment = Payment.builder()
                .order(order)
                .invoice(invoice)
                .amount(expectedAmountWithVat)        // số tiền phải thanh toán
                .customerPaid(customerPaid)           // số tiền khách trả
                .changeAmount(changeAmount)           // tiền thừa
                .method(req.getMethod())
                .note(req.getNote())
                .paidAt(LocalDateTime.now())
                .createdBy(user.getId())
                .build();

        paymentRepository.save(payment);

        // =====================================================================
        // B8: Nếu có dùng voucher → tăng số lần sử dụng (usedCount)
        // =====================================================================
        if (appliedVoucherCode != null) {
            voucherService.increaseUsedCount(appliedVoucherCode);
        }

        // =====================================================================
        // 🟢 B8: cập nhật trạng thái Order → PAID
        // =====================================================================
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // =====================================================================
        // MODULE 16 – GIẢI PHÓNG BÀN KHI THANH TOÁN ORDER
        // =====================================================================
        if (order.getStatus() == OrderStatus.PAID) {
            if (order.getTable() != null && order.getTable().getId() != null) {
                restaurantTableService.markTableAvailable(order.getTable().getId());
            }
        }

        // =====================================================================
        // GỬI THÔNG BÁO: Tạo thanh toán
        // =====================================================================
        CreateNotificationRequest re = new CreateNotificationRequest();
        re.setTitle("Tạo thanh toán");
        re.setType(NotificationType.PAYMENT);
        re.setMessage("Tạo thanh toán");
        re.setLink("");
        notificationService.createNotification(re);

        // ✅ Audit log tạo payment
        auditLogService.log(
                AuditAction.PAYMENT_CREATE,
                "payment",
                payment.getId(),
                null,
                payment
        );

        // =====================================================================
        // Trả về kết quả
        // =====================================================================
        return toResponse(payment, loyaltyEarnedPoint);
    }

    // =====================================================================
    // 2. LẤY PAYMENT THEO ID
    // =====================================================================

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment"));
        return toResponse(payment, null);
    }

    // =====================================================================
    // 3. FILTER PAYMENT THEO KHOẢNG NGÀY
    // =====================================================================

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPayments(LocalDateTime from, LocalDateTime to) {
        List<Payment> payments;
        // Nếu FE không truyền gì → trả về toàn bộ
        if (from == null && to == null) {
            payments = paymentRepository.findAll();
        }
        // Nếu chỉ có from → lấy từ from → NOW
        else if (from != null && to == null) {
            payments = paymentRepository.findByPaidAtBetween(from, LocalDateTime.now());
        }
        // Nếu chỉ có to → lấy từ đầu → to
        else if (from == null) {
            payments = paymentRepository.findByPaidAtBetween(LocalDateTime.MIN, to);
        }
        // Nếu có đủ from và to
        else {
            payments = paymentRepository.findByPaidAtBetween(from, to);
        }

        return payments.stream()
                .map(p -> toResponse(p, null))
                .toList();
    }

    // =====================================================================
    // 4. HÀM CHUYỂN ENTITY → DTO
    // =====================================================================

    private PaymentResponse toResponse(Payment p, Integer loyaltyEarnedPoint) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrder().getId())
                .invoiceId(p.getInvoice() != null ? p.getInvoice().getId() : null)
                .amount(p.getAmount())
                .customerPaid(p.getCustomerPaid())
                .changeAmount(p.getChangeAmount())
                .method(p.getMethod())
                .note(p.getNote())
                .paidAt(p.getPaidAt())
                .loyaltyEarnedPoint(loyaltyEarnedPoint)  // gắn giá trị loyaltyEarnedPoint đã tính
                .createdBy(p.getCreatedBy())
                .createdAt(p.getCreatedAt())
                .build();
    }

    // =====================================================================
    // 1B. HÀM TÍNH THỬ SỐ TIỀN THANH TOÁN (KHÔNG LƯU DB)
    // =====================================================================

    /**
     * Tính toán số tiền cần thanh toán cho 1 order (preview).
     * ------------------------------------------------------------
     * - Dùng cho API /api/payments/calc
     * - KHÔNG tạo Payment, KHÔNG tạo Invoice, KHÔNG cập nhật Order
     * - Chỉ dùng để FE hiển thị chi tiết:
     *      + Tổng tiền gốc
     *      + Giảm voucher
     *      + Giảm mặc định
     *      + VAT
     *      + Số tiền cuối cùng cần thanh toán
     */
    @Transactional(readOnly = true)
    public CalcPaymentResponse calcPayment(CalcPaymentRequest req) {
        // B1: Tìm order
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

        // B2: Kiểm tra trạng thái order giống createPayment để tránh tính cho order đã thanh toán
        if (order.getStatus() == OrderStatus.PAID) {
            throw new RuntimeException("Order này đã thanh toán trước đó, không thể tính lại.");
        }
        if (order.getStatus() != OrderStatus.SERVING) {
            throw new RuntimeException("Chỉ order đang SERVING mới được tính số tiền thanh toán.");
        }

        // B3: Gọi hàm dùng chung
        return calculateAmountForOrder(order, req.getVoucherCode());
    }

    // =====================================================================
    // HÀM DÙNG CHUNG: TÍNH TOÁN SỐ TIỀN THANH TOÁN CHO 1 ORDER
    // =====================================================================

    /**
     * Tính toàn bộ các giá trị tiền cho 1 order:
     *  - Tổng gốc (orderTotal)
     *  - Giảm voucher
     *  - Giảm mặc định
     *  - Tổng giảm
     *  - VAT %
     *  - VAT amount
     *  - Final amount
     *  - Mã voucher thực tế áp dụng
     *  - Điểm loyalty nhận được
     *
     * Hàm này KHÔNG ghi DB, chỉ tính toán và trả về CalcPaymentResponse.
     */
    private CalcPaymentResponse calculateAmountForOrder(Order order, String voucherCodeInput) {

        // Mặc định: không dùng voucher
        BigDecimal discountAmount = BigDecimal.ZERO;      // Tổng số tiền giảm (voucher + default discount)
        String appliedVoucherCode = null;                 // Mã voucher thực tế áp dụng (có thể null)
        BigDecimal expectedAmount;                        // Số tiền sau giảm, trước VAT

        // Tổng tiền gốc của order (chưa áp dụng bất kỳ giảm giá nào)
        BigDecimal orderTotal = order.getTotalPrice();
        if (orderTotal == null) {
            orderTotal = BigDecimal.ZERO;
        }

        // =======================
        // 1) TÍNH VOUCHER
        // =======================
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        String voucherCode = voucherCodeInput;

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            VoucherApplyRequest applyReq = new VoucherApplyRequest();
            applyReq.setOrderId(order.getId());
            applyReq.setVoucherCode(voucherCode.trim());

            VoucherApplyResponse applyRes = voucherService.applyVoucher(applyReq);

            voucherDiscount = applyRes.getDiscountAmount();
            if (voucherDiscount == null) {
                voucherDiscount = BigDecimal.ZERO;
            }

            discountAmount = voucherDiscount;
            expectedAmount = applyRes.getFinalAmount(); // sau voucher, chưa VAT
            appliedVoucherCode = applyRes.getVoucherCode();
        } else {
            expectedAmount = orderTotal;
        }

        // =======================
        // 2) DISCOUNT MẶC ĐỊNH
        // =======================

        BigDecimal defaultDiscountPercent = systemSettingService.getNumberSetting(
                "discount.default_percent",
                BigDecimal.ZERO
        );
        BigDecimal maxDiscountPercent = systemSettingService.getNumberSetting(
                "discount.max_percent",
                new BigDecimal("100")
        );
        boolean allowWithVoucher = systemSettingService.getBooleanSetting(
                "discount.allow_with_voucher",
                true
        );
        boolean useDefaultDiscount = systemSettingService.getBooleanSetting(
                "discount.use_default",
                true
        );
        if (!useDefaultDiscount) {
            defaultDiscountPercent = BigDecimal.ZERO;
        }

        // Chuẩn hóa %
        if (defaultDiscountPercent.compareTo(BigDecimal.ZERO) < 0) defaultDiscountPercent = BigDecimal.ZERO;
        if (defaultDiscountPercent.compareTo(new BigDecimal("100")) > 0) defaultDiscountPercent = new BigDecimal("100");
        if (maxDiscountPercent.compareTo(BigDecimal.ZERO) < 0) maxDiscountPercent = BigDecimal.ZERO;
        if (maxDiscountPercent.compareTo(new BigDecimal("100")) > 0) maxDiscountPercent = new BigDecimal("100");

        BigDecimal defaultDiscountAmount = BigDecimal.ZERO;
        boolean hasVoucher = (appliedVoucherCode != null);

        if (defaultDiscountPercent.compareTo(BigDecimal.ZERO) > 0) {
            if (!hasVoucher || allowWithVoucher) {
                BigDecimal baseForDefault = hasVoucher ? expectedAmount : orderTotal;

                BigDecimal percent = defaultDiscountPercent
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

                defaultDiscountAmount = baseForDefault
                        .multiply(percent)
                        .setScale(0, RoundingMode.HALF_UP);

                expectedAmount = baseForDefault.subtract(defaultDiscountAmount);
                if (expectedAmount.compareTo(BigDecimal.ZERO) < 0) {
                    expectedAmount = BigDecimal.ZERO;
                }

                discountAmount = discountAmount.add(defaultDiscountAmount);
            }
        }

        // Giới hạn giảm giá tối đa
        if (orderTotal.compareTo(BigDecimal.ZERO) > 0 && maxDiscountPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxDiscountAmount = orderTotal
                    .multiply(maxDiscountPercent)
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                    .setScale(0, RoundingMode.HALF_UP);

            if (discountAmount.compareTo(maxDiscountAmount) > 0) {
                discountAmount = maxDiscountAmount;
                expectedAmount = orderTotal.subtract(discountAmount);
                if (expectedAmount.compareTo(BigDecimal.ZERO) < 0) {
                    expectedAmount = BigDecimal.ZERO;
                }
            }
        }

        // =======================
        // 3) VAT
        // =======================

        BigDecimal amountBeforeVat = expectedAmount != null ? expectedAmount : BigDecimal.ZERO;

        BigDecimal vatPercent = systemSettingService.getNumberSetting(
                "vat.rate",
                BigDecimal.ZERO
        );
        if (vatPercent.compareTo(BigDecimal.ZERO) < 0) vatPercent = BigDecimal.ZERO;
        if (vatPercent.compareTo(new BigDecimal("100")) > 0) vatPercent = new BigDecimal("100");

        BigDecimal vatAmount = BigDecimal.ZERO;

        if (vatPercent.compareTo(BigDecimal.ZERO) > 0 && amountBeforeVat.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal vatDecimal = vatPercent
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

            vatAmount = amountBeforeVat
                    .multiply(vatDecimal)
                    .setScale(0, RoundingMode.HALF_UP);
        }

        BigDecimal finalAmount = amountBeforeVat.add(vatAmount);

        // =======================
        // 4) LOYALTY
        // =======================

        boolean loyaltyEnabled = systemSettingService.getBooleanSetting(
                "loyalty.enabled",
                false
        );

        int loyaltyEarnedPoint = 0;

        if (loyaltyEnabled) {
            BigDecimal earnRate = systemSettingService.getNumberSetting(
                    "loyalty.earn_rate",
                    BigDecimal.ZERO
            );

            BigDecimal thousand = new BigDecimal("1000");

            BigDecimal point = finalAmount
                    .divide(thousand, 4, RoundingMode.DOWN)
                    .multiply(earnRate);

            loyaltyEarnedPoint = point.setScale(0, RoundingMode.DOWN).intValue();
        }

        // Build response
        return CalcPaymentResponse.builder()
                .orderTotal(orderTotal)
                .voucherDiscount(voucherDiscount)
                .defaultDiscount(defaultDiscountAmount)
                .totalDiscount(discountAmount)
                .amountAfterDiscount(amountBeforeVat)
                .vatPercent(vatPercent)
                .vatAmount(vatAmount)
                .finalAmount(finalAmount)
                .appliedVoucherCode(appliedVoucherCode)
                .loyaltyEarnedPoint(loyaltyEarnedPoint)
                .build();
    }
}
