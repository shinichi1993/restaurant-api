package com.restaurant.api.export.invoice;

import com.restaurant.api.dto.invoice.InvoiceExportData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * InvoiceHtmlRenderer
 * =====================================================================
 * Class chuyên dùng để RENDER HÓA ĐƠN THÀNH HTML (để in ra máy POS).
 *
 * Mục đích:
 *  - KHÔNG in PDF, mà trả về 1 đoạn HTML chuẩn bill 80mm
 *  - FE mở HTML này trong cửa sổ mới → gọi window.print() để in trực tiếp
 *
 * Lưu ý:
 *  - KHÔNG tính toán lại tiền (VAT, giảm giá...). Toàn bộ logic
 *    đã nằm trong InvoiceExportData (build ở InvoiceService).
 *  - Class này chỉ chịu trách nhiệm GHÉP HTML.
 * =====================================================================
 */
@Component
@RequiredArgsConstructor
public class InvoiceHtmlRenderer {

    // Định dạng ngày giờ để hiển thị trên bill
    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Hàm public chính để render HTML từ InvoiceExportData.
     * --------------------------------------------------------------
     * @param data dữ liệu hóa đơn đã chuẩn hóa
     * @return String HTML hoàn chỉnh để FE in ra
     */
    public String render(InvoiceExportData data) {
        StringBuilder sb = new StringBuilder();

        // =========================================================
        // 1. PHẦN <head> – meta + CSS cho khổ giấy 80mm
        // =========================================================
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<meta charset='UTF-8'/>");
        sb.append("<title>Hóa đơn</title>");

        // CSS đơn giản cho bill nhiệt (80mm)
        sb.append("<style>");
        sb.append("body { width: 80mm; margin: 0; padding: 4px; ");
        sb.append("font-family: Arial, sans-serif; font-size: 12px; }");

        sb.append(".center { text-align: center; }");
        sb.append(".right { text-align: right; }");
        sb.append(".row { display: flex; justify-content: space-between; }");
        sb.append(".mt-4 { margin-top: 4px; }");
        sb.append(".mt-8 { margin-top: 8px; }");
        sb.append(".line { border-bottom: 1px dashed #000; margin: 6px 0; }");
        sb.append(".bold { font-weight: bold; }");
        sb.append("</style>");

        sb.append("</head>");
        sb.append("<body>");

        // =========================================================
        // 2. HEADER CỬA HÀNG
        // =========================================================
        String storeName = safe(data.getRestaurantName());
        if (storeName.isEmpty()) {
            storeName = "TÊN CỬA HÀNG";
        }
        sb.append("<div class='center bold'>").append(escape(storeName)).append("</div>");

        String addr = safe(data.getRestaurantAddress());
        if (!addr.isEmpty()) {
            sb.append("<div class='center'>").append(escape(addr)).append("</div>");
        }

        String phone = safe(data.getRestaurantPhone());
        if (!phone.isEmpty()) {
            sb.append("<div class='center'>ĐT: ").append(escape(phone)).append("</div>");
        }

        sb.append("<div class='line'></div>");

        // =========================================================
        // 3. TIÊU ĐỀ HÓA ĐƠN
        // =========================================================
        sb.append("<div class='center bold mt-4'>HÓA ĐƠN THANH TOÁN</div>");

        // =========================================================
        // 4. THÔNG TIN HÓA ĐƠN
        // =========================================================
        sb.append("<div class='mt-4'>");
        sb.append("<div class='row'><span>HĐ:</span><span>")
                .append(data.getInvoiceId())
                .append("</span></div>");

        if (data.getOrderId() != null) {
            sb.append("<div class='row'><span>Order:</span><span>")
                    .append(data.getOrderId())
                    .append("</span></div>");
        }

        String paidText = "-";
        if (data.getPaidAt() != null) {
            paidText = data.getPaidAt().format(DATE_TIME_FMT);
        }
        sb.append("<div class='row'><span>Thời gian:</span><span>")
                .append(escape(paidText))
                .append("</span></div>");

        String method = safe(data.getPaymentMethod());
        sb.append("<div class='row'><span>Thanh toán:</span><span>")
                .append(method.isEmpty() ? "-" : escape(method))
                .append("</span></div>");

        sb.append("</div>"); // end info

        sb.append("<div class='line'></div>");

        // =========================================================
        // 5. DANH SÁCH MÓN
        // =========================================================
        if (data.getItems() != null && !data.getItems().isEmpty()) {
            for (InvoiceExportData.Item item : data.getItems()) {
                // Tên món trên 1 dòng riêng
                sb.append("<div class='mt-4'>")
                        .append(escape(safe(item.getDishName())))
                        .append("</div>");

                // Dòng số lượng + thành tiền
                Long qty = item.getQuantity() != null ? item.getQuantity() : 0L;
                sb.append("<div class='row'>");
                sb.append("<span>SL: ").append(qty).append("</span>");
                sb.append("<span>").append(formatMoney(item.getSubtotal())).append("</span>");
                sb.append("</div>");
            }
        }

        sb.append("<div class='line'></div>");

        // =========================================================
        // 6. TỔNG TIỀN (HÀNG + GIẢM GIÁ + VAT + FINAL)
        // =========================================================
        sb.append("<div class='mt-4'>");

        // Tổng tiền hàng
        sb.append("<div class='row'>");
        sb.append("<span>Tổng tiền hàng</span>");
        sb.append("<span>").append(formatMoney(data.getTotalBeforeDiscount())).append("</span>");
        sb.append("</div>");

        // Giảm giá
        sb.append("<div class='row'>");
        sb.append("<span>Giảm giá</span>");
        sb.append("<span>").append(formatMoney(data.getDiscountAmount())).append("</span>");
        sb.append("</div>");

        // VAT
        sb.append("<div class='row'>");
        sb.append("<span>VAT</span>");
        sb.append("<span>").append(formatMoney(data.getVatAmount())).append("</span>");
        sb.append("</div>");

        sb.append("<div class='line'></div>");

        // Tổng thanh toán (in đậm)
        sb.append("<div class='row bold'>");
        sb.append("<span>TỔNG THANH TOÁN</span>");
        sb.append("<span>").append(formatMoney(data.getFinalAmount())).append("</span>");
        sb.append("</div>");

        sb.append("</div>"); // end totals

        sb.append("<div class='line'></div>");

        // =========================================================
        // 7. VOUCHER + ĐIỂM LOYALTY (NẾU CÓ)
        // =========================================================
        boolean hasVoucher = data.getVoucherCode() != null && !data.getVoucherCode().isBlank();
        boolean hasPoint = data.getLoyaltyEarnedPoint() != null && data.getLoyaltyEarnedPoint() > 0;

        if (hasVoucher) {
            sb.append("<div>Voucher: ")
                    .append(escape(data.getVoucherCode()))
                    .append("</div>");
        }

        if (hasPoint) {
            sb.append("<div>Điểm tích lũy: ")
                    .append(data.getLoyaltyEarnedPoint())
                    .append("</div>");
        }

        // =========================================================
        // 8. FOOTER CẢM ƠN
        // =========================================================
        sb.append("<div class='mt-8 center'>Cảm ơn quý khách!</div>");

        sb.append("</body>");
        sb.append("</html>");

        return sb.toString();
    }

    // =================================================================
    // HÀM TIỆN ÍCH
    // =================================================================

    /** Tránh null cho String */
    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /** Escape 1 số ký tự đặc biệt trong HTML để tránh lỗi render */
    private String escape(String s) {
        return safe(s)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /** Format tiền kiểu "120,000 đ" từ BigDecimal */
    private String formatMoney(BigDecimal v) {
        if (v == null) return "0 đ";
        return String.format("%,.0f đ", v.doubleValue());
    }
}
