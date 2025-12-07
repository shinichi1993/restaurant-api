package com.restaurant.api.export.excel;

import com.restaurant.api.dto.report.TopDishReportItem;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * TopDishExcelExporter
 * =====================================================================
 * Class chuyên xuất Excel cho BÁO CÁO TOP MÓN BÁN CHẠY.
 *
 *  - Áp dụng STYLE A (header xám nhạt, border mảnh, layout chuyên nghiệp)
 *  - Tách riêng khỏi ReportService theo Rule clean architecture
 *
 *  Quy trình xuất:
 *    1) Tạo Workbook + Sheet
 *    2) Ghi tiêu đề (merge cột)
 *    3) Ghi thông tin khoảng ngày lọc
 *    4) Ghi header bảng
 *    5) Ghi từng dòng món ăn
 *    6) Ghi dòng tổng cộng doanh thu
 *    7) Auto-size + Freeze header
 * =====================================================================
 */
@Component
public class TopDishExcelExporter extends BaseExcelExporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Hàm export danh sách top món.
     * ----------------------------------------------------------------
     * @param items : danh sách TopDishReportItem
     * @param from  : ngày bắt đầu filter
     * @param to    : ngày kết thúc filter
     * @return      : byte[] nội dung Excel
     */
    public byte[] export(List<TopDishReportItem> items, LocalDate from, LocalDate to) {

        Workbook wb = createWorkbook();
        Sheet sheet = wb.createSheet("TopMon");

        // Tạo style dùng chung
        CellStyle titleStyle = createTitleStyle(wb);
        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle bodyStyle = createBodyStyle(wb);
        CellStyle numberStyle = createNumberStyle(wb);
        CellStyle footerStyle = createFooterStyle(wb);

        int rowIndex = 0;

        // ==========================================================
        // 1. TIÊU ĐỀ
        // ==========================================================
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BÁO CÁO TOP MÓN BÁN CHẠY");
        titleCell.setCellStyle(titleStyle);

        // Merge 3 cột: Món ăn – Số lượng – Doanh thu
        mergeCells(sheet, 0, 0, 0, 2);

        // ==========================================================
        // 2. KHOẢNG THỜI GIAN
        // ==========================================================
        Row rangeRow = sheet.createRow(rowIndex++);
        Cell rangeCell = rangeRow.createCell(0);
        rangeCell.setCellStyle(bodyStyle);
        rangeCell.setCellValue(buildRangeText(from, to));
        mergeCells(sheet, 1, 1, 0, 2);

        rowIndex++; // dòng trống

        // ==========================================================
        // 3. HEADER BẢNG
        // ==========================================================
        String[] headers = {"Món ăn", "Số lượng bán", "Doanh thu"};

        Row headerRow = sheet.createRow(rowIndex++);
        for (int col = 0; col < headers.length; col++) {
            Cell c = headerRow.createCell(col);
            c.setCellValue(headers[col]);
            c.setCellStyle(headerStyle);
        }

        int headerRowIndex = rowIndex - 1;

        // ==========================================================
        // 4. DỮ LIỆU BẢNG
        // ==========================================================
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalQuantity = 0;

        if (items != null) {
            for (TopDishReportItem item : items) {

                Row row = sheet.createRow(rowIndex++);

                // Cột 0 – Tên món
                Cell nameCell = row.createCell(0);
                nameCell.setCellStyle(bodyStyle);
                nameCell.setCellValue(item.getDishName() != null ? item.getDishName() : "");

                // Cột 1 – Số lượng
                BigDecimal qty = BigDecimal.valueOf(item.getTotalQuantity() != null ? item.getTotalQuantity() : 0);
                createNumberCell(row, 1, qty, numberStyle);

                // Cột 2 – Doanh thu
                BigDecimal rev = item.getTotalRevenue() != null ? item.getTotalRevenue() : BigDecimal.ZERO;
                createNumberCell(row, 2, rev, numberStyle);

                totalRevenue = totalRevenue.add(rev);
                totalQuantity += qty.longValue();
            }
        }

        // ==========================================================
        // 5. DÒNG TỔNG CỘNG
        // ==========================================================
        Row footerRow = sheet.createRow(rowIndex++);

        // Label "TỔNG CỘNG"
        Cell labelCell = footerRow.createCell(0);
        labelCell.setCellValue("TỔNG CỘNG");
        labelCell.setCellStyle(footerStyle);

        // Tổng số lượng
        createNumberCell(footerRow, 1, BigDecimal.valueOf(totalQuantity), footerStyle);

        // Tổng doanh thu
        createNumberCell(footerRow, 2, totalRevenue, footerStyle);

        // ==========================================================
        // 6. AUTO-SIZE + FREEZE HEADER
        // ==========================================================
        autoSizeColumns(sheet, headers.length);

        sheet.createFreezePane(0, headerRowIndex + 1);

        // ==========================================================
        // 7. Xuất file
        // ==========================================================
        return writeToByteArray(wb);
    }

    /**
     * Gộp text mô tả khoảng thời gian
     */
    private String buildRangeText(LocalDate from, LocalDate to) {
        if (from == null && to == null) return "Khoảng thời gian: Toàn bộ dữ liệu";
        String f = from != null ? from.format(DATE_FMT) : "...";
        String t = to != null ? to.format(DATE_FMT) : "...";
        return "Khoảng thời gian: Từ " + f + " đến " + t;
    }

    // --------------------------------------------------------------
    // Hàm tiện ích tạo cell số (BigDecimal → số)
    // --------------------------------------------------------------
    private void createNumberCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        cell.setCellValue(value != null ? value.doubleValue() : 0);
    }
}
