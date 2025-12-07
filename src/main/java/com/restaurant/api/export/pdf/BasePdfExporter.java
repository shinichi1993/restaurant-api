package com.restaurant.api.export.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;

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
 * BasePdfExporter
 * =====================================================================
 * Class nền dùng chung cho tất cả PDF Exporter của module Report.
 *
 * Áp dụng STYLE A – Enterprise:
 *  - Font Unicode nhúng hoàn toàn (tránh lỗi tiếng Việt)
 *  - Tiêu đề căn giữa, chữ đậm, cỡ 16
 *  - Header bảng: nền xám (#E8E8E8), chữ đậm
 *  - Body: border mảnh, căn trái
 *  - Số liệu: căn phải
 *  - Footer tổng cộng: chữ đậm
 *
 * Các PdfExporter khác (Revenue, TopDish, IngredientUsage, StockEntry)
 * sẽ kế thừa BasePdfExporter để bảo đảm style thống nhất.
 * =====================================================================
 */
@Component
public class BasePdfExporter {

    // Đường dẫn font Unicode (Arial hoặc Roboto)
    protected static final String FONT_PATH = "fonts/arial.ttf";

    /**
     * Tạo Document A4 chuẩn, margin đẹp.
     */
    protected Document createDocument() {
        return new Document(PageSize.A4, 36, 36, 36, 36);
    }

    /**
     * Tạo writer + lưu vào byte[].
     */
    protected PdfWriter createWriter(Document doc, ByteArrayOutputStream baos) {
        try {
            return PdfWriter.getInstance(doc, baos);
        } catch (Exception e) {
            throw new RuntimeException("Không tạo được PdfWriter", e);
        }
    }

    /**
     * Load font Unicode (bắt buộc để hiển thị đúng tiếng Việt).
     */
    protected Font font(float size, boolean bold) {
        try {
            BaseFont bf = BaseFont.createFont(
                    FONT_PATH,
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );

            Font f = new Font(bf, size);
            if (bold) f.setStyle(Font.BOLD);

            return f;

        } catch (Exception e) {
            throw new RuntimeException("Không load được font Unicode", e);
        }
    }

    /**
     * Tiêu đề lớn căn giữa.
     */
    protected Paragraph title(String text) {
        Paragraph p = new Paragraph(text, font(16, true));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(12);
        return p;
    }

    /**
     * Dòng mô tả filter (ngày tháng...).
     */
    protected Paragraph description(String text) {
        Paragraph p = new Paragraph(text, font(10, false));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(10);
        return p;
    }

    /**
     * Tạo bảng với số cột cố định và full width.
     */
    protected PdfPTable createTable(float... widths) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        return table;
    }

    /**
     * Ô header: nền xám, border, chữ đậm, căn giữa.
     */
    protected PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(10, true)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(new Color(230, 230, 230));
        cell.setPadding(5);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    /**
     * Ô body: border mảnh, căn trái.
     */
    protected PdfPCell bodyCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(10, false)));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    /**
     * Ô body số liệu: border mảnh, căn phải.
     */
    protected PdfPCell numberCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(10, false)));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    /**
     * Footer tổng cộng (chữ đậm).
     */
    protected PdfPCell footerCell(String text, boolean alignRight) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(10, true)));
        cell.setHorizontalAlignment(alignRight ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorderWidth(0.8f);
        return cell;
    }

    /**
     * Chuyển BigDecimal -> format số đẹp.
     */
    protected String money(Number n) {
        if (n == null) return "0";
        return String.format("%,.0f", n.doubleValue());
    }

    /**
     * Kết thúc PDF -> trả về byte[].
     */
    protected byte[] toByteArray(Document doc, ByteArrayOutputStream baos) {
        doc.close();
        return baos.toByteArray();
    }
}
