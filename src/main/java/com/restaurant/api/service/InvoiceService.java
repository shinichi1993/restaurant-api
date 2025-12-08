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

    // =====================================================================================
    // 1) TẠO HÓA ĐƠN TỪ ORDER – CHUẨN PHASE 2
    // =====================================================================================
    @Transactional
    public Invoice createInvoiceFromOrder(Long orderId,
                                          PaymentMethod paymentMethod,
                                          String voucherCode,
                                          BigDecimal discountAmount,
                                          Integer loyaltyEarnedPoint) {

        // 1. Kiểm tra order tồn tại
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.SERVING) {
            throw new RuntimeException("Chỉ tạo hóa đơn khi đơn hàng đang SERVING");
        }

        // 2. Load danh sách OrderItem (giờ dùng findByOrder_Id)
        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);
        if (items.isEmpty()) {
            throw new RuntimeException("Order không có món ăn");
        }

        // 3. Tính tổng tiền theo snapshotPrice (Phase 2)
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem oi : items) {
            Dish dish = oi.getDish();
            if (dish == null) continue;

            BigDecimal price = oi.getSnapshotPrice() != null
                    ? oi.getSnapshotPrice()
                    : dish.getPrice();

            total = total.add(price.multiply(BigDecimal.valueOf(oi.getQuantity())));
        }

        // ---- XỬ LÝ DISCOUNT + VAT (giữ nguyên logic cũ, không sửa ở bước này) ----
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) discountAmount = BigDecimal.ZERO;
        if (discountAmount.compareTo(total) > 0) discountAmount = total;

        BigDecimal baseAmount = total.subtract(discountAmount);
        if (baseAmount.compareTo(BigDecimal.ZERO) < 0) baseAmount = BigDecimal.ZERO;

        BigDecimal vatPercent = systemSettingService.getNumberSetting("vat.rate", BigDecimal.ZERO);
        vatPercent = vatPercent.max(BigDecimal.ZERO).min(new BigDecimal("100"));

        BigDecimal vatAmount = BigDecimal.ZERO;
        if (vatPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal vatRate = vatPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            vatAmount = baseAmount.multiply(vatRate).setScale(0, RoundingMode.HALF_UP);
        }

        BigDecimal finalAmount = baseAmount.add(vatAmount);

        // 4. Tạo Invoice
        Invoice invoice = Invoice.builder()
                .orderId(orderId)
                .paymentMethod(paymentMethod != null ? paymentMethod.name() : null)
                .totalAmount(finalAmount)
                .discountAmount(discountAmount)
                .voucherCode(voucherCode)
                .loyaltyEarnedPoint(loyaltyEarnedPoint != null ? loyaltyEarnedPoint : 0)
                .paidAt(LocalDateTime.now())
                .build();

        invoiceRepository.save(invoice);

        // 5. Tạo InvoiceItem (snapshot)
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

        // Log
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

        // Tính phần tiền
        BigDecimal totalBeforeDiscount = itemResponses.stream()
                .map(InvoiceItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = Optional.ofNullable(invoice.getDiscountAmount())
                .orElse(BigDecimal.ZERO);

        discountAmount = discountAmount.max(BigDecimal.ZERO).min(totalBeforeDiscount);

        BigDecimal amountBeforeVat = totalBeforeDiscount.subtract(discountAmount);

        BigDecimal vatPercent = systemSettingService.getNumberSetting("vat.rate", BigDecimal.ZERO)
                .max(BigDecimal.ZERO)
                .min(new BigDecimal("100"));

        BigDecimal vatAmount = BigDecimal.ZERO;

        if (amountBeforeVat.compareTo(BigDecimal.ZERO) > 0 &&
                vatPercent.compareTo(BigDecimal.ZERO) > 0) {

            vatAmount = amountBeforeVat
                    .multiply(vatPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                    .setScale(0, RoundingMode.HALF_UP);
        }

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .orderId(invoice.getOrderId())
                .paymentMethod(invoice.getPaymentMethod())
                .paidAt(invoice.getPaidAt())
                .totalAmount(invoice.getTotalAmount())
                .discountAmount(discountAmount)
                .amountBeforeVat(amountBeforeVat)
                .vatPercent(vatPercent)
                .vatAmount(vatAmount)
                .voucherCode(invoice.getVoucherCode())
                .loyaltyEarnedPoint(
                        invoice.getLoyaltyEarnedPoint() != null
                                ? invoice.getLoyaltyEarnedPoint()
                                : 0
                )
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
        BigDecimal totalBeforeDiscount = itemDTOs.stream()
                .map(InvoiceExportData.Item::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = Optional.ofNullable(invoice.getDiscountAmount())
                .orElse(BigDecimal.ZERO);

        discountAmount = discountAmount.max(BigDecimal.ZERO).min(totalBeforeDiscount);

        BigDecimal amountBeforeVat = totalBeforeDiscount.subtract(discountAmount);

        BigDecimal vatPercent = systemSettingService.getNumberSetting(
                "vat.rate",
                BigDecimal.ZERO
        ).max(BigDecimal.ZERO).min(new BigDecimal("100"));

        BigDecimal vatAmount = BigDecimal.ZERO;

        if (amountBeforeVat.compareTo(BigDecimal.ZERO) > 0 &&
                vatPercent.compareTo(BigDecimal.ZERO) > 0) {

            vatAmount = amountBeforeVat.multiply(
                    vatPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
            ).setScale(0, RoundingMode.HALF_UP);
        }

        BigDecimal finalAmount = amountBeforeVat.add(vatAmount);

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
                .totalBeforeDiscount(totalBeforeDiscount)
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
                .build();
    }

    private String getSetting(String key) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse("");
    }
}
