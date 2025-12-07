package com.restaurant.api.export.excel;

import com.restaurant.api.dto.report.RevenueByDayItem;
import com.restaurant.api.dto.report.RevenueReportResponse;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * RevenueExcelExporter
 * =====================================================================
 * Class chuyên xuất Excel cho BÁO CÁO DOANH THU.
 *
 *  - Áp dụng STYLE A (hiện đại, sạch, border mảnh, header xám nhạt)
 *  - Dùng BaseExcelExporter để tái sử dụng style & tiện ích chung
 *  - Dữ liệu đầu vào: RevenueReportResponse (từ ReportService)
 *
 *  Quy trình xuất:
 *    1) Tạo Workbook + Sheet
 *    2) Ghi tiêu đề + khoảng thời gian filter
 *    3) Ghi khối tổng hợp (tổng doanh thu, số đơn, TB/ngày)
 *    4) Ghi header bảng chi tiết
 *    5) Ghi từng dòng doanh thu theo ngày
 *    6) Ghi dòng "TỔNG CỘNG" ở cuối bảng
 *    7) Auto-size cột + freeze header
 *    8) Trả về byte[] cho Controller
 * =====================================================================
 */
@Component
public class RevenueExcelExporter extends BaseExcelExporter {

    // Định dạng ngày hiển thị trong Excel (dd/MM/yyyy)
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Hàm xuất Excel cho báo cáo doanh thu.
     * --------------------------------------------------------------
     * @param report  : DTO tổng hợp báo cáo doanh thu
     * @param from    : Ngày bắt đầu filter (có thể null)
     * @param to      : Ngày kết thúc filter (có thể null)
     * @return        : Mảng byte[] nội dung file .xlsx
     */
    public byte[] export(RevenueReportResponse report, LocalDate from, LocalDate to) {

        // 1. Tạo workbook + sheet
        Workbook wb = createWorkbook();
        Sheet sheet = wb.createSheet("DoanhThu");

        // 2. Tạo sẵn các style dùng chung
        CellStyle titleStyle = createTitleStyle(wb);       // Style tiêu đề lớn
        CellStyle headerStyle = createHeaderStyle(wb);     // Style header bảng
        CellStyle bodyStyle = createBodyStyle(wb);         // Style nội dung
        CellStyle numberStyle = createNumberStyle(wb);     // Style số liệu
        CellStyle footerStyle = createFooterStyle(wb);     // Style dòng tổng cộng

        int rowIndex = 0; // Biến đếm số dòng hiện tại

        // ==========================================================
        // 2.1. GHI TIÊU ĐỀ BÁO CÁO (MERGE NHIỀU CỘT)
        // ==========================================================
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BÁO CÁO DOANH THU");
        titleCell.setCellStyle(titleStyle);

        // Merge từ cột 0 → 2 (3 cột: Ngày, Doanh thu, Số đơn)
        mergeCells(sheet, 0, 0, 0, 2);

        // ==========================================================
        // 2.2. GHI KHOẢNG THỜI GIAN LỌC
        // ==========================================================
        Row rangeRow = sheet.createRow(rowIndex++);
        Cell rangeCell = rangeRow.createCell(0);
        rangeCell.setCellStyle(bodyStyle);
        rangeCell.setCellValue(buildRangeText(from, to));
        // Merge cả dòng range cho đẹp
        mergeCells(sheet, 1, 1, 0, 2);

        // Tạo 1 dòng trống để cách khối tiêu đề và nội dung
        rowIndex++;

        // ==========================================================
        // 3. GHI KHỐI TỔNG HỢP (TỔNG DOANH THU / SỐ ĐƠN / TB NGÀY)
        // ==========================================================
        // Dòng: Tổng doanh thu
        Row totalRevenueRow = sheet.createRow(rowIndex++);
        createLabelCell(totalRevenueRow, 0, "Tổng doanh thu", bodyStyle);
        createNumberCell(totalRevenueRow, 1, report.getTotalRevenue(), numberStyle);

        // Dòng: Tổng số đơn
        Row totalOrdersRow = sheet.createRow(rowIndex++);
        createLabelCell(totalOrdersRow, 0, "Tổng số đơn", bodyStyle);
        createNumberCell(totalOrdersRow, 1, BigDecimal.valueOf(
                report.getTotalOrders() != null ? report.getTotalOrders() : 0
        ), numberStyle);

        // Dòng: Doanh thu TB/ngày
        Row avgRow = sheet.createRow(rowIndex++);
        createLabelCell(avgRow, 0, "Doanh thu trung bình/ngày", bodyStyle);
        createNumberCell(avgRow, 1, report.getAverageRevenuePerDay(), numberStyle);

        // Thêm 1 dòng trống
        rowIndex++;

        // ==========================================================
        // 4. GHI HEADER BẢNG CHI TIẾT
        // ==========================================================
        int headerRowIndex = rowIndex; // lưu lại để freeze pane
        Row headerRow = sheet.createRow(rowIndex++);

        String[] headers = {"Ngày", "Doanh thu", "Số đơn"};

        for (int col = 0; col < headers.length; col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(headers[col]);
            cell.setCellStyle(headerStyle);
        }

        // ==========================================================
        // 5. GHI DỮ LIỆU DOANH THU THEO TỪNG NGÀY
        // ==========================================================
        if (report.getItems() != null) {
            for (RevenueByDayItem item : report.getItems()) {

                Row row = sheet.createRow(rowIndex++);

                // Cột 0: Ngày
                Cell dateCell = row.createCell(0);
                dateCell.setCellStyle(bodyStyle);
                if (item.getDate() != null) {
                    dateCell.setCellValue(item.getDate().format(DATE_FMT));
                } else {
                    dateCell.setCellValue("");
                }

                // Cột 1: Doanh thu
                createNumberCell(row, 1, item.getRevenue(), numberStyle);

                // Cột 2: Số đơn
                BigDecimal orderCount = BigDecimal.valueOf(
                        item.getOrderCount() != null ? item.getOrderCount() : 0
                );
                createNumberCell(row, 2, orderCount, numberStyle);
            }
        }

        // ==========================================================
        // 6. DÒNG "TỔNG CỘNG" Ở CUỐI BẢNG CHI TIẾT
        // ==========================================================
        Row footerRow = sheet.createRow(rowIndex++);

        // Ô label "TỔNG CỘNG"
        Cell footerLabelCell = footerRow.createCell(0);
        footerLabelCell.setCellValue("TỔNG CỘNG");
        footerLabelCell.setCellStyle(footerStyle);

        // Ô tổng doanh thu
        createNumberCell(footerRow, 1, report.getTotalRevenue(), footerStyle);

        // Ô tổng số đơn
        BigDecimal totalOrders = BigDecimal.valueOf(
                report.getTotalOrders() != null ? report.getTotalOrders() : 0
        );
        createNumberCell(footerRow, 2, totalOrders, footerStyle);

        // ==========================================================
        // 7. TUNE LẠI SHEET: AUTO-SIZE CỘT + FREEZE HEADER
        // ==========================================================
        autoSizeColumns(sheet, headers.length);

        // Freeze pane: cố định phần header (dòng header + các dòng phía trên)
        // Tham số: colSplit = 0, rowSplit = headerRowIndex + 1
        sheet.createFreezePane(0, headerRowIndex + 1);

        // ==========================================================
        // 8. GHI RA BYTE[]
        // ==========================================================
        return writeToByteArray(wb);
    }

    /**
     * Tạo text hiển thị khoảng thời gian lọc.
     * Ví dụ:
     *  - "Khoảng thời gian: Toàn bộ dữ liệu"
     *  - "Khoảng thời gian: Từ 01/01/2025 đến 31/01/2025"
     */
    private String buildRangeText(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return "Khoảng thời gian: Toàn bộ dữ liệu";
        }

        String fromStr = from != null ? from.format(DATE_FMT) : "...";
        String toStr = to != null ? to.format(DATE_FMT) : "...";

        return "Khoảng thời gian: Từ " + fromStr + " đến " + toStr;
    }

    /**
     * Tạo ô label (text thường, ở cột trái).
     */
    private void createLabelCell(Row row, int colIndex, String text, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(text != null ? text : "");
        cell.setCellStyle(style);
    }

    /**
     * Tạo ô số (dùng cho BigDecimal / Long).
     * --------------------------------------------------------------
     * - Nếu value == null → set 0
     * - Hiển thị dạng số, để người dùng có thể format tiếp trong Excel
     */
    private void createNumberCell(Row row, int colIndex, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellStyle(style);

        if (value == null) {
            cell.setCellValue(0);
        } else {
            cell.setCellValue(value.doubleValue());
        }
    }
}
