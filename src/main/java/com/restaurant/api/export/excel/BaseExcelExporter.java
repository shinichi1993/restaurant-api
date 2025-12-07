package com.restaurant.api.export.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;

/**
 * BaseExcelExporter
 * =====================================================================
 * Class tiện ích dùng chung cho tất cả Excel Exporter.
 * Áp dụng STYLE A – giao diện hiện đại, sạch, chuyên nghiệp:
 *
 *  - Header nền xám nhạt (#E8E8E8), chữ đậm
 *  - Border mảnh (thin)
 *  - Các ô body có border, căn trái
 *  - Số liệu căn phải (khi cần)
 *  - Merge cell cho tiêu đề
 *  - Tự động căn chỉnh độ rộng cột
 *
 *  File này sẽ được RevenueExcelExporter, IngredientUsageExcelExporter,
 *  TopDishExcelExporter, StockEntryExcelExporter kế thừa.
 * =====================================================================
 */
public abstract class BaseExcelExporter {

    /**
     * Tạo workbook mới (.xlsx)
     */
    protected Workbook createWorkbook() {
        return new XSSFWorkbook();
    }

    /**
     * Style tiêu đề lớn (merge nhiều cột)
     */
    protected CellStyle createTitleStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);

        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);

        return style;
    }

    /**
     * Style header: nền xám, chữ đậm, border mảnh
     */
    protected CellStyle createHeaderStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);

        CellStyle style = wb.createCellStyle();
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Nền xám nhạt style A
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorder(style);

        return style;
    }

    /**
     * Style body: border mảnh, căn trái
     */
    protected CellStyle createBodyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(style);
        return style;
    }

    /**
     * Style số liệu (căn phải)
     */
    protected CellStyle createNumberStyle(Workbook wb) {
        CellStyle style = createBodyStyle(wb);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    /**
     * Style footer (tổng cộng)
     */
    protected CellStyle createFooterStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);

        CellStyle style = createNumberStyle(wb);
        style.setFont(font);

        return style;
    }

    /**
     * Border mảnh cho các ô
     */
    private void setBorder(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    /**
     * Merge cell (dùng cho tiêu đề)
     */
    protected void mergeCells(Sheet sheet, int rowStart, int rowEnd, int colStart, int colEnd) {
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                rowStart, rowEnd, colStart, colEnd
        ));
    }

    /**
     * Tự động căn chỉnh độ rộng các cột
     */
    protected void autoSizeColumns(Sheet sheet, int colCount) {
        for (int i = 0; i < colCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Ghi workbook ra byte[] để controller trả về
     */
    protected byte[] writeToByteArray(Workbook wb) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất Excel", e);
        }
    }
}
