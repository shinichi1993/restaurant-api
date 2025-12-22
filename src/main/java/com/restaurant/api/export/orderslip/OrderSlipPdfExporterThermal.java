package com.restaurant.api.export.orderslip;

import com.lowagie.text.*;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.restaurant.api.dto.orderslip.OrderSlipExportData;
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
 * OrderSlipPdfExporterThermal
 * =====================================================================
 * Phiếu gọi món – khổ giấy nhiệt 80mm
 * Dùng cho:
 *  - In ngay sau khi tạo order
 *  - Khách cầm / quầy giữ
 *
 * KHÔNG phải hóa đơn thanh toán.
 * =====================================================================
 */
@Component
public class OrderSlipPdfExporterThermal {

    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] export(OrderSlipExportData data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // 80mm ~ 226pt
            Rectangle page = new Rectangle(226f, 5000f);
            Document doc = new Document(page, 10, 10, 10, 10);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ================= FONT =================
            String fontPath = "fonts/arial.ttf";
            Font titleFont = FontFactory.getFont(
                    fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 11, Font.BOLD);
            Font storeFont = FontFactory.getFont(
                    fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10, Font.BOLD);
            Font normalFont = FontFactory.getFont(
                    fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 8);
            Font boldFont = FontFactory.getFont(
                    fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 8, Font.BOLD);

            // ========================================================
            // QR CODE – DÙNG ORDER ID (InvoiceExportData hiện chưa có orderCode)
            // ========================================================
            if (data.getOrderCode() != null) {

                String qrText = "ORDER:" + data.getOrderCode();

                Image qr = com.restaurant.api.util.QrCodeUtil.generateQrImage(qrText, 80);
                qr.setAlignment(Image.ALIGN_CENTER);
                qr.setSpacingAfter(4);
                doc.add(qr);

                Paragraph pOrder = new Paragraph("Order: " + data.getOrderCode(), normalFont);
                pOrder.setAlignment(Element.ALIGN_CENTER);
                pOrder.setSpacingAfter(4);
                doc.add(pOrder);
            }

            // ================= HEADER =================
            Paragraph store = new Paragraph(
                    safe(data.getRestaurantName()), storeFont);
            store.setAlignment(Element.ALIGN_CENTER);
            doc.add(store);

            addSeparatorLine(doc, normalFont);

            Paragraph title = new Paragraph("PHIẾU GỌI MÓN", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            doc.add(title);

            // ================= ORDER INFO =================
            //addOneLine(doc, "Order: " + safe(data.getOrderCode()), normalFont);

            if (data.getTableName() != null) {
                addOneLine(doc,
                        "Bàn: " + data.getTableName(),
                        normalFont);
            }

            if (data.getCreatedAt() != null) {
                addOneLine(doc,
                        "Giờ: " + data.getCreatedAt().format(DATE_TIME_FMT),
                        normalFont);
            }

            //addSeparatorLine(doc, normalFont);

            // ================= ITEMS =================
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100f);
            table.setWidths(new float[]{5f, 1.2f, 2.2f, 2.4f});

            addHeader(table, "Món", boldFont);
            addHeader(table, "SL", boldFont);
            addHeader(table, "Giá", boldFont);
            addHeader(table, "Tiền", boldFont);

            if (data.getItems() != null) {
                for (OrderSlipExportData.Item item : data.getItems()) {
                    addCell(table, safe(item.getDishName()), normalFont, Element.ALIGN_LEFT);
                    addCell(table,
                            String.valueOf(item.getQuantity()),
                            normalFont, Element.ALIGN_RIGHT);
                    addCell(table,
                            money(item.getDishPrice()),
                            normalFont, Element.ALIGN_RIGHT);
                    addCell(table,
                            money(item.getSubtotal()),
                            normalFont, Element.ALIGN_RIGHT);
                }
            }

            doc.add(table);

            // ================= NOTE =================
            if (data.getNote() != null && !data.getNote().isBlank()) {
                addSeparatorLine(doc, normalFont);
                Paragraph note = new Paragraph(
                        "Ghi chú: " + data.getNote(), normalFont);
                note.setAlignment(Element.ALIGN_LEFT);
                doc.add(note);
            }

            //addSeparatorLine(doc, normalFont);

            Paragraph footer = new Paragraph("Vui lòng mang phiếu khi thanh toán", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(4);
            doc.add(footer);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất Order Slip Thermal", e);
        }
    }

    // ================= HELPER =================

    private void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(230, 230, 230));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(2f);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(2f);
        table.addCell(cell);
    }

    private void addOneLine(Document doc, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_LEFT);
        p.setSpacingAfter(2);
        doc.add(p);
    }

    private void addSeparatorLine(Document doc, Font font) throws DocumentException {
        Paragraph line = new Paragraph("----------------------------------------", font);
        line.setAlignment(Element.ALIGN_CENTER);
        line.setSpacingBefore(3);
        line.setSpacingAfter(3);
        doc.add(line);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String money(BigDecimal v) {
        if (v == null) return "0 đ";
        return String.format("%,.0f đ", v.doubleValue());
    }
}
