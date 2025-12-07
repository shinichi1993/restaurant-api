package com.restaurant.api.export.excel;

import com.restaurant.api.dto.report.StockEntryReportItem;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class StockEntryExcelExporter extends BaseExcelExporter {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] export(List<StockEntryReportItem> items,
                         LocalDate from, LocalDate to) {

        Workbook wb = createWorkbook();
        Sheet sheet = wb.createSheet("NhapKho");

        CellStyle titleStyle = createTitleStyle(wb);
        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle bodyStyle = createBodyStyle(wb);
        CellStyle numberStyle = createNumberStyle(wb);
        CellStyle footerStyle = createFooterStyle(wb);

        int rowIndex = 0;

        // ==========================================================
        // TIÊU ĐỀ
        // ==========================================================
        Row title = sheet.createRow(rowIndex++);
        Cell t = title.createCell(0);
        t.setCellValue("BÁO CÁO NHẬP KHO NGUYÊN LIỆU");
        t.setCellStyle(titleStyle);
        mergeCells(sheet, 0, 0, 0, 2);

        // ==========================================================
        // RANGE
        // ==========================================================
        Row range = sheet.createRow(rowIndex++);
        Cell r = range.createCell(0);
        r.setCellValue(buildRangeText(from, to));
        r.setCellStyle(bodyStyle);
        mergeCells(sheet, 1, 1, 0, 2);

        rowIndex++;

        // ==========================================================
        // HEADER
        // ==========================================================
        String[] headers = {"Nguyên liệu", "Đơn vị", "Tổng nhập kho"};
        Row headerRow = sheet.createRow(rowIndex++);

        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        int headerRowIndex = rowIndex - 1;

        // ==========================================================
        // DATA
        // ==========================================================
        BigDecimal totalImported = BigDecimal.ZERO;

        if (items != null) {
            for (StockEntryReportItem item : items) {
                Row row = sheet.createRow(rowIndex++);

                // Nguyên liệu
                Cell c0 = row.createCell(0);
                c0.setCellValue(item.getIngredientName());
                c0.setCellStyle(bodyStyle);

                // Đơn vị
                Cell c1 = row.createCell(1);
                c1.setCellValue(item.getUnit());
                c1.setCellStyle(bodyStyle);

                // Tổng nhập
                BigDecimal imported = item.getTotalImportedAmount() != null
                        ? item.getTotalImportedAmount()
                        : BigDecimal.ZERO;

                createNumberCell(row, 2, imported, numberStyle);

                totalImported = totalImported.add(imported);
            }
        }

        // ==========================================================
        // FOOTER
        // ==========================================================
        Row footer = sheet.createRow(rowIndex++);

        Cell fl = footer.createCell(0);
        fl.setCellValue("TỔNG CỘNG");
        fl.setCellStyle(footerStyle);

        footer.createCell(1).setCellStyle(footerStyle);
        createNumberCell(footer, 2, totalImported, footerStyle);

        // ==========================================================
        // AUTO-SIZE + FREEZE
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
