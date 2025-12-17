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
    // ✅ Phase 4.3 – Rule Engine thông báo
    private final NotificationRuleService notificationRuleService;
    private final AuditLogService auditLogService;
    private final RestaurantTableService restaurantTableService;
    private final VoucherService voucherService;
    private final SystemSettingService systemSettingService;
    private final MemberService memberService;

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
        // B2.1: GÁN HỘI VIÊN CHO ORDER (NẾU FE CHỌN)
        // ---------------------------------------------------------------------
        // - FE có thể chọn hội viên ngay tại PaymentModal
        // - Order là source of truth → cần lưu memberId vào order
        // - Chỉ gán khi order chưa có member
        // =====================================================================
        if (req.getMemberId() != null) {
            order.setMemberId(req.getMemberId());
            orderRepository.save(order);
        }

        // =====================================================================
        // B3: TÍNH TOÁN SỐ TIỀN CẦN THANH TOÁN (DÙNG HÀM CHUNG, tính cả redeem nếu có)
        // =====================================================================
        CalcPaymentResponse calc = calculateAmountForOrder(
                order,
                req.getVoucherCode(),
                req.getMemberId(),
                req.getRedeemPoint()
        );

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

        // ======================================================
        // B4.0: REDEEM ANTI-CHEAT
        // ------------------------------------------------------
        // - redeemPoint lấy từ request (FE gửi lên)
        // - redeemDiscount lấy từ calc (do chính BE vừa tính)
        // - BE tính lại 1 lần nữa để đảm bảo dữ liệu không bị sửa
        // ======================================================

        Integer redeemPointReq = (req.getRedeemPoint() != null ? req.getRedeemPoint() : 0);

        BigDecimal redeemDiscountFromCalc =
                (calc.getRedeemDiscount() != null ? calc.getRedeemDiscount() : BigDecimal.ZERO);

        if (order.getMemberId() != null && redeemPointReq > 0) {

            RedeemResult expectedRedeemResult = calculateRedeemResult(
                    order.getMemberId(),
                    redeemPointReq,
                    calc.getAmountBeforeRedeem()
            );

            // So sánh TIỀN
            if (expectedRedeemResult.getDiscountAmount()
                    .compareTo(redeemDiscountFromCalc) != 0) {
                throw new RuntimeException("Dữ liệu redeem point không hợp lệ (discount)");
            }

            // So sánh ĐIỂM
            if (expectedRedeemResult.getUsedPoint()
                    != calc.getRedeemedPoint()) {
                throw new RuntimeException("Dữ liệu redeem point không hợp lệ (point)");
            }
        }

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

        // ======================================================
        // B7.2: TRỪ ĐIỂM HỘI VIÊN (REDEEM THẬT)
        // ------------------------------------------------------
        // - Chỉ trừ nếu có memberId và redeemPointReq > 0
        // - Lưu lịch sử vào member_point_history (trong MemberService)
        // ======================================================
        // ======================================================
        // TRỪ ĐIỂM THEO SỐ ĐIỂM THỰC TẾ ĐƯỢC SỬ DỤNG
        // - KHÔNG trừ theo số FE nhập
        // ======================================================
        int redeemedPointFinal =
                (calc.getRedeemedPoint() != null ? calc.getRedeemedPoint() : 0);

        if (order.getMemberId() != null && redeemedPointFinal > 0) {
            memberService.redeemPoint(order.getMemberId(), redeemedPointFinal, order.getId());
        }


        // =====================================================================
        // B8: Nếu có dùng voucher → tăng số lần sử dụng (usedCount)
        // =====================================================================
        if (appliedVoucherCode != null) {
            voucherService.increaseUsedCount(appliedVoucherCode);
        }

        // =====================================================================
        // B9: CẬP NHẬT ĐIỂM LOYALTY CHO HỘI VIÊN (NẾU CÓ)
        // =====================================================================
        // Điều kiện:
        //  - Order có memberId (đã gán hội viên)
        //  - loyaltyEarnedPoint > 0 (loyalty đang bật + có điểm để cộng)
        if (order.getMemberId() != null && loyaltyEarnedPoint > 0) {
            try {
                memberService.earnPoint(order.getMemberId(), loyaltyEarnedPoint, order.getId());
            } catch (Exception ex) {
                // Không để lỗi Loyalty làm hỏng luồng thanh toán chính
                // → ghi log sau, hiện tại chỉ ném RuntimeException tuỳ thiết kế
                throw new RuntimeException("Lỗi khi cộng điểm cho hội viên: " + ex.getMessage());
            }
        }

        // =====================================================================
        // 🟢 B10: cập nhật trạng thái Order → PAID
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

        // ============================================================
        // Phase 4.3 – Rule Engine: thanh toán thành công
        // ============================================================
        notificationRuleService.onPaymentSuccess(payment);

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

        // B3: Gọi hàm dùng chung (có hỗ trợ memberId + redeemPoint)
        return calculateAmountForOrder(
                order,
                req.getVoucherCode(),
                req.getMemberId(),
                req.getRedeemPoint()
        );
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
     *@param order             Order cần tính
     *@param voucherCodeInput  Mã voucher FE nhập (có thể null/empty)
     *@param memberIdInput     MemberId FE chọn (có thể null)
     *@param redeemPointInput  RedeemPoint FE nhập (có thể null)
     */
    private CalcPaymentResponse calculateAmountForOrder(Order order,
                                                        String voucherCodeInput,
                                                        Long memberIdInput,
                                                        Integer redeemPointInput) {

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

        // ======================================================
        // ✅ BASE TÍNH VAT (SAU voucher + default, CHƯA redeem)
        // ------------------------------------------------------
        // Quy ước nghiệp vụ:
        // - Redeem KHÔNG được trừ vào VAT
        // - VAT luôn tính trên giá trước redeem
        // ======================================================
        BigDecimal amountBeforeVatBase = expectedAmount;

        // ======================================================
        // XÁC ĐỊNH HỘI VIÊN & SỐ ĐIỂM DÙNG (SOURCE OF TRUTH)
        // ------------------------------------------------------
        // - Ưu tiên memberId FE truyền
        // - Nếu FE không truyền thì lấy từ order
        // - redeemPoint chỉ hợp lệ khi > 0
        // ======================================================
        Long memberIdToUse =
                (memberIdInput != null)
                        ? memberIdInput
                        : order.getMemberId();

        int redeemPointToUse =
                (redeemPointInput != null && redeemPointInput > 0)
                        ? redeemPointInput
                        : 0;

        // =======================
        // 2.5) REDEEM POINT (DÙNG ĐIỂM)
        // ------------------------------------------------------------
        // Quy tắc CHUẨN:
        // - Redeem CHỈ ảnh hưởng số tiền KHÁCH PHẢI TRẢ
        // - KHÔNG ảnh hưởng base tính VAT
        // ============================================================

        BigDecimal redeemDiscount = BigDecimal.ZERO;
        BigDecimal amountBeforeRedeem = expectedAmount;
        BigDecimal amountAfterRedeem = expectedAmount;
        int redeemedPointFinal = 0;

        if (memberIdToUse != null && redeemPointToUse > 0) {
            RedeemResult redeemResult = calculateRedeemResult(
                    memberIdToUse,
                    redeemPointToUse,
                    expectedAmount
            );

            redeemDiscount = redeemResult.getDiscountAmount();
            redeemedPointFinal = redeemResult.getUsedPoint();

            amountAfterRedeem = expectedAmount.subtract(redeemDiscount);
            if (amountAfterRedeem.compareTo(BigDecimal.ZERO) < 0) {
                amountAfterRedeem = BigDecimal.ZERO;
            }
        }

        // ======================================================
        // CỘNG REDEEM VÀO TỔNG GIẢM (voucher + default + redeem)
        // ======================================================
        discountAmount = discountAmount.add(redeemDiscount);

        // Sau khi trừ hết discount (voucher + default + redeem)
        BigDecimal amountBeforeVat = amountBeforeVatBase;

        // =======================
        // 3) VAT
        // VAT tính trên base TRƯỚC redeem
        // =======================

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

        // Tổng phải thanh toán = (sau redeem) + VAT
        BigDecimal finalAmount = amountAfterRedeem.add(vatAmount);

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
                // ✅ REDEEM
                .redeemDiscount(redeemDiscount)
                .redeemedPoint(redeemedPointFinal)
                .amountBeforeRedeem(amountBeforeRedeem)
                .totalDiscount(discountAmount)
                .amountAfterDiscount(amountAfterRedeem)
                .vatPercent(vatPercent)
                .vatAmount(vatAmount)
                .finalAmount(finalAmount)
                .appliedVoucherCode(appliedVoucherCode)
                .loyaltyEarnedPoint(loyaltyEarnedPoint)
                .build();
    }

    /**
     * Tính kết quả giảm giá khi dùng điểm hội viên (REDEEM).
     * --------------------------------------------------------
     * Trả về:
     *  - discountAmount: số tiền giảm thực tế (đã bị giới hạn theo max_percent và amountBefore)
     *  - usedPoint: số điểm thực tế bị trừ (tương ứng với discountAmount)
     *
     * Quy tắc:
     *  - Điểm thực dùng KHÔNG được vượt quá điểm request
     *  - Nếu bị cap tiền giảm thì điểm thực dùng cũng phải giảm theo
     */
    private RedeemResult calculateRedeemResult(
            Long memberId,
            Integer redeemPointReq,
            BigDecimal amountBefore
    ) {
        // Không có hội viên hoặc không dùng điểm
        if (memberId == null || redeemPointReq == null || redeemPointReq <= 0) {
            return new RedeemResult(BigDecimal.ZERO, 0);
        }

        // Kiểm tra bật/tắt loyalty
        boolean loyaltyEnabled = systemSettingService.getBooleanSetting("loyalty.enabled", false);
        boolean redeemEnabled = systemSettingService.getBooleanSetting("loyalty.redeem.enabled", false);
        if (!loyaltyEnabled || !redeemEnabled) {
            return new RedeemResult(BigDecimal.ZERO, 0);
        }

        // Lấy thông tin hội viên
        Member member = memberService.getEntityById(memberId);

        // Không đủ điểm
        if (member.getTotalPoint() < redeemPointReq) {
            throw new RuntimeException("Số điểm hội viên không đủ để sử dụng");
        }

        // 1 điểm = redeemRate (vd 1000đ)
        BigDecimal redeemRate = systemSettingService.getNumberSetting(
                "loyalty.redeem.rate",
                new BigDecimal("1000")
        );

        // Tiền giảm theo điểm request
        BigDecimal requestedAmount = redeemRate.multiply(new BigDecimal(redeemPointReq));

        // Giới hạn % tối đa được redeem
        BigDecimal maxPercent = systemSettingService.getNumberSetting(
                "loyalty.redeem.max_percent",
                new BigDecimal("50")
        );

        BigDecimal maxRedeemAmount = amountBefore
                .multiply(maxPercent)
                .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);

        // Cap tiền giảm thực tế
        BigDecimal finalRedeemAmount = requestedAmount;
        if (finalRedeemAmount.compareTo(maxRedeemAmount) > 0) {
            finalRedeemAmount = maxRedeemAmount;
        }
        if (finalRedeemAmount.compareTo(amountBefore) > 0) {
            finalRedeemAmount = amountBefore;
        }
        if (finalRedeemAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalRedeemAmount = BigDecimal.ZERO;
        }

        // Tính số điểm thực dùng theo tiền giảm thực tế
        // dùng FLOOR để không vượt quá tiền giảm (tránh lẻ)
        int usedPoint = 0;
        if (redeemRate.compareTo(BigDecimal.ZERO) > 0) {
            usedPoint = finalRedeemAmount
                    .divide(redeemRate, 0, RoundingMode.DOWN)
                    .intValue();
        }

        // Chốt: không cho vượt quá điểm request
        if (usedPoint > redeemPointReq) {
            usedPoint = redeemPointReq;
        }

        return new RedeemResult(finalRedeemAmount, usedPoint);
    }

    // =====================================================================
    // DTO nội bộ: Kết quả redeem (tiền giảm + điểm thực dùng)
    // =====================================================================
    private static class RedeemResult {
        private final BigDecimal discountAmount;
        private final int usedPoint;

        private RedeemResult(BigDecimal discountAmount, int usedPoint) {
            this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
            this.usedPoint = Math.max(usedPoint, 0);
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public int getUsedPoint() {
            return usedPoint;
        }
    }

}
