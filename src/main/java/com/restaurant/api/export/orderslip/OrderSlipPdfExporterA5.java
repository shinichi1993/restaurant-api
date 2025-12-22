package com.restaurant.api.export.orderslip;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.restaurant.api.dto.orderslip.OrderSlipExportData;
import com.restaurant.api.util.QrCodeUtil;
import org.springframework.stereotype.Component;

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
 * OrderSlipPdfExporterA5
 * ------------------------------------------------------------
 * Phiếu gọi món – khổ A5
 */
@Component
public class OrderSlipPdfExporterA5 {

    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] export(OrderSlipExportData data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document doc = new Document(PageSize.A5, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            String fontPath = "fonts/arial.ttf";
            Font titleFont = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 14, Font.BOLD);
            Font normal = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10);
            Font bold = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10, Font.BOLD);

            // ========================================================
            // QR CODE – DÙNG ORDER ID (InvoiceExportData hiện chưa có orderCode)
            // ========================================================
            if (data.getOrderCode() != null) {

                // Nội dung QR: có thể đổi format tùy ý
                String qrText = "ORDER:" + data.getOrderCode();

                Image qr = QrCodeUtil.generateQrImage(qrText, 120);
                qr.setAlignment(Image.ALIGN_CENTER);
                qr.setSpacingAfter(6);
                doc.add(qr);

                Paragraph pOrder = new Paragraph("Order: " + data.getOrderCode(), normal);
                pOrder.setAlignment(Element.ALIGN_CENTER);
                pOrder.setSpacingAfter(8);
                doc.add(pOrder);
            }

            // ===== HEADER =====
            Paragraph store = new Paragraph(data.getRestaurantName(), bold);
            store.setAlignment(Element.ALIGN_CENTER);
            doc.add(store);

            doc.add(new Paragraph("PHIẾU GỌI MÓN", titleFont));
            doc.add(Chunk.NEWLINE);

            // ===== INFO =====
            //doc.add(new Paragraph("Order: " + data.getOrderCode(), normal));
            doc.add(new Paragraph("Bàn: " + data.getTableName(), normal));
            doc.add(new Paragraph("Giờ tạo: " +
                    (data.getCreatedAt() != null ? data.getCreatedAt().format(DATE_TIME_FMT) : "-"), normal));

            doc.add(Chunk.NEWLINE);

            // ===== TABLE =====
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{5, 1.5f, 2f, 2f});

            addHeader(table, "Món", bold);
            addHeader(table, "SL", bold);
            addHeader(table, "Giá", bold);
            addHeader(table, "Tiền", bold);

            for (OrderSlipExportData.Item i : data.getItems()) {
                addCell(table, i.getDishName(), normal);
                addCell(table, String.valueOf(i.getQuantity()), normal);
                addCell(table, money(i.getDishPrice()), normal);
                addCell(table, money(i.getSubtotal()), normal);
            }

            doc.add(table);

            if (data.getNote() != null && !data.getNote().isBlank()) {
                doc.add(Chunk.NEWLINE);
                doc.add(new Paragraph("Ghi chú: " + data.getNote(), normal));
            }

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất Order Slip A5", e);
        }
    }

    private void addHeader(PdfPTable t, String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void addCell(PdfPTable t, String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        t.addCell(c);
    }

    private String money(BigDecimal v) {
        if (v == null) return "0 đ";
        return String.format("%,.0f đ", v.doubleValue());
    }
}
