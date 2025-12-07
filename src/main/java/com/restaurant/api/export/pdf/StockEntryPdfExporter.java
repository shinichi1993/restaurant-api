package com.restaurant.api.export.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.restaurant.api.dto.report.StockEntryReportItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * StockEntryPdfExporter
 * =====================================================================
 * Xuất PDF cho Báo cáo NHẬP KHO NGUYÊN LIỆU.
 *
 * Cấu trúc file PDF:
 *  1) Tiêu đề
 *  2) Khoảng thời gian lọc
 *  3) Tổng số lượng nhập
 *  4) Bảng nhập kho (Nguyên liệu – Đơn vị – Nhập kho)
 *  5) Footer ngày in
 *
 * Áp dụng STYLE A:
 *  - Header màu xám
 *  - Border mảnh, đẹp
 *  - Font Unicode từ BasePdfExporter (không lỗi tiếng Việt)
 * =====================================================================
 */
@Component
@RequiredArgsConstructor
public class StockEntryPdfExporter extends BasePdfExporter {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Hàm xuất PDF nhập kho nguyên liệu.
     */
    public byte[] export(List<StockEntryReportItem> items,
                         LocalDate from, LocalDate to) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = createDocument();
        createWriter(doc, baos);

        doc.open();

        // ==========================================================
        // 1. TIÊU ĐỀ
        // ==========================================================
        doc.add(title("BÁO CÁO NHẬP KHO NGUYÊN LIỆU"));
        doc.add(description(buildRangeText(from, to)));

        // ==========================================================
        // 2. TÍNH TỔNG NHẬP KHO
        // ==========================================================
        BigDecimal totalImported = BigDecimal.ZERO;

        if (items != null) {
            for (StockEntryReportItem i : items) {
                totalImported = totalImported.add(
                        i.getTotalImportedAmount() != null ? i.getTotalImportedAmount() : BigDecimal.ZERO
                );
            }
        }

        Paragraph summary = new Paragraph("", font(11, false));
        summary.setSpacingAfter(10);
        summary.add("Tổng số lượng nhập: " + money(totalImported) + "\n");
        doc.add(summary);

        // ==========================================================
        // 3. TẠO BẢNG DỮ LIỆU
        // ==========================================================
        PdfPTable table = createTable(5, 2, 3);

        // Header
        table.addCell(headerCell("Nguyên liệu"));
        table.addCell(headerCell("Đơn vị"));
        table.addCell(headerCell("Nhập kho"));

        // Body
        if (items != null) {
            for (StockEntryReportItem item : items) {

                // Cột 1
                table.addCell(bodyCell(item.getIngredientName()));

                // Cột 2
                table.addCell(bodyCell(item.getUnit()));

                // Cột 3 — Số lượng nhập (căn phải)
                table.addCell(numberCell(
                        money(item.getTotalImportedAmount())
                ));
            }
        }

        doc.add(table);

        // ==========================================================
        // 4. FOOTER – NGÀY IN
        // ==========================================================
        Paragraph footer = new Paragraph(
                "Ngày in: " + LocalDate.now().format(DATE_FMT),
                font(9, false)
        );
        footer.setAlignment(Element.ALIGN_RIGHT);
        footer.setSpacingBefore(10);
        doc.add(footer);

        // ==========================================================
        // 5. KẾT THÚC – TRẢ FILE PDF
        // ==========================================================
        return toByteArray(doc, baos);
    }


    /**
     * Tạo text mô tả khoảng thời gian lọc.
     */
    private String buildRangeText(LocalDate from, LocalDate to) {
        if (from == null && to == null)
            return "Khoảng thời gian: Toàn bộ dữ liệu";

        String f = from != null ? from.format(DATE_FMT) : "...";
        String t = to != null ? to.format(DATE_FMT) : "...";

        return "Khoảng thời gian: Từ " + f + " đến " + t;
    }
}
