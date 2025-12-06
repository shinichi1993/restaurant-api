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
 * InvoicePdfExporterThermal
 * =====================================================================
 * Export hóa đơn ra PDF khổ giấy nhiệt 80mm.
 *
 * Đặc điểm:
 *  - Khổ giấy hẹp (80mm), phù hợp máy in bill nhiệt.
 *  - Layout đơn giản, tập trung thông tin chính:
 *      + Cửa hàng (KHÔNG in MST ở Thermal)
 *      + Thông tin hóa đơn
 *      + Danh sách món
 *      + Tổng tiền hàng, giảm giá, VAT, tổng thanh toán
 *      + Voucher + điểm loyalty (nếu có)
 *      + Footer cảm ơn khách
 *
 * Lưu ý:
 *  - KHÔNG tính toán lại tiền (VAT, discount...). Mọi logic đã được
 *    xử lý ở InvoiceService.buildInvoiceExportData().
 *  - Class này chỉ chịu trách nhiệm "vẽ" PDF dạng bill.
 *
 * Font:
 *  - Dùng font Unicode: fonts/arial.ttf
 * =====================================================================
 */
@Component
public class InvoicePdfExporterThermal {

    // Định dạng ngày giờ thanh toán
    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Định dạng ngày in footer
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Hàm public chính để export hóa đơn ra PDF Thermal 80mm.
     * --------------------------------------------------------------
     * @param data dữ liệu hóa đơn đã được chuẩn hóa
     * @return mảng byte[] nội dung file PDF
     */
    public byte[] export(InvoiceExportData data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Khổ giấy nhiệt 80mm:
            //  - 1 inch ~ 25.4mm, 1 point ~ 1/72 inch ~ 0.352mm
            //  - 80mm ~ 226.7 point → dùng 226f là đủ.
            // Chiều cao set dư (10f), nếu nội dung nhiều sẽ tự xuống trang.
            Rectangle thermalPage = new Rectangle(226f, 5000f);

            Document doc = new Document(thermalPage, 10, 10, 10, 10);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ===================== FONT UNICODE ======================
            String fontPath = "fonts/arial.ttf";
            Font fontTitle = FontFactory.getFont(fontPath,
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 11, Font.BOLD);
            Font fontStoreName = FontFactory.getFont(fontPath,
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10, Font.BOLD);
            Font fontNormal = FontFactory.getFont(fontPath,
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 8);
            Font fontBold = FontFactory.getFont(fontPath,
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 8, Font.BOLD);
            Font fontBigTotal = FontFactory.getFont(fontPath,
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 9, Font.BOLD);

            // ========================================================
            // 1. HEADER CỬA HÀNG (KHÔNG IN MST)
            // ========================================================
            addStoreHeader(doc, data, fontStoreName, fontNormal);
            addSeparatorLine(doc, fontNormal);

            // ========================================================
            // 2. TIÊU ĐỀ HÓA ĐƠN
            // ========================================================
            Paragraph title = new Paragraph("HÓA ĐƠN THANH TOÁN", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(2);
            title.setSpacingAfter(5);
            doc.add(title);

            // ========================================================
            // 3. THÔNG TIN HÓA ĐƠN
            // ========================================================
            addInvoiceInfo(doc, data, fontNormal);
            addSeparator(doc, fontNormal);

            // ========================================================
            // 4. BẢNG MÓN ĂN
            // ========================================================
            addItemsTable(doc, data, fontNormal, fontBold);

            // ========================================================
            // 5. TỔNG KẾT TIỀN
            // ========================================================
            addTotalsBlock(doc, data, fontNormal, fontBigTotal);

            // ========================================================
            // 6. VOUCHER + LOYALTY (NẾU CÓ)
            // ========================================================
            addVoucherAndLoyalty(doc, data, fontNormal);
            addBoldSeparator(doc, fontBold);

            // ========================================================
            // 7. FOOTER CẢM ƠN
            // ========================================================
            addFooter(doc, fontNormal);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất PDF hóa đơn Thermal 80mm", e);
        }
    }

    // =====================================================================
    // HÀM PHỤ: HEADER CỬA HÀNG
    // =====================================================================

    /**
     * Header Thermal:
     *  - Tên cửa hàng (đậm, canh giữa)
     *  - Địa chỉ
     *  - Điện thoại
     *  - KHÔNG hiển thị MST (taxId) trên bill nhiệt
     */
    private void addStoreHeader(Document doc,
                                InvoiceExportData data,
                                Font fontStoreName,
                                Font fontNormal) throws DocumentException {

        String storeName = safe(data.getRestaurantName());
        if (storeName.isEmpty()) {
            storeName = "TÊN CỬA HÀNG";
        }
        Paragraph pName = new Paragraph(storeName, fontStoreName);
        pName.setAlignment(Element.ALIGN_CENTER);
        pName.setSpacingAfter(1);
        doc.add(pName);

        String address = safe(data.getRestaurantAddress());
        if (!address.isEmpty()) {
            Paragraph pAddress = new Paragraph(address, fontNormal);
            pAddress.setAlignment(Element.ALIGN_CENTER);
            pAddress.setSpacingAfter(1);
            doc.add(pAddress);
        }

        String phone = safe(data.getRestaurantPhone());
        if (!phone.isEmpty()) {
            Paragraph pPhone = new Paragraph("ĐT: " + phone, fontNormal);
            pPhone.setAlignment(Element.ALIGN_CENTER);
            pPhone.setSpacingAfter(3);
            doc.add(pPhone);
        } else {
            Paragraph sep = new Paragraph(" ", fontNormal);
            sep.setSpacingAfter(3);
            doc.add(sep);
        }

        // KHÔNG in MST trong Thermal → bỏ qua restaurantTaxId
    }

    // =====================================================================
    // HÀM PHỤ: THÔNG TIN HÓA ĐƠN
    // =====================================================================

    /**
     * Thông tin hóa đơn dạng ngắn gọn, phù hợp bill:
     *  - HĐ: <invoiceId>   Bàn/Order: <orderId>
     *  - Ngày: dd/MM/yyyy HH:mm
     *  - TT: CASH / BANK / ...
     */
    private void addInvoiceInfo(Document doc,
                                InvoiceExportData data,
                                Font fontNormal) throws DocumentException {

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100f);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // Dòng 1: HĐ: x  -  Order: y
        String line1 = "HĐ: " + data.getInvoiceId();
        if (data.getOrderId() != null) {
            line1 += "   Order: " + data.getOrderId();
        }
        addOneLine(table, line1, fontNormal);

        // Dòng 2: Ngày: ...
        String paidAtText = "-";
        if (data.getPaidAt() != null) {
            paidAtText = data.getPaidAt().format(DATE_TIME_FMT);
        }
        addOneLine(table, "Ngày: " + paidAtText, fontNormal);

        // Dòng 3: TT: phương thức thanh toán
        String method = safe(data.getPaymentMethod());
        addOneLine(table, "TT: " + (method.isEmpty() ? "-" : method), fontNormal);

        table.setSpacingAfter(4);
        doc.add(table);
    }

    private void addOneLine(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(1f);
        table.addCell(cell);
    }

    // =====================================================================
    // HÀM PHỤ: BẢNG MÓN ĂN
    // =====================================================================

    /**
     * Bảng món ăn (4 cột):
     *  - Tên | SL | Giá | Tiền
     * Layout gọn, chữ nhỏ cho khổ 80mm.
     */
    private void addItemsTable(Document doc,
                               InvoiceExportData data,
                               Font fontNormal,
                               Font fontHeader) throws DocumentException {

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{5f, 1.2f, 2.2f, 2.4f});
        table.setSpacingAfter(4);

        // Header
        addHeaderCell(table, "Tên", fontHeader);
        addHeaderCell(table, "SL", fontHeader);
        addHeaderCell(table, "Giá", fontHeader);
        addHeaderCell(table, "Tiền", fontHeader);

        // Dữ liệu
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
        cell.setPadding(2f);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table,
                             String text,
                             Font font,
                             int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(2f);
        table.addCell(cell);
    }

    // =====================================================================
    // HÀM PHỤ: TỔNG TIỀN
    // =====================================================================

    /**
     * Khối tổng tiền (canh phải):
     *  - Tổng tiền hàng
     *  - Giảm giá
     *  - Thành tiền trước VAT
     *  - Thuế VAT (x%)
     *  - TỔNG THANH TOÁN (in đậm)
     */
    private void addTotalsBlock(Document doc,
                                InvoiceExportData data,
                                Font fontNormal,
                                Font fontBigTotal) throws DocumentException {

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{5f, 3f});
        table.setSpacingAfter(4);

        addTotalRow(table, "Tổng tiền hàng",
                formatMoney(data.getTotalBeforeDiscount()), fontNormal);

        addTotalRow(table, "Giảm giá",
                formatMoney(data.getDiscountAmount()), fontNormal);

        addTotalRow(table, "Trước VAT",
                formatMoney(data.getAmountBeforeVat()), fontNormal);

        BigDecimal vatPercent = data.getVatPercent() != null ? data.getVatPercent() : BigDecimal.ZERO;
        String vatLabel = "VAT " + vatPercent.stripTrailingZeros().toPlainString() + "%";
        addTotalRow(table, vatLabel,
                formatMoney(data.getVatAmount()), fontNormal);

        addTotalRow(table, "TỔNG THANH TOÁN",
                formatMoney(data.getFinalAmount()), fontBigTotal);

        doc.add(table);
    }

    private void addTotalRow(PdfPTable table,
                             String label,
                             String value,
                             Font font) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, font));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
        c1.setPadding(1.5f);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, font));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(1.5f);
        table.addCell(c2);
    }

    // =====================================================================
    // HÀM PHỤ: VOUCHER + LOYALTY
    // =====================================================================

    private void addVoucherAndLoyalty(Document doc,
                                      InvoiceExportData data,
                                      Font fontNormal) throws DocumentException {

        boolean hasVoucher = data.getVoucherCode() != null && !data.getVoucherCode().isBlank();
        boolean hasPoint = data.getLoyaltyEarnedPoint() != null && data.getLoyaltyEarnedPoint() > 0;

        if (!hasVoucher && !hasPoint) {
            return;
        }

        /*
        if (hasVoucher) {
            Paragraph pVoucher = new Paragraph("Voucher: " + data.getVoucherCode(), fontNormal);
            pVoucher.setAlignment(Element.ALIGN_LEFT);
            doc.add(pVoucher);
        }
        */

        if (hasPoint) {
            Paragraph pPoint = new Paragraph(
                    "Điểm tích lũy: " + data.getLoyaltyEarnedPoint(), fontNormal);
            pPoint.setAlignment(Element.ALIGN_LEFT);
            doc.add(pPoint);
        }

        Paragraph sep = new Paragraph(" ", fontNormal);
        sep.setSpacingAfter(3);
        doc.add(sep);
    }

    // =====================================================================
    // HÀM PHỤ: FOOTER
    // =====================================================================

    private void addFooter(Document doc,
                           Font fontNormal) throws DocumentException {

        String printedDate = java.time.LocalDate.now().format(DATE_FMT);
        Paragraph pDate = new Paragraph("In lúc: " + printedDate, fontNormal);
        pDate.setAlignment(Element.ALIGN_CENTER);
        pDate.setSpacingBefore(4);
        doc.add(pDate);

        Paragraph pThanks = new Paragraph("Cảm ơn quý khách!", fontNormal);
        pThanks.setAlignment(Element.ALIGN_CENTER);
        pThanks.setSpacingBefore(2);
        doc.add(pThanks);
    }

    // =====================================================================
    // HÀM TIỆN ÍCH
    // =====================================================================

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0 đ";
        return String.format("%,.0f đ", value.doubleValue());
    }
    private void addSeparator(Document doc, Font font) throws DocumentException {
        Paragraph line = new Paragraph("", font);
        line.setAlignment(Element.ALIGN_CENTER);
        line.setSpacingBefore(4);
        line.setSpacingAfter(4);
        doc.add(line);
    }
    private void addSeparatorLine(Document doc, Font font) throws DocumentException {
        Paragraph line = new Paragraph("----------------------------------------", font);
        line.setAlignment(Element.ALIGN_CENTER);
        line.setSpacingBefore(4);
        line.setSpacingAfter(4);
        doc.add(line);
    }
    private void addBoldSeparator(Document doc, Font fontBold) throws DocumentException {
        Paragraph line = new Paragraph("========================================", fontBold);
        line.setAlignment(Element.ALIGN_CENTER);
        line.setSpacingBefore(6);
        line.setSpacingAfter(6);
        doc.add(line);
    }
    private void add2ColRow(Document doc, String left, String right, Font font) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{5f, 3f});

        PdfPCell cLeft = new PdfPCell(new Phrase(left, font));
        cLeft.setBorder(Rectangle.NO_BORDER);
        cLeft.setHorizontalAlignment(Element.ALIGN_LEFT);
        cLeft.setPadding(2f);

        PdfPCell cRight = new PdfPCell(new Phrase(right, font));
        cRight.setBorder(Rectangle.NO_BORDER);
        cRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cRight.setPadding(2f);

        table.addCell(cLeft);
        table.addCell(cRight);

        doc.add(table);
    }
}
