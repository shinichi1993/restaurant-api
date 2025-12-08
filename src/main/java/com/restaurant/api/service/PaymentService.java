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
        // B3: Tính lại số tiền cần thanh toán (có xét đến voucher + discount mặc định)
        // =====================================================================

        // Mặc định: không dùng voucher
        BigDecimal discountAmount = BigDecimal.ZERO;      // Tổng số tiền giảm (voucher + default discount)
        String appliedVoucherCode = null;                 // Mã voucher thực tế áp dụng (có thể null)
        BigDecimal expectedAmount;                        // Số tiền cuối cùng cần thanh toán

        // Tổng tiền gốc của order (chưa áp dụng bất kỳ giảm giá nào)
        BigDecimal orderTotal = order.getTotalPrice();
        if (orderTotal == null) {
            orderTotal = BigDecimal.ZERO;
        }

        String voucherCode = req.getVoucherCode();

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            // Nếu FE gửi voucherCode → gọi lại VoucherService để tính toán chính xác
            VoucherApplyRequest applyReq = new VoucherApplyRequest();
            applyReq.setOrderId(order.getId());
            applyReq.setVoucherCode(voucherCode.trim());

            // Hàm này sẽ:
            //  - Kiểm tra hiệu lực voucher
            //  - Kiểm tra minOrderAmount, usageLimit
            //  - Tính discountAmount & finalAmount (sau khi trừ voucher,  CHƯA VAT)
            VoucherApplyResponse applyRes = voucherService.applyVoucher(applyReq);

            BigDecimal voucherDiscount = applyRes.getDiscountAmount();
            if (voucherDiscount == null) {
                voucherDiscount = BigDecimal.ZERO;
            }

            discountAmount = voucherDiscount;
            expectedAmount = applyRes.getFinalAmount(); // số tiền sau khi áp dụng voucher
            appliedVoucherCode = applyRes.getVoucherCode();
        } else {
            // Không dùng voucher → số tiền cần thanh toán trước khi áp dụng discount mặc định
            expectedAmount = orderTotal;
        }

        // -----------------------------------------------------------------
        // 🚩 TÍCH HỢP DISCOUNT TỪ SYSTEM SETTING (Module 20)
        // -----------------------------------------------------------------
        // Các cấu hình sử dụng:
        //  - discount.default_percent      → % giảm mặc định
        //  - discount.max_percent          → % giảm tối đa cho 1 hóa đơn
        //  - discount.allow_with_voucher   → có cho phép giảm thêm khi đã dùng voucher hay không
        // -----------------------------------------------------------------

        // Đọc cấu hình từ SystemSetting
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

        // ✅ Cấu hình BẬT/TẮT giảm giá mặc định
        // - discount.use_default = true  → dùng defaultDiscountPercent như bình thường
        // - discount.use_default = false → ép defaultDiscountPercent = 0 (coi như không giảm)
        boolean useDefaultDiscount = systemSettingService.getBooleanSetting(
                "discount.use_default",
                true // mặc định = true để giữ hành vi cũ nếu chưa cấu hình
        );
        if (!useDefaultDiscount) {
            // Nếu tắt giảm giá mặc định → ép % về 0
            defaultDiscountPercent = BigDecimal.ZERO;
        }

        // Chuẩn hóa %: không âm, không vượt quá 100
        if (defaultDiscountPercent.compareTo(BigDecimal.ZERO) < 0) {
            defaultDiscountPercent = BigDecimal.ZERO;
        }
        if (defaultDiscountPercent.compareTo(new BigDecimal("100")) > 0) {
            defaultDiscountPercent = new BigDecimal("100");
        }
        if (maxDiscountPercent.compareTo(BigDecimal.ZERO) < 0) {
            maxDiscountPercent = BigDecimal.ZERO;
        }
        if (maxDiscountPercent.compareTo(new BigDecimal("100")) > 0) {
            maxDiscountPercent = new BigDecimal("100");
        }

        // Tính giảm giá mặc định (nếu > 0)
        BigDecimal defaultDiscountAmount = BigDecimal.ZERO;
        boolean hasVoucher = (appliedVoucherCode != null);

        if (defaultDiscountPercent.compareTo(BigDecimal.ZERO) > 0) {
            // Nếu đã có voucher và không cho phép dùng kèm → bỏ qua default discount
            if (!hasVoucher || allowWithVoucher) {
                // Cơ sở tính giảm giá:
                //  - Nếu đã có voucher → giảm trên số tiền còn lại sau voucher (expectedAmount)
                //  - Nếu không có voucher → giảm trên tổng tiền order
                BigDecimal baseForDefault = hasVoucher ? expectedAmount : orderTotal;

                BigDecimal percent = defaultDiscountPercent
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

                defaultDiscountAmount = baseForDefault
                        .multiply(percent)
                        .setScale(0, RoundingMode.HALF_UP); // làm tròn về tiền VND

                // Cập nhật expectedAmount sau khi trừ discount mặc định
                expectedAmount = baseForDefault.subtract(defaultDiscountAmount);
                if (expectedAmount.compareTo(BigDecimal.ZERO) < 0) {
                    expectedAmount = BigDecimal.ZERO;
                }

                // Tổng discount = discount voucher + discount mặc định
                discountAmount = discountAmount.add(defaultDiscountAmount);
            }
        }

        // Áp dụng giới hạn giảm giá tối đa (max_percent) trên tổng tiền order
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

        // -----------------------------------------------------------------
        // 🚩 B3.1 – TÍNH VAT DỰA TRÊN CẤU HÌNH HỆ THỐNG (Module 20)
        // -----------------------------------------------------------------

        // expectedAmount hiện tại là: (tổng tiền - voucher - default discount)
        // Ta sẽ tính VAT trên số tiền này
        BigDecimal amountBeforeVat = expectedAmount;

        if (amountBeforeVat == null) {
            amountBeforeVat = BigDecimal.ZERO;
        }

        // Đọc VAT từ system setting (vd: 10 = 10%)
        BigDecimal vatPercent = systemSettingService.getNumberSetting(
                "vat.rate",
                BigDecimal.ZERO
        );

        // Chuẩn hóa về [0, 100]
        if (vatPercent.compareTo(BigDecimal.ZERO) < 0) vatPercent = BigDecimal.ZERO;
        if (vatPercent.compareTo(new BigDecimal("100")) > 0) vatPercent = new BigDecimal("100");

        BigDecimal vatAmount = BigDecimal.ZERO;

        if (vatPercent.compareTo(BigDecimal.ZERO) > 0 && amountBeforeVat.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal vatDecimal = vatPercent
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

            vatAmount = amountBeforeVat
                    .multiply(vatDecimal)
                    .setScale(0, RoundingMode.HALF_UP); // làm tròn tiền Việt
        }

        // Cập nhật lại expectedAmount = amountBeforeVat + vatAmount
        BigDecimal expectedAmountWithVat = amountBeforeVat.add(vatAmount);

        // ===============================
        // B3.2 – CHECK SỐ TIỀN FE GỬI LÊN
        // ===============================
        if (req.getAmount() == null || req.getAmount().compareTo(expectedAmountWithVat) != 0) {
            throw new RuntimeException("Số tiền thanh toán không khớp với số tiền cần thanh toán");
        }

        // --------------------------------------------------------------
        // 🎯 B3.3 – TÍNH ĐIỂM LOYALTY (nếu bật trong SystemSetting)
        // --------------------------------------------------------------
        boolean loyaltyEnabled = systemSettingService.getBooleanSetting(
                "loyalty.enabled",
                false
        );

        int loyaltyEarnedPoint = 0;

        if (loyaltyEnabled) {
            // Tỉ lệ tích điểm: số điểm trên mỗi 1.000đ
            BigDecimal earnRate = systemSettingService.getNumberSetting(
                    "loyalty.earn_rate",
                    BigDecimal.ZERO
            );

            BigDecimal thousand = new BigDecimal("1000");

            // Công thức: (số tiền cuối cùng phải trả / 1000) * earn_rate
            BigDecimal point = expectedAmountWithVat
                    .divide(thousand, 4, RoundingMode.DOWN)
                    .multiply(earnRate);

            loyaltyEarnedPoint = point.setScale(0, RoundingMode.DOWN).intValue();
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
                appliedVoucherCode,   // có thể null nếu không dùng voucher
                discountAmount,       // có thể 0 nếu không dùng voucher
                loyaltyEarnedPoint    // ⭐ điểm tích lũy đã tính
        );

        // =====================================================================
        // 🟢 B7: TẠO PAYMENT (gắn invoice ngay lập tức)
        // =====================================================================
        Payment payment = Payment.builder()
                .order(order)
                .invoice(invoice)          // 🟢 KHÔNG ĐƯỢC ĐỂ SAU
                .amount(req.getAmount())
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
            throw new RuntimeException("Chỉ order đang phục vụ mới được tính số tiền thanh toán.");
        }

        // =====================================================================
        // B3: Tính lại số tiền cần thanh toán (có xét đến voucher + discount mặc định)
        // =====================================================================

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
        // B3.1: TÍNH VOUCHER
        // =======================
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        String voucherCode = req.getVoucherCode();

        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            // Nếu FE gửi voucherCode → gọi lại VoucherService để tính toán chính xác
            VoucherApplyRequest applyReq = new VoucherApplyRequest();
            applyReq.setOrderId(order.getId());
            applyReq.setVoucherCode(voucherCode.trim());

            // Hàm này sẽ:
            //  - Kiểm tra hiệu lực voucher
            //  - Kiểm tra minOrderAmount, usageLimit
            //  - Tính discountAmount & finalAmount (sau khi trừ voucher,  CHƯA VAT)
            VoucherApplyResponse applyRes = voucherService.applyVoucher(applyReq);

            voucherDiscount = applyRes.getDiscountAmount();
            if (voucherDiscount == null) {
                voucherDiscount = BigDecimal.ZERO;
            }

            discountAmount = voucherDiscount;
            expectedAmount = applyRes.getFinalAmount(); // số tiền sau khi áp dụng voucher
            appliedVoucherCode = applyRes.getVoucherCode();
        } else {
            // Không dùng voucher → số tiền cần thanh toán trước khi áp dụng discount mặc định
            expectedAmount = orderTotal;
        }

        // -----------------------------------------------------------------
        // B3.2: TÍCH HỢP DISCOUNT TỪ SYSTEM SETTING (Module 20)
        // -----------------------------------------------------------------
        // Các cấu hình sử dụng:
        //  - discount.default_percent      → % giảm mặc định
        //  - discount.max_percent          → % giảm tối đa cho 1 hóa đơn
        //  - discount.allow_with_voucher   → có cho phép giảm thêm khi đã dùng voucher hay không
        // -----------------------------------------------------------------

        // Đọc cấu hình từ SystemSetting
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

        // ✅ BẬT/TẮT giảm giá mặc định cho phần tính thử
        boolean useDefaultDiscount = systemSettingService.getBooleanSetting(
                "discount.use_default",
                true
        );
        if (!useDefaultDiscount) {
            defaultDiscountPercent = BigDecimal.ZERO;
        }

        // Chuẩn hóa %: không âm, không vượt quá 100
        if (defaultDiscountPercent.compareTo(BigDecimal.ZERO) < 0) {
            defaultDiscountPercent = BigDecimal.ZERO;
        }
        if (defaultDiscountPercent.compareTo(new BigDecimal("100")) > 0) {
            defaultDiscountPercent = new BigDecimal("100");
        }
        if (maxDiscountPercent.compareTo(BigDecimal.ZERO) < 0) {
            maxDiscountPercent = BigDecimal.ZERO;
        }
        if (maxDiscountPercent.compareTo(new BigDecimal("100")) > 0) {
            maxDiscountPercent = new BigDecimal("100");
        }

        // Tính giảm giá mặc định (nếu > 0)
        BigDecimal defaultDiscountAmount = BigDecimal.ZERO;
        boolean hasVoucher = (appliedVoucherCode != null);

        if (defaultDiscountPercent.compareTo(BigDecimal.ZERO) > 0) {
            // Nếu đã có voucher và không cho phép dùng kèm → bỏ qua default discount
            if (!hasVoucher || allowWithVoucher) {
                // Cơ sở tính giảm giá:
                //  - Nếu đã có voucher → giảm trên số tiền còn lại sau voucher (expectedAmount)
                //  - Nếu không có voucher → giảm trên tổng tiền order
                BigDecimal baseForDefault = hasVoucher ? expectedAmount : orderTotal;

                BigDecimal percent = defaultDiscountPercent
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

                defaultDiscountAmount = baseForDefault
                        .multiply(percent)
                        .setScale(0, RoundingMode.HALF_UP); // làm tròn về tiền VND

                // Cập nhật expectedAmount sau khi trừ discount mặc định
                expectedAmount = baseForDefault.subtract(defaultDiscountAmount);
                if (expectedAmount.compareTo(BigDecimal.ZERO) < 0) {
                    expectedAmount = BigDecimal.ZERO;
                }

                // Tổng discount = discount voucher + discount mặc định
                discountAmount = discountAmount.add(defaultDiscountAmount);
            }
        }

        // Áp dụng giới hạn giảm giá tối đa (max_percent) trên tổng tiền order
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

        // =====================================================================
        // B4: TÍNH VAT DỰA TRÊN CẤU HÌNH HỆ THỐNG (Module 20)
        // =====================================================================

        // expectedAmount hiện tại là: (tổng tiền - voucher - default discount)
        // Ta sẽ tính VAT trên số tiền này
        BigDecimal amountBeforeVat = expectedAmount;

        if (amountBeforeVat == null) {
            amountBeforeVat = BigDecimal.ZERO;
        }

        // Đọc VAT từ system setting (vd: 10 = 10%)
        BigDecimal vatPercent = systemSettingService.getNumberSetting(
                "vat.rate",
                BigDecimal.ZERO
        );

        // Chuẩn hóa về [0, 100]
        if (vatPercent.compareTo(BigDecimal.ZERO) < 0) vatPercent = BigDecimal.ZERO;
        if (vatPercent.compareTo(new BigDecimal("100")) > 0) vatPercent = new BigDecimal("100");

        BigDecimal vatAmount = BigDecimal.ZERO;

        if (vatPercent.compareTo(BigDecimal.ZERO) > 0 && amountBeforeVat.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal vatDecimal = vatPercent
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

            vatAmount = amountBeforeVat
                    .multiply(vatDecimal)
                    .setScale(0, RoundingMode.HALF_UP); // làm tròn tiền Việt
        }

        // Số tiền cuối cùng cần thanh toán
        BigDecimal finalAmount = amountBeforeVat.add(vatAmount);

        // --------------------------------------------------------------
        // 🎯 TÍNH ĐIỂM LOYALTY (Step 5 – chỉ tính, chưa lưu DB)
        // --------------------------------------------------------------

        // Đọc cấu hình: loyalty có bật không?
        boolean loyaltyEnabled = systemSettingService.getBooleanSetting(
                "loyalty.enabled",
                false // mặc định KHÔNG bật
        );

        // Nếu tắt → điểm nhận được = 0
        int loyaltyEarnedPoint = 0;

        if (loyaltyEnabled) {

            // Tỉ lệ earn_rate: số điểm cho mỗi 1000đ
            BigDecimal earnRate = systemSettingService.getNumberSetting(
                    "loyalty.earn_rate",
                    BigDecimal.ZERO
            );

            // Công thức: finalAmount / 1000 * earn_rate
            BigDecimal thousand = new BigDecimal("1000");
            BigDecimal point = finalAmount
                    .divide(thousand, 4, RoundingMode.DOWN)
                    .multiply(earnRate);

            loyaltyEarnedPoint = point.setScale(0, RoundingMode.DOWN).intValue();
        }

        // =====================================================================
        // B5: Build response cho FE
        // =====================================================================

        return CalcPaymentResponse.builder()
                .orderTotal(orderTotal)
                .voucherDiscount(voucherDiscount)
                .defaultDiscount(defaultDiscountAmount)
                .totalDiscount(discountAmount)
                .amountAfterDiscount(amountBeforeVat)
                .vatPercent(vatPercent)
                .vatAmount(vatAmount)
                .finalAmount(finalAmount)
                // ⭐ TRẢ VỀ MÃ VOUCHER
                .appliedVoucherCode(appliedVoucherCode)
                // ⭐ TRẢ VỀ ĐIỂM LOYALTY
                .loyaltyEarnedPoint(loyaltyEarnedPoint)
                .build();
    }
}
