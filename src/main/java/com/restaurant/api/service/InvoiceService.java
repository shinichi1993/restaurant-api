package com.restaurant.api.service;

import com.restaurant.api.dto.invoice.InvoiceItemResponse;
import com.restaurant.api.dto.invoice.InvoiceResponse;
import com.restaurant.api.entity.*;
import com.restaurant.api.enums.AuditAction;
import com.restaurant.api.enums.OrderStatus;
import com.restaurant.api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.restaurant.api.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.math.RoundingMode;

/**
 * InvoiceService
 * =====================================================================
 * Xử lý toàn bộ nghiệp vụ liên quan đến HÓA ĐƠN (Invoice).
 *
 * Chức năng chính:
 *  - Tạo hóa đơn từ Order (tự động khi payment)
 *  - Lấy hóa đơn theo orderId
 *  - Lấy chi tiết hóa đơn theo invoiceId
 *  - Convert Entity → DTO trả về FE
 *
 * Quy ước:
 *  - Mỗi Order chỉ có 1 Invoice duy nhất
 *  - Invoice được tạo khi Order chuyển sang trạng thái PAID (Module 10)
 * =====================================================================
 * Tất cả comment theo Rule 13 — tiếng Việt đầy đủ.
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DishRepository dishRepository;
    private final AuditLogService auditLogService;
    // Service đọc cấu hình hệ thống (Module 20 - System Settings)
    private final SystemSettingService systemSettingService;

    // =====================================================================
    // 1. TẠO HÓA ĐƠN TỪ ORDER (được gọi từ PaymentService / PaymentController)
    // =====================================================================
    /**
     * Tạo hóa đơn cho Order, được gọi từ PaymentService.
     * ----------------------------------------------------------
     * Bước xử lý:
     *  1. Validate Order tồn tại + trạng thái hợp lệ
     *  2. Load OrderItem + Dish để tính tiền
     *  3. Lưu Invoice + InvoiceItem
     *  4. Trả về chính entity Invoice để Payment gắn quan hệ
     *
     * - orderId: ID order
     * - method: phương thức thanh toán
     * - voucherCode: mã voucher áp dụng (có thể null)
     * - discountAmount: tổng số tiền giảm (voucher + default)
     * - loyaltyEarnedPoint: số điểm tích lũy khách nhận được
     * @return Invoice đã lưu trong DB
     */
    @Transactional
    public Invoice createInvoiceFromOrder(Long orderId, PaymentMethod paymentMethod, String voucherCode,
                                          BigDecimal discountAmount, Integer loyaltyEarnedPoint) {

        // 1. Kiểm tra order tồn tại
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng"));

        // 2. Chỉ cho phép tạo invoice nếu order đang ở trạng thái SERVING
        if (order.getStatus() != OrderStatus.SERVING) {
            throw new RuntimeException("Chỉ có thể tạo hóa đơn khi đơn hàng đang phục vụ (SERVING)");
        }

        // 3. Load danh sách order item
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            throw new RuntimeException("Đơn hàng không có món ăn");
        }

        // 4. Lấy thông tin dish để map nhanh (id -> dish)
        Set<Long> dishIds = items.stream().map(OrderItem::getDishId).collect(Collectors.toSet());
        Map<Long, Dish> dishMap = dishRepository.findAllById(dishIds)
                .stream()
                .collect(Collectors.toMap(Dish::getId, d -> d));
        // 5. Tính tổng tiền
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem oi : items) {
            Dish d = dishMap.get(oi.getDishId());
            if (d == null) continue;

            BigDecimal price = d.getPrice();
            BigDecimal qty = BigDecimal.valueOf(oi.getQuantity());
            total = total.add(price.multiply(qty));
        }

        // Sau khi đã tính xong tổng tiền ban đầu "total"
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }

        // Đảm bảo discount không âm và không vượt quá total
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }
        if (discountAmount.compareTo(total) > 0) {
            discountAmount = total;
        }

        // --------------------------------------------------------------------
        // B5: TÍNH VAT DỰA TRÊN CẤU HÌNH HỆ THỐNG (Module 20)
        // --------------------------------------------------------------------
        // total          = tổng tiền trước khi giảm
        // discountAmount = tổng số tiền giảm (voucher, giảm giá khác...)
        // baseAmount     = tổng tiền sau giảm, trước VAT
        BigDecimal baseAmount = total.subtract(discountAmount);
        if (baseAmount.compareTo(BigDecimal.ZERO) < 0) {
            baseAmount = BigDecimal.ZERO;
        }

        // Đọc % VAT từ SystemSetting (ví dụ: 10 = 10%)
        BigDecimal vatPercent = systemSettingService.getNumberSetting(
                "vat.rate",
                BigDecimal.ZERO // mặc định 0% nếu chưa cấu hình
        );

        // Chuẩn hóa VAT trong khoảng [0, 100]
        if (vatPercent.compareTo(BigDecimal.ZERO) < 0) {
            vatPercent = BigDecimal.ZERO;
        }
        if (vatPercent.compareTo(new BigDecimal("100")) > 0) {
            vatPercent = new BigDecimal("100");
        }

        BigDecimal vatAmount = BigDecimal.ZERO;

        // Chỉ tính VAT nếu vatPercent > 0 và baseAmount > 0
        if (vatPercent.compareTo(BigDecimal.ZERO) > 0 && baseAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Chuyển % → hệ số (vd: 10% → 0.10)
            BigDecimal vatRateDecimal = vatPercent
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

            // Tính tiền VAT, làm tròn về số tiền VND
            vatAmount = baseAmount
                    .multiply(vatRateDecimal)
                    .setScale(0, RoundingMode.HALF_UP);
        }

        // Số tiền cuối cùng sau khi trừ giảm giá và cộng VAT
        BigDecimal finalAmount = baseAmount.add(vatAmount);


        // 6. Tạo Invoice (lưu phương thức thanh toán dạng chuỗi)
        Invoice invoice = Invoice.builder()
                .orderId(orderId)
                .paymentMethod(paymentMethod != null ? paymentMethod.name() : null)
                // Tổng tiền hóa đơn sau khi trừ giảm giá và cộng VAT
                .totalAmount(finalAmount)
                // Lưu thêm thông tin giảm giá & mã voucher
                .discountAmount(discountAmount)
                .loyaltyEarnedPoint(loyaltyEarnedPoint != null ? loyaltyEarnedPoint : 0)
                .voucherCode(voucherCode)
                .paidAt(LocalDateTime.now())
                .build();

        invoiceRepository.save(invoice);

        // 7. Lưu danh sách InvoiceItem (snapshot món)
        List<InvoiceItem> invoiceItems = new ArrayList<>();
        for (OrderItem oi : items) {
            Dish d = dishMap.get(oi.getDishId());
            if (d == null) continue;

            InvoiceItem ii = InvoiceItem.builder()
                    .invoice(invoice)
                    .dishId(d.getId())
                    .dishName(d.getName())
                    .dishPrice(d.getPrice())
                    .quantity(oi.getQuantity())
                    .subtotal(d.getPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
                    .build();

            invoiceItems.add(ii);
        }
        invoiceItemRepository.saveAll(invoiceItems);

        // ✅ Audit log tạo hóa đơn
        auditLogService.log(
                AuditAction.INVOICE_CREATE,
                "invoice",
                invoice.getId(),
                null,
                invoice
        );
        // 8. Trả về entity Invoice để Payment gắn quan hệ
        return invoice;
    }

    // =====================================================================
    // 2. LẤY HÓA ĐƠN THEO ORDER ID
    // =====================================================================

    /**
     * Lấy hóa đơn theo orderId.
     * Dùng khi FE từ Order chuyển sang xem hóa đơn.
     */
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByOrderId(Long orderId) {

        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order chưa có hóa đơn"));

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(invoice.getId());

        return toInvoiceResponse(invoice, items);
    }

    // =====================================================================
    // 3. LẤY CHI TIẾT HÓA ĐƠN THEO INVOICE ID
    // =====================================================================

    /**
     * Lấy chi tiết 1 invoice theo ID.
     */
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceDetail(Long invoiceId) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hóa đơn"));

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(invoiceId);

        return toInvoiceResponse(invoice, items);
    }

    // =====================================================================
    // 4. HÀM CONVERT ENTITY → DTO
    // =====================================================================

    /**
     * Convert Invoice + InvoiceItem → InvoiceResponse (DTO trả ra FE)
     */
    private InvoiceResponse toInvoiceResponse(Invoice invoice, List<InvoiceItem> items) {

        // Convert danh sách InvoiceItem → InvoiceItemResponse
        List<InvoiceItemResponse> itemResponses =
                items.stream()
                        .map(ii -> InvoiceItemResponse.builder()
                                .dishId(ii.getDishId())
                                .dishName(ii.getDishName())
                                .dishPrice(ii.getDishPrice())
                                .quantity(ii.getQuantity())
                                .subtotal(ii.getSubtotal())
                                .build())
                        .toList();

        // --------------------------------------------------------------------
        // TÍNH LẠI SUBTOTAL, DISCOUNT, VAT ĐỂ TRẢ RA FE
        // --------------------------------------------------------------------

        // Tổng tiền hàng trước giảm (sum(subtotal))
        BigDecimal totalBeforeDiscount = itemResponses.stream()
                .map(InvoiceItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // discount từ invoice (có thể null)
        BigDecimal discountAmount = invoice.getDiscountAmount();
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }
        if (discountAmount.compareTo(totalBeforeDiscount) > 0) {
            discountAmount = totalBeforeDiscount;
        }

        // Số tiền sau giảm, trước VAT
        BigDecimal amountBeforeVat = totalBeforeDiscount.subtract(discountAmount);
        if (amountBeforeVat.compareTo(BigDecimal.ZERO) < 0) {
            amountBeforeVat = BigDecimal.ZERO;
        }

        // Đọc % VAT hiện tại (lưu ý: dùng setting hiện tại, không lưu trong invoice)
        BigDecimal vatPercent = systemSettingService.getNumberSetting(
                "vat.rate",
                BigDecimal.ZERO
        );
        if (vatPercent.compareTo(BigDecimal.ZERO) < 0) {
            vatPercent = BigDecimal.ZERO;
        }
        if (vatPercent.compareTo(new BigDecimal("100")) > 0) {
            vatPercent = new BigDecimal("100");
        }

        BigDecimal vatAmount = BigDecimal.ZERO;
        if (vatPercent.compareTo(BigDecimal.ZERO) > 0 && amountBeforeVat.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal vatRateDecimal = vatPercent
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

            vatAmount = amountBeforeVat
                    .multiply(vatRateDecimal)
                    .setScale(0, RoundingMode.HALF_UP);
        }

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .orderId(invoice.getOrderId())
                .paymentMethod(invoice.getPaymentMethod())
                .paidAt(invoice.getPaidAt())
                // Tổng tiền cuối cùng (sau discount + VAT)
                .totalAmount(invoice.getTotalAmount())
                // Số tiền giảm
                .discountAmount(discountAmount)
                // Thông tin VAT & subtotal (cho FE hiển thị)
                .amountBeforeVat(amountBeforeVat)
                .vatPercent(vatPercent)
                .vatAmount(vatAmount)
                // Voucher & thời gian
                .voucherCode(invoice.getVoucherCode())
                .loyaltyEarnedPoint(invoice.getLoyaltyEarnedPoint())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    /**
     * Tạo hóa đơn từ Order + OrderItem + Payment.
     * ------------------------------------------------------------
     * Hàm này được gọi trực tiếp từ PaymentService sau khi tạo Payment.
     *
     * Bước xử lý:
     *  - Lấy thông tin món ăn (Dish) để snapshot vào InvoiceItem
     *  - Tính tổng tiền từ OrderItem
     *  - Lưu Invoice
     *  - Lưu InvoiceItem (snapshot theo thời điểm thanh toán)
     *
     * @param order       Đơn hàng đã được thanh toán
     * @param orderItems  Danh sách món của order
     * @param payment     Payment vừa được tạo từ PaymentService
     * @return Invoice
     */
    @Transactional
    public Invoice createInvoice(Order order, List<OrderItem> orderItems, Payment payment) {

        // 1. Chuẩn bị map dishId → Dish
        Set<Long> dishIds = orderItems.stream()
                .map(OrderItem::getDishId)
                .collect(Collectors.toSet());

        Map<Long, Dish> dishMap = dishRepository.findAllById(dishIds)
                .stream()
                .collect(Collectors.toMap(Dish::getId, d -> d));

        // 2. Tính tổng tiền từ OrderItem
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem oi : orderItems) {
            Dish d = dishMap.get(oi.getDishId());
            if (d == null) continue;

            BigDecimal price = d.getPrice();
            BigDecimal qty = BigDecimal.valueOf(oi.getQuantity());

            total = total.add(price.multiply(qty));
        }

        // 3. Tạo Invoice entity
        Invoice invoice = Invoice.builder()
                .orderId(order.getId())
                .paymentMethod(payment.getMethod().name())  // Lấy method từ Payment
                .totalAmount(total)
                .paidAt(payment.getPaidAt())                // Cùng thời điểm thanh toán
                .build();

        invoiceRepository.save(invoice);

        // 4. Snapshot InvoiceItem
        List<InvoiceItem> invoiceItems = new ArrayList<>();

        for (OrderItem oi : orderItems) {
            Dish d = dishMap.get(oi.getDishId());

            InvoiceItem ii = InvoiceItem.builder()
                    .invoice(invoice)
                    .dishId(d.getId())
                    .dishName(d.getName())
                    .dishPrice(d.getPrice())
                    .quantity(oi.getQuantity())
                    .subtotal(d.getPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
                    .build();

            invoiceItems.add(ii);
        }

        invoiceItemRepository.saveAll(invoiceItems);

        // 5. Trả về Invoice để PaymentService có thể setInvoice(payment)
        return invoice;
    }

}
