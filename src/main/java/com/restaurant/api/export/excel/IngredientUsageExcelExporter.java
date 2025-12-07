package com.restaurant.api.export.excel;

import com.restaurant.api.dto.report.IngredientUsageReportItem;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * IngredientUsageExcelExporter
 * =====================================================================
 * Xuất Excel cho BÁO CÁO NGUYÊN LIỆU TIÊU HAO.
 *
 *  - STYLE A: Header xám, border mảnh, freeze header, auto-size
 *  - Tách khỏi ReportService để đảm bảo clean architecture
 * =====================================================================
 */
@Component
public class IngredientUsageExcelExporter extends BaseExcelExporter {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] export(List<IngredientUsageReportItem> items,
                         LocalDate from, LocalDate to) {

        Workbook wb = createWorkbook();
        Sheet sheet = wb.createSheet("TieuHao");

        // Style dùng chung
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
        titleCell.setCellValue("BÁO CÁO NGUYÊN LIỆU TIÊU HAO");
        titleCell.setCellStyle(titleStyle);
        mergeCells(sheet, 0, 0, 0, 2);

        // ==========================================================
        // 2. KHOẢNG THỜI GIAN
        // ==========================================================
        Row rangeRow = sheet.createRow(rowIndex++);
        Cell rangeCell = rangeRow.createCell(0);
        rangeCell.setCellValue(buildRangeText(from, to));
        rangeCell.setCellStyle(bodyStyle);
        mergeCells(sheet, 1, 1, 0, 2);

        rowIndex++;

        // ==========================================================
        // 3. HEADER
        // ==========================================================
        String[] headers = {"Nguyên liệu", "Đơn vị", "Tổng tiêu hao"};
        Row headerRow = sheet.createRow(rowIndex++);

        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        int headerRowIndex = rowIndex - 1;

        // ==========================================================
        // 4. DATA ROWS
        // ==========================================================
        BigDecimal totalUsed = BigDecimal.ZERO;

        if (items != null) {
            for (IngredientUsageReportItem item : items) {
                Row row = sheet.createRow(rowIndex++);

                // Nguyên liệu
                Cell c0 = row.createCell(0);
                c0.setCellValue(item.getIngredientName());
                c0.setCellStyle(bodyStyle);

                // Đơn vị
                Cell c1 = row.createCell(1);
                c1.setCellValue(item.getUnit());
                c1.setCellStyle(bodyStyle);

                // Tổng tiêu hao
                BigDecimal used = item.getTotalUsed() != null
                        ? item.getTotalUsed()
                        : BigDecimal.ZERO;
                totalUsed = totalUsed.add(used);

                createNumberCell(row, 2, used, numberStyle);
            }
        }

        // ==========================================================
        // 5. Footer – TỔNG CỘNG
        // ==========================================================
        Row footer = sheet.createRow(rowIndex++);

        Cell l = footer.createCell(0);
        l.setCellValue("TỔNG CỘNG");
        l.setCellStyle(footerStyle);

        footer.createCell(1).setCellStyle(footerStyle);
        createNumberCell(footer, 2, totalUsed, footerStyle);

        // ==========================================================
        // 6. AUTO-SIZE + FREEZE
        // ==========================================================
        autoSizeColumns(sheet, headers.length);
        sheet.createFreezePane(0, headerRowIndex + 1);

        return writeToByteArray(wb);
    }

    private String buildRangeText(LocalDate from, LocalDate to) {
        if (from == null && to == null) return "Khoảng thời gian: Toàn bộ dữ liệu";
        String f = from != null ? from.format(DATE_FMT) : "...";
        String t = to != null ? to.format(DATE_FMT) : "...";
        return "Khoảng thời gian: Từ " + f + " đến " + t;
    }

    private void createNumberCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellStyle(style);
        c.setCellValue(value != null ? value.doubleValue() : 0);
    }
}
