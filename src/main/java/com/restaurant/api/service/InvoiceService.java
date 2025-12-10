package com.restaurant.api.service;

import com.restaurant.api.dto.invoice.InvoiceExportData;
import com.restaurant.api.dto.invoice.InvoiceItemResponse;
import com.restaurant.api.dto.invoice.InvoiceResponse;
import com.restaurant.api.entity.*;
import com.restaurant.api.enums.AuditAction;
import com.restaurant.api.enums.OrderStatus;
import com.restaurant.api.enums.PaymentMethod;
import com.restaurant.api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.math.RoundingMode;

/**
 * InvoiceService (REFECTOR PHASE 2)
 * ==============================================================================
 * Toàn bộ hàm trong class này đã được chuẩn hóa theo OrderItem entity mới:
 *
 *  - OrderItem không còn dishId / orderId dạng cột, mà dùng @ManyToOne:
 *        + oi.getDish()  → Dish entity
 *        + oi.getOrder() → Order entity
 *
 *  - Snapshot giá phải lấy từ:
 *        + oi.getSnapshotPrice(), nếu null → dish.getPrice()
 *
 *  - Không còn bất kỳ chỗ nào được gọi getDishId() hoặc getOrderId()
 * ==============================================================================
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
    private final SystemSettingService systemSettingService;
    private final SystemSettingRepository systemSettingRepository;

    /**
     * 1) TẠO HÓA ĐƠN TỪ ORDER
     * Tạo Invoice từ Order với đầy đủ snapshot tiền.
     * ----------------------------------------------------------------------
     * Toàn bộ logic tính toán (tổng tiền, giảm giá, VAT, loyalty, tiền khách trả)
     * đã được xử lý tại PaymentService.
     *
     * Hàm này CHỈ nhận dữ liệu snapshot và lưu lại vào bảng invoice.
     */
    @Transactional
    public Invoice createInvoiceFromOrder(Long orderId,
                                          PaymentMethod paymentMethod,
                                          String voucherCode,
                                          BigDecimal originalTotal,
                                          BigDecimal voucherDiscount,
                                          BigDecimal defaultDiscount,
                                          BigDecimal discountAmount,
                                          BigDecimal amountBeforeVat,
                                          BigDecimal vatRate,
                                          BigDecimal vatAmount,
                                          BigDecimal finalAmount,
                                          Integer loyaltyEarnedPoint,
                                          BigDecimal customerPaid,
                                          BigDecimal changeAmount) {

        // 1. Kiểm tra order tồn tại
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.SERVING && order.getStatus() != OrderStatus.PAID) {
            // Sau khi refactor PaymentService, thường createInvoiceFromOrder được gọi
            // ngay trước khi set Order sang PAID.
            throw new RuntimeException("Chỉ tạo hóa đơn khi đơn hàng đang SERVING hoặc vừa thanh toán");
        }

        // 2. Load danh sách OrderItem để snapshot InvoiceItem
        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);
        if (items.isEmpty()) {
            throw new RuntimeException("Order không có món ăn");
        }

        // 3. Chuẩn hóa các giá trị tiền (tránh null, tránh âm)
        originalTotal = defaultBigDecimal(originalTotal);
        voucherDiscount = defaultBigDecimal(voucherDiscount);
        defaultDiscount = defaultBigDecimal(defaultDiscount);
        discountAmount = defaultBigDecimal(discountAmount);
        amountBeforeVat = defaultBigDecimal(amountBeforeVat);
        vatRate = vatRate == null ? BigDecimal.ZERO : vatRate;
        vatAmount = defaultBigDecimal(vatAmount);
        finalAmount = defaultBigDecimal(finalAmount);
        customerPaid = customerPaid; // có thể null (hóa đơn cũ)
        changeAmount = changeAmount; // có thể null (hóa đơn cũ)

        // Đảm bảo không âm & discount không vượt quá originalTotal
        if (discountAmount.compareTo(originalTotal) > 0) {
            discountAmount = originalTotal;
        }
        if (amountBeforeVat.compareTo(BigDecimal.ZERO) < 0) {
            amountBeforeVat = BigDecimal.ZERO;
        }

        // 4. Tạo Invoice với snapshot tiền
        Invoice invoice = Invoice.builder()
                .orderId(orderId)
                .paymentMethod(paymentMethod != null ? paymentMethod.name() : null)
                .voucherCode(voucherCode)
                .originalTotalAmount(originalTotal)
                .voucherDiscountAmount(voucherDiscount)
                .defaultDiscountAmount(defaultDiscount)
                .discountAmount(discountAmount)
                .amountBeforeVat(amountBeforeVat)
                .vatRate(vatRate)
                .vatAmount(vatAmount)
                .totalAmount(finalAmount) // finalAmount = tiền cuối cùng cần thanh toán
                .loyaltyEarnedPoint(loyaltyEarnedPoint != null ? loyaltyEarnedPoint : 0)
                .customerPaid(customerPaid)
                .changeAmount(changeAmount)
                .paidAt(LocalDateTime.now())
                .build();

        invoiceRepository.save(invoice);

        // 5. Tạo InvoiceItem (snapshot món ăn)
        List<InvoiceItem> invoiceItems = new ArrayList<>();

        for (OrderItem oi : items) {
            Dish dish = oi.getDish();
            if (dish == null) continue;

            BigDecimal price = oi.getSnapshotPrice() != null
                    ? oi.getSnapshotPrice()
                    : dish.getPrice();

            InvoiceItem ii = InvoiceItem.builder()
                    .invoice(invoice)
                    .dishId(dish.getId())
                    .dishName(dish.getName())
                    .dishPrice(price)
                    .quantity(oi.getQuantity())
                    .subtotal(price.multiply(BigDecimal.valueOf(oi.getQuantity())))
                    .build();

            invoiceItems.add(ii);
        }

        invoiceItemRepository.saveAll(invoiceItems);

        // 6. Audit log
        auditLogService.log(
                AuditAction.INVOICE_CREATE,
                "invoice",
                invoice.getId(),
                null,
                invoice
        );

        return invoice;
    }

    // =====================================================================================
    // 2) LẤY HÓA ĐƠN THEO ORDER
    // =====================================================================================
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByOrderId(Long orderId) {

        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order chưa có hóa đơn"));

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(invoice.getId());

        return toInvoiceResponse(invoice, items);
    }

    // =====================================================================================
    // 3) XEM CHI TIẾT HÓA ĐƠN – PHASE 2
    // =====================================================================================
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceDetail(Long invoiceId) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hóa đơn"));

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(invoiceId);

        return toInvoiceResponse(invoice, items);
    }

    // =====================================================================================
    // 4) CONVERT ENTITY → DTO
    // =====================================================================================
    private InvoiceResponse toInvoiceResponse(Invoice invoice, List<InvoiceItem> items) {

        List<InvoiceItemResponse> itemResponses = items.stream()
                .map(ii -> InvoiceItemResponse.builder()
                        .dishId(ii.getDishId())
                        .dishName(ii.getDishName())
                        .dishPrice(ii.getDishPrice())
                        .quantity(ii.getQuantity())
                        .subtotal(ii.getSubtotal())
                        .build())
                .toList();

        // Tính tổng trước giảm từ item (dùng để fallback nếu thiếu snapshot)
        BigDecimal totalFromItems = items.stream()
                .map(InvoiceItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ưu tiên dùng snapshot từ Invoice, nếu null thì fallback từ item
        BigDecimal originalTotal = invoice.getOriginalTotalAmount() != null
                ? invoice.getOriginalTotalAmount()
                : totalFromItems;

        BigDecimal discountAmount = Optional.ofNullable(invoice.getDiscountAmount())
                .orElse(BigDecimal.ZERO);

        BigDecimal voucherDiscount = Optional.ofNullable(invoice.getVoucherDiscountAmount())
                .orElse(BigDecimal.ZERO);

        BigDecimal defaultDiscount = Optional.ofNullable(invoice.getDefaultDiscountAmount())
                .orElse(BigDecimal.ZERO);

        BigDecimal amountBeforeVat = Optional.ofNullable(invoice.getAmountBeforeVat())
                .orElse(originalTotal.subtract(discountAmount));

        BigDecimal vatPercent = Optional.ofNullable(invoice.getVatRate())
                .orElse(BigDecimal.ZERO);

        BigDecimal vatAmount = Optional.ofNullable(invoice.getVatAmount())
                .orElse(BigDecimal.ZERO);

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .orderId(invoice.getOrderId())
                .paymentMethod(invoice.getPaymentMethod())
                .paidAt(invoice.getPaidAt())
                .totalAmount(invoice.getTotalAmount())
                .discountAmount(discountAmount)
                .voucherCode(invoice.getVoucherCode())
                .amountBeforeVat(amountBeforeVat)
                .vatPercent(vatPercent)
                .vatAmount(vatAmount)
                .originalTotalAmount(originalTotal)
                .voucherDiscountAmount(voucherDiscount)
                .defaultDiscountAmount(defaultDiscount)
                .loyaltyEarnedPoint(
                        invoice.getLoyaltyEarnedPoint() != null
                                ? invoice.getLoyaltyEarnedPoint()
                                : 0
                )
                .customerPaid(invoice.getCustomerPaid())
                .changeAmount(invoice.getChangeAmount())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    // =====================================================================================
    // 5) HÀM SỬ DỤNG CHO EXPORT PDF (A5 / THERMAL)
    // =====================================================================================
    @Transactional(readOnly = true)
    public InvoiceExportData buildInvoiceExportData(Long invoiceId) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        List<InvoiceItem> invoiceItems = invoiceItemRepository.findByInvoiceId(invoiceId);

        if (invoiceItems.isEmpty()) {
            throw new RuntimeException("InvoiceItem trống");
        }

        List<InvoiceExportData.Item> itemDTOs = invoiceItems.stream()
                .map(ii -> InvoiceExportData.Item.builder()
                        .dishName(ii.getDishName())
                        .dishPrice(ii.getDishPrice())
                        .quantity(ii.getQuantity())
                        .subtotal(ii.getSubtotal())
                        .build())
                .toList();

        // Lấy thông tin nhà hàng
        String restaurantName = getSetting("restaurant.name");
        String restaurantAddress = getSetting("restaurant.address");
        String restaurantPhone = getSetting("restaurant.phone");
        String restaurantTaxId = getSetting("restaurant.tax_id");

        // Tính tiền xuất PDF
        // Ưu tiên dùng snapshot từ Invoice
        BigDecimal originalTotal = Optional.ofNullable(invoice.getOriginalTotalAmount())
                .orElse(
                        invoiceItems.stream()
                                .map(InvoiceItem::getSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                );

        BigDecimal discountAmount = Optional.ofNullable(invoice.getDiscountAmount())
                .orElse(BigDecimal.ZERO);

        BigDecimal amountBeforeVat = Optional.ofNullable(invoice.getAmountBeforeVat())
                .orElse(originalTotal.subtract(discountAmount));

        BigDecimal vatPercent = Optional.ofNullable(invoice.getVatRate())
                .orElse(BigDecimal.ZERO);

        BigDecimal vatAmount = Optional.ofNullable(invoice.getVatAmount())
                .orElse(BigDecimal.ZERO);

        BigDecimal finalAmount = Optional.ofNullable(invoice.getTotalAmount())
                .orElse(amountBeforeVat.add(vatAmount));


        return InvoiceExportData.builder()
                .restaurantName(restaurantName)
                .restaurantAddress(restaurantAddress)
                .restaurantPhone(restaurantPhone)
                .restaurantTaxId(restaurantTaxId)
                .invoiceId(invoice.getId())
                .orderId(invoice.getOrderId())
                .paidAt(invoice.getPaidAt())
                .paymentMethod(invoice.getPaymentMethod())
                .items(itemDTOs)
                .totalBeforeDiscount(originalTotal)
                .discountAmount(discountAmount)
                .amountBeforeVat(amountBeforeVat)
                .vatPercent(vatPercent)
                .vatAmount(vatAmount)
                .finalAmount(finalAmount)
                .voucherCode(invoice.getVoucherCode())
                .loyaltyEarnedPoint(
                        invoice.getLoyaltyEarnedPoint() != null
                                ? invoice.getLoyaltyEarnedPoint()
                                : 0
                )
                .customerPaid(invoice.getCustomerPaid())
                .changeAmount(invoice.getChangeAmount())
                .build();
    }

    private String getSetting(String key) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse("");
    }

    /**
     * Hàm tiện ích: nếu null → trả về 0.
     */
    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
