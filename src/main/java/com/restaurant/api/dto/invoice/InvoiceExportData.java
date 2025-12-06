package com.restaurant.api.dto.invoice;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * InvoiceExportData
 * =====================================================================
 * DTO chuẩn dùng cho việc EXPORT HÓA ĐƠN (PDF A5 / PDF Thermal).
 *
 * Lý do tách riêng:
 *  - Không để mỗi exporter tự xử lý logic → dễ sai lệch.
 *  - Mọi tính toán (VAT, discount, subtotal...) được gom tại InvoiceService.
 *  - Exporter chỉ việc render layout theo dữ liệu đã chuẩn hoá.
 *
 * Gồm 5 nhóm dữ liệu:
 *  1) Thông tin cửa hàng (restaurant.* từ SystemSetting)
 *  2) Thông tin hóa đơn
 *  3) Danh sách món (snapshot từ InvoiceItem)
 *  4) Thông tin tiền (tổng trước giảm, giảm giá, VAT, tổng cuối cùng)
 *  5) Thông tin phụ (voucher, loyalty point)
 *
 * Thermal layout:
 *  - KHÔNG hiển thị taxId theo rule Phase 1.
 * =====================================================================
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceExportData {

    // ============================================================
    // 1) THÔNG TIN CỬA HÀNG (từ SystemSetting)
    // ============================================================
    private String restaurantName;       // restaurant.name
    private String restaurantAddress;    // restaurant.address
    private String restaurantPhone;      // restaurant.phone
    private String restaurantTaxId;      // restaurant.tax_id (A5 dùng, Thermal không dùng)

    // ============================================================
    // 2) THÔNG TIN HÓA ĐƠN
    // ============================================================
    private Long invoiceId;
    private Long orderId;
    private LocalDateTime paidAt;
    private String paymentMethod;

    // ============================================================
    // 3) DANH SÁCH MÓN
    // ============================================================
    private List<Item> items;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String dishName;       // Tên món tại thời điểm thanh toán
        private BigDecimal dishPrice;  // Đơn giá snapshot
        private Integer quantity;      // Số lượng
        private BigDecimal subtotal;   // dishPrice * quantity
    }

    // ============================================================
    // 4) THÔNG TIN TIỀN TỆ
    // ============================================================
    private BigDecimal totalBeforeDiscount; // Tổng tiền món
    private BigDecimal discountAmount;      // Tổng giảm giá (voucher+default)
    private BigDecimal amountBeforeVat;     // Sau giảm giá, trước VAT
    private BigDecimal vatPercent;          // %
    private BigDecimal vatAmount;           // Tiền VAT
    private BigDecimal finalAmount;         // Số tiền cuối cùng cần thanh toán

    // ============================================================
    // 5) THÔNG TIN KHÁC
    // ============================================================
    private String voucherCode;             // Mã voucher nếu có
    private Integer loyaltyEarnedPoint;     // Điểm tích được
}
