package com.restaurant.api.export.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.restaurant.api.dto.report.RevenueByDayItem;
import com.restaurant.api.dto.report.RevenueReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * RevenuePdfExporter
 * =====================================================================
 * Xuất PDF cho Báo cáo Doanh thu.
 *
 * Dùng BasePdfExporter để đạt:
 *  - Font Unicode chuẩn
 *  - Style A: header xám, body border, số liệu căn phải
 *  - Layout rõ ràng, chuyên nghiệp
 * =====================================================================
 */
@Component
@RequiredArgsConstructor
public class RevenuePdfExporter extends BasePdfExporter {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Hàm xuất PDF cho báo cáo doanh thu.
     */
    public byte[] export(RevenueReportResponse report,
                         LocalDate from, LocalDate to) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = createDocument();
        createWriter(doc, baos);

        doc.open();

        // ==========================================================
        // 1. TIÊU ĐỀ
        // ==========================================================
        doc.add(title("BÁO CÁO DOANH THU"));

        // Hiển thị khoảng ngày
        doc.add(description(buildRangeText(from, to)));

        // ==========================================================
        // 2. TỔNG QUAN
        // ==========================================================
        Paragraph summary = new Paragraph("", font(11, false));
        summary.setSpacingAfter(8);

        summary.add("Tổng doanh thu: " + money(report.getTotalRevenue()) + " đ\n");
        summary.add("Tổng số đơn: " + report.getTotalOrders() + "\n");
        summary.add("Doanh thu TB/ngày: " + money(report.getAverageRevenuePerDay()) + " đ");

        doc.add(summary);

        // ==========================================================
        // 3. BẢNG CHI TIẾT
        // ==========================================================
        PdfPTable table = createTable(3, 3, 2); // Width ratio

        // Header
        table.addCell(headerCell("Ngày"));
        table.addCell(headerCell("Doanh thu"));
        table.addCell(headerCell("Số đơn"));

        // Dòng dữ liệu
        if (report.getItems() != null) {
            for (RevenueByDayItem item : report.getItems()) {

                table.addCell(bodyCell(
                        item.getDate() != null ? item.getDate().format(DATE_FMT) : ""
                ));

                table.addCell(numberCell(
                        money(item.getRevenue()) + " đ"
                ));

                table.addCell(numberCell(
                        item.getOrderCount() != null
                                ? item.getOrderCount().toString()
                                : "0"
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
     * Tạo text mô tả khoảng thời gian lọc.
     */
    private String buildRangeText(LocalDate from, LocalDate to) {
        if (from == null && to == null) return "Khoảng thời gian: Toàn bộ dữ liệu";

        String f = from != null ? from.format(DATE_FMT) : "...";
        String t = to != null ? to.format(DATE_FMT) : "...";

        return "Khoảng thời gian: Từ " + f + " đến " + t;
    }
}
