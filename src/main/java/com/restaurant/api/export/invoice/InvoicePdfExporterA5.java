package com.restaurant.api.export.invoice;

import com.lowagie.text.*;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.restaurant.api.dto.invoice.InvoiceExportData;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

// ========================== PDF (OpenPDF) ==========================
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.BaseFont;

/**
 * InvoicePdfExporterA5
 * =====================================================================
 * Export hóa đơn ra PDF khổ A5 (dọc), layout đầy đủ giống hóa đơn
 * của các quán lớn.
 *
 * Nhiệm vụ:
 *  - Nhận vào dữ liệu đã chuẩn hóa từ InvoiceExportData
 *  - Render ra file PDF khổ A5 với:
 *      + Thông tin cửa hàng
 *      + Thông tin hóa đơn
 *      + Bảng món ăn
 *      + Tổng tiền hàng, giảm giá, VAT, tổng thanh toán
 *      + Voucher + điểm loyalty (nếu có)
 *      + Footer cảm ơn khách hàng
 *
 * Lưu ý:
 *  - KHÔNG tính toán lại tiền (VAT, discount...) trong class này.
 *    Mọi logic đã được tính ở InvoiceService.buildInvoiceExportData().
 *  - Class này chỉ chịu trách nhiệm "vẽ" PDF.
 *
 * Font:
 *  - Dùng font Unicode: fonts/arial.ttf
 *  - Cần đảm bảo file fonts/arial.ttf có trong resources.
 * =====================================================================
 */
@Component
public class InvoicePdfExporterA5 {

    // Định dạng ngày giờ thanh toán
    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Định dạng ngày in footer
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Hàm public chính để export hóa đơn ra PDF.
     * --------------------------------------------------------------
     * @param data dữ liệu hóa đơn đã được chuẩn hóa
     * @return mảng byte[] nội dung file PDF
     */
    public byte[] export(InvoiceExportData data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Khởi tạo document khổ A5 dọc, chừa lề trái/phải/trên/dưới
            Document doc = new Document(PageSize.A5, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ===================== FONT UNICODE ======================
            String fontPath = "fonts/arial.ttf";
            Font fontTitle = FontFactory.getFont(fontPath,
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 14, Font.BOLD);
            Font fontStoreName = FontFactory.getFont(fontPath,
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 12, Font.BOLD);
            Font fontNormal = FontFactory.getFont(fontPath,
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10);
            Font fontBold = FontFactory.getFont(fontPath,
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10, Font.BOLD);
            Font fontBigTotal = FontFactory.getFont(fontPath,
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 11, Font.BOLD);

            // ========================================================
            // 1. HEADER CỬA HÀNG
            // ========================================================
            addStoreHeader(doc, data, fontStoreName, fontNormal);

            // ========================================================
            // 2. TIÊU ĐỀ HÓA ĐƠN
            // ========================================================
            Paragraph title = new Paragraph("HÓA ĐƠN THANH TOÁN", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(5);
            title.setSpacingAfter(10);
            doc.add(title);

            // ========================================================
            // 3. THÔNG TIN HÓA ĐƠN
            // ========================================================
            addInvoiceInfo(doc, data, fontNormal);

            // ========================================================
            // 4. BẢNG MÓN ĂN
            // ========================================================
            addItemsTable(doc, data, fontNormal, fontBold);

            // ========================================================
            // 5. TỔNG KẾT TIỀN (CANH PHẢI)
            // ========================================================
            addTotalsBlock(doc, data, fontNormal, fontBigTotal);

            // ========================================================
            // 6. VOUCHER + LOYALTY (NẾU CÓ)
            // ========================================================
            addVoucherAndLoyalty(doc, data, fontNormal);

            // ========================================================
            // 7. FOOTER CẢM ƠN
            // ========================================================
            addFooter(doc, data, fontNormal);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất PDF hóa đơn (A5)", e);
        }
    }

    // =====================================================================
    // HÀM PHỤ: HEADER CỬA HÀNG
    // =====================================================================

    /**
     * Vẽ phần header cửa hàng:
     *  - Tên cửa hàng (in đậm, to, canh giữa)
     *  - Địa chỉ
     *  - Số điện thoại
     *  - Mã số thuế (restaurantTaxId) nếu có
     */
    private void addStoreHeader(Document doc,
                                InvoiceExportData data,
                                Font fontStoreName,
                                Font fontNormal) throws DocumentException {

        // Tên cửa hàng
        String storeName = safe(data.getRestaurantName());
        if (storeName.isEmpty()) {
            storeName = "TÊN CỬA HÀNG";
        }
        Paragraph pName = new Paragraph(storeName, fontStoreName);
        pName.setAlignment(Element.ALIGN_CENTER);
        pName.setSpacingAfter(2);
        doc.add(pName);

        // Địa chỉ
        String address = safe(data.getRestaurantAddress());
        if (!address.isEmpty()) {
            Paragraph pAddress = new Paragraph("Địa chỉ: " + address, fontNormal);
            pAddress.setAlignment(Element.ALIGN_CENTER);
            pAddress.setSpacingAfter(2);
            doc.add(pAddress);
        }

        // Số điện thoại
        String phone = safe(data.getRestaurantPhone());
        if (!phone.isEmpty()) {
            Paragraph pPhone = new Paragraph("Điện thoại: " + phone, fontNormal);
            pPhone.setAlignment(Element.ALIGN_CENTER);
            pPhone.setSpacingAfter(2);
            doc.add(pPhone);
        }

        // Mã số thuế (chỉ A5 dùng)
        String taxId = safe(data.getRestaurantTaxId());
        if (!taxId.isEmpty()) {
            Paragraph pTax = new Paragraph("MST: " + taxId, fontNormal);
            pTax.setAlignment(Element.ALIGN_CENTER);
            pTax.setSpacingAfter(5);
            doc.add(pTax);
        } else {
            // Nếu không có MST thì chừa khoảng cách
            Paragraph sep = new Paragraph(" ", fontNormal);
            sep.setSpacingAfter(5);
            doc.add(sep);
        }
    }

    // =====================================================================
    // HÀM PHỤ: THÔNG TIN HÓA ĐƠN
    // =====================================================================

    /**
     * Hiển thị các thông tin:
     *  - Mã hóa đơn
     *  - Mã order
     *  - Ngày giờ thanh toán
     *  - Phương thức thanh toán
     */
    private void addInvoiceInfo(Document doc,
                                InvoiceExportData data,
                                Font fontNormal) throws DocumentException {

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 7});
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // Mã hóa đơn
        addInfoRow(table, "Mã hóa đơn:", String.valueOf(data.getInvoiceId()), fontNormal);

        // Mã order
        Long orderId = data.getOrderId();
        addInfoRow(table, "Mã order:", orderId != null ? String.valueOf(orderId) : "-", fontNormal);

        // Ngày giờ thanh toán
        String paidAtText = "-";
        if (data.getPaidAt() != null) {
            paidAtText = data.getPaidAt().format(DATE_TIME_FMT);
        }
        addInfoRow(table, "Thanh toán:", paidAtText, fontNormal);

        // Phương thức thanh toán
        String methodText = safe(data.getPaymentMethod());
        addInfoRow(table, "Phương thức:", methodText.isEmpty() ? "-" : methodText, fontNormal);

        table.setSpacingAfter(8);
        doc.add(table);
    }

    private void addInfoRow(PdfPTable table,
                            String label,
                            String value,
                            Font fontNormal) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, fontNormal));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, fontNormal));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(c2);
    }

    // =====================================================================
    // HÀM PHỤ: BẢNG MÓN ĂN
    // =====================================================================

    /**
     * Bảng món ăn gồm 4 cột:
     *  - Món ăn | SL | Đơn giá | Thành tiền
     */
    private void addItemsTable(Document doc,
                               InvoiceExportData data,
                               Font fontNormal,
                               Font fontHeader) throws DocumentException {

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{5, 1.2f, 2.2f, 2.4f});
        table.setSpacingAfter(8);

        // Header có nền xám nhạt
        addHeaderCell(table, "Món ăn", fontHeader);
        addHeaderCell(table, "SL", fontHeader);
        addHeaderCell(table, "Đơn giá", fontHeader);
        addHeaderCell(table, "Thành tiền", fontHeader);

        // Dữ liệu từng dòng
        if (data.getItems() != null) {
            for (InvoiceExportData.Item item : data.getItems()) {
                addBodyCell(table, safe(item.getDishName()), fontNormal, Element.ALIGN_LEFT);
                addBodyCell(table,
                        item.getQuantity() != null ? String.valueOf(item.getQuantity()) : "0",
                        fontNormal, Element.ALIGN_RIGHT);
                addBodyCell(table, formatMoney(item.getDishPrice()), fontNormal, Element.ALIGN_RIGHT);
                addBodyCell(table, formatMoney(item.getSubtotal()), fontNormal, Element.ALIGN_RIGHT);
            }
        }

        doc.add(table);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(230, 230, 230));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table,
                             String text,
                             Font font,
                             int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(4);
        table.addCell(cell);
    }

    // =====================================================================
    // HÀM PHỤ: KHỐI TỔNG TIỀN
    // =====================================================================

    /**
     * Khối tổng tiền, canh phải:
     *  - Tổng tiền hàng
     *  - Giảm giá
     *  - Thành tiền trước VAT
     *  - Thuế VAT (x%)
     *  - TỔNG THANH TOÁN
     */
    private void addTotalsBlock(Document doc,
                                InvoiceExportData data,
                                Font fontNormal,
                                Font fontBigTotal) throws DocumentException {

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60); // chiếm 60% chiều ngang
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingAfter(8);
        table.setWidths(new float[]{4, 3});

        // Hàng: Tổng tiền hàng
        addTotalRow(table, "Tổng tiền hàng:",
                formatMoney(data.getTotalBeforeDiscount()), fontNormal);

        // Hàng: Giảm giá
        addTotalRow(table, "Giảm giá:",
                formatMoney(data.getDiscountAmount()), fontNormal);

        // Hàng: Thành tiền trước VAT
        addTotalRow(table, "Thành tiền trước VAT:",
                formatMoney(data.getAmountBeforeVat()), fontNormal);

        // Hàng: Thuế VAT (x%)
        BigDecimal vatPercent = data.getVatPercent() != null ? data.getVatPercent() : BigDecimal.ZERO;
        String vatLabel = "Thuế VAT (" + vatPercent.stripTrailingZeros().toPlainString() + "%):";
        addTotalRow(table, vatLabel,
                formatMoney(data.getVatAmount()), fontNormal);

        // Hàng: TỔNG THANH TOÁN (in đậm)
        addTotalRow(table, "TỔNG THANH TOÁN:",
                formatMoney(data.getFinalAmount()), fontBigTotal);

        doc.add(table);
    }

    private void addTotalRow(PdfPTable table,
                             String label,
                             String value,
                             Font font) {

        PdfPCell c1 = new PdfPCell(new Phrase(label, font));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c1.setPadding(2);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, font));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(2);
        table.addCell(c2);
    }

    // =====================================================================
    // HÀM PHỤ: VOUCHER + LOYALTY
    // =====================================================================

    /**
     * Hiển thị thông tin voucher + điểm loyalty nếu có.
     */
    private void addVoucherAndLoyalty(Document doc,
                                      InvoiceExportData data,
                                      Font fontNormal) throws DocumentException {

        boolean hasVoucher = data.getVoucherCode() != null && !data.getVoucherCode().isBlank();
        boolean hasPoint = data.getLoyaltyEarnedPoint() != null && data.getLoyaltyEarnedPoint() > 0;

        if (!hasVoucher && !hasPoint) {
            return;
        }

        if (hasVoucher) {
            Paragraph pVoucher = new Paragraph(
                    "Voucher áp dụng: " + data.getVoucherCode(), fontNormal);
            pVoucher.setAlignment(Element.ALIGN_LEFT);
            doc.add(pVoucher);
        }

        if (hasPoint) {
            Paragraph pPoint = new Paragraph(
                    "Điểm tích lũy nhận được: " + data.getLoyaltyEarnedPoint(), fontNormal);
            pPoint.setAlignment(Element.ALIGN_LEFT);
            doc.add(pPoint);
        }

        Paragraph sep = new Paragraph(" ", fontNormal);
        sep.setSpacingAfter(5);
        doc.add(sep);
    }

    // =====================================================================
    // HÀM PHỤ: FOOTER
    // =====================================================================

    /**
     * Footer đơn giản:
     *  - Ngày in
     *  - Câu cảm ơn khách hàng
     */
    private void addFooter(Document doc,
                           InvoiceExportData data,
                           Font fontNormal) throws DocumentException {

        String printedDate = java.time.LocalDate.now().format(DATE_FMT);
        Paragraph pDate = new Paragraph("Ngày in: " + printedDate, fontNormal);
        pDate.setAlignment(Element.ALIGN_RIGHT);
        pDate.setSpacingBefore(10);
        doc.add(pDate);

        Paragraph pThanks = new Paragraph(
                "Cảm ơn quý khách. Hẹn gặp lại!", fontNormal);
        pThanks.setAlignment(Element.ALIGN_CENTER);
        pThanks.setSpacingBefore(5);
        doc.add(pThanks);
    }

    // =====================================================================
    // HÀM TIỆN ÍCH
    // =====================================================================

    /** Tránh NullPointer cho String */
    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /** Format tiền kiểu "120,000 đ" từ BigDecimal */
    private String formatMoney(BigDecimal value) {
        if (value == null) return "0 đ";
        return String.format("%,.0f đ", value.doubleValue());
    }
}
