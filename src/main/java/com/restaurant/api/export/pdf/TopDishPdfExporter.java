package com.restaurant.api.export.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.restaurant.api.dto.report.TopDishReportItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * TopDishPdfExporter
 * =====================================================================
 * Xuất PDF cho Báo cáo TOP MÓN BÁN CHẠY.
 *
 * Cấu trúc PDF:
 *  - Tiêu đề
 *  - Khoảng ngày lọc
 *  - Bảng chi tiết:
 *      + Món ăn
 *      + Số lượng bán
 *      + Doanh thu tạo ra
 *
 * Áp dụng STYLE A từ BasePdfExporter:
 *  - Header xám
 *  - Border mảnh
 *  - Font Unicode
 *  - Layout chuyên nghiệp, đồng nhất với Excel
 * =====================================================================
 */
@Component
@RequiredArgsConstructor
public class TopDishPdfExporter extends BasePdfExporter {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Hàm xuất PDF Top món.
     */
    public byte[] export(List<TopDishReportItem> items,
                         LocalDate from, LocalDate to) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = createDocument();
        createWriter(doc, baos);

        doc.open();

        // ==========================================================
        // 1. TIÊU ĐỀ
        // ==========================================================
        doc.add(title("BÁO CÁO TOP MÓN BÁN CHẠY"));

        // Hiển thị khoảng ngày lọc
        doc.add(description(buildRangeText(from, to)));

        // ==========================================================
        // 2. TÍNH TỔNG (Tổng doanh thu + tổng số lượng)
        // ==========================================================
        long totalQty = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        if (items != null) {
            for (TopDishReportItem i : items) {
                totalQty += i.getTotalQuantity() != null ? i.getTotalQuantity() : 0;
                totalRevenue = totalRevenue.add(
                        i.getTotalRevenue() != null ? i.getTotalRevenue() : BigDecimal.ZERO
                );
            }
        }

        Paragraph summary = new Paragraph("", font(11, false));
        summary.setSpacingAfter(10);
        summary.add("Tổng số lượng bán: " + totalQty + "\n");
        summary.add("Tổng doanh thu: " + money(totalRevenue) + " đ");
        doc.add(summary);

        // ==========================================================
        // 3. TẠO BẢNG
        // ==========================================================
        PdfPTable table = createTable(5, 2, 3);

        // Header
        table.addCell(headerCell("Món ăn"));
        table.addCell(headerCell("Số lượng bán"));
        table.addCell(headerCell("Doanh thu"));

        // Body rows
        if (items != null) {
            for (TopDishReportItem item : items) {

                // Cột 1 – Tên món
                table.addCell(bodyCell(item.getDishName()));

                // Cột 2 – Số lượng (right align)
                table.addCell(numberCell(item.getTotalQuantity() != null
                        ? String.valueOf(item.getTotalQuantity())
                        : "0"
                ));

                // Cột 3 – Doanh thu
                table.addCell(numberCell(
                        money(item.getTotalRevenue()) + " đ"
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
        // KẾT THÚC PDF
        // ==========================================================
        return toByteArray(doc, baos);
    }

    /**
     * Tạo text mô tả khoảng ngày.
     */
    private String buildRangeText(LocalDate from, LocalDate to) {
        if (from == null && to == null)
            return "Khoảng thời gian: Toàn bộ dữ liệu";
        String f = from != null ? from.format(DATE_FMT) : "...";
        String t = to != null ? to.format(DATE_FMT) : "...";
        return "Khoảng thời gian: Từ " + f + " đến " + t;
    }
}
