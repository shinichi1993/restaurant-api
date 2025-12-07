package com.restaurant.api.export.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.restaurant.api.dto.report.IngredientUsageReportItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * IngredientUsagePdfExporter
 * =====================================================================
 * Xuất PDF cho Báo cáo NGUYÊN LIỆU TIÊU HAO.
 *
 * Cấu trúc PDF:
 *  - Tiêu đề
 *  - Khoảng ngày
 *  - Bảng dữ liệu (Nguyên liệu – Đơn vị – Tiêu hao)
 *  - Footer ngày in
 *
 * Áp dụng STYLE A:
 *  - Header nền xám
 *  - Border mảnh
 *  - Căn phải cho số liệu
 *  - Font Unicode từ BasePdfExporter
 * =====================================================================
 */
@Component
@RequiredArgsConstructor
public class IngredientUsagePdfExporter extends BasePdfExporter {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Hàm xuất PDF nguyên liệu TIÊU HAO
     */
    public byte[] export(List<IngredientUsageReportItem> items,
                         LocalDate from, LocalDate to) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = createDocument();
        createWriter(doc, baos);

        doc.open();

        // ==========================================================
        // 1. TIÊU ĐỀ
        // ==========================================================
        doc.add(title("BÁO CÁO NGUYÊN LIỆU TIÊU HAO"));

        // Khoảng ngày
        doc.add(description(buildRangeText(from, to)));

        // ==========================================================
        // 2. TÍNH TỔNG
        // ==========================================================
        BigDecimal totalUsed = BigDecimal.ZERO;

        if (items != null) {
            for (IngredientUsageReportItem i : items) {
                totalUsed = totalUsed.add(
                        i.getTotalUsed() != null ? i.getTotalUsed() : BigDecimal.ZERO
                );
            }
        }

        Paragraph summary = new Paragraph("", font(11, false));
        summary.setSpacingAfter(10);
        summary.add("Tổng tiêu hao: " + money(totalUsed) + " " + "\n");
        doc.add(summary);

        // ==========================================================
        // 3. TẠO BẢNG PDF
        // ==========================================================
        PdfPTable table = createTable(5, 2, 3); // Tỷ lệ: rộng – vừa – rộng

        // Header
        table.addCell(headerCell("Nguyên liệu"));
        table.addCell(headerCell("Đơn vị"));
        table.addCell(headerCell("Tiêu hao"));

        // Dữ liệu
        if (items != null) {
            for (IngredientUsageReportItem item : items) {

                // Cột 1 — Nguyên liệu
                table.addCell(bodyCell(item.getIngredientName()));

                // Cột 2 — Đơn vị
                table.addCell(bodyCell(item.getUnit()));

                // Cột 3 — Số lượng tiêu hao (căn phải)
                table.addCell(numberCell(
                        money(item.getTotalUsed())
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
        // KẾT THÚC
        // ==========================================================
        return toByteArray(doc, baos);
    }

    /**
     * Tạo text mô tả khoảng thời gian.
     */
    private String buildRangeText(LocalDate from, LocalDate to) {
        if (from == null && to == null)
            return "Khoảng thời gian: Toàn bộ dữ liệu";

        String f = from != null ? from.format(DATE_FMT) : "...";
        String t = to != null ? to.format(DATE_FMT) : "...";

        return "Khoảng thời gian: Từ " + f + " đến " + t;
    }
}
