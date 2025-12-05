package com.restaurant.api.controller;

import com.restaurant.api.dto.report.*;
import com.restaurant.api.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * ReportController – Module 12
 * ======================================================================
 * Controller cung cấp các API BÁO CÁO cho FE:
 *
 *  1. GET /api/reports/revenue
 *     → Báo cáo doanh thu theo khoảng ngày
 *
 *  2. GET /api/reports/top-dishes
 *     → Báo cáo TOP món bán chạy
 *
 *  3. GET /api/reports/ingredient-usage
 *     → Báo cáo NGUYÊN LIỆU TIÊU HAO
 *
 *  4. GET /api/reports/stock-entry
 *     → Báo cáo NHẬP KHO nguyên liệu
 *
 * Ghi chú:
 *  - Toàn bộ comment dùng tiếng Việt theo Rule 13.
 *  - Bảo mật: Các API này sẽ đi qua SecurityConfig, chỉ cho phép
 *    user đã đăng nhập (và có role phù hợp) gọi.
 * ======================================================================
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ==================================================================
    // 1. BÁO CÁO DOANH THU THEO KHOẢNG NGÀY
    // ==================================================================

    /**
     * API: Lấy báo cáo doanh thu trong khoảng ngày.
     * --------------------------------------------------------------
     * URL ví dụ:
     *  - GET /api/reports/revenue
     *      → không truyền fromDate/toDate → lấy toàn bộ
     *
     *  - GET /api/reports/revenue?fromDate=2025-12-01&toDate=2025-12-10
     *      → Lấy dữ liệu từ 01/12 đến hết 10/12
     *
     * Tham số:
     *  - fromDate, toDate (yyyy-MM-dd) – có thể để trống
     *
     * Kết quả:
     *  - RevenueReportResponse:
     *      + totalRevenue          → tổng doanh thu
     *      + totalOrders           → tổng số order
     *      + averageRevenuePerDay  → doanh thu TB/ngày
     *      + items                 → list doanh thu từng ngày
     */
    @GetMapping("/revenue")
    public ResponseEntity<RevenueReportResponse> getRevenueReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        RevenueReportResponse result = reportService.getRevenueReport(fromDate, toDate);
        return ResponseEntity.ok(result);
    }

    // ==================================================================
    // 2. BÁO CÁO TOP MÓN BÁN CHẠY
    // ==================================================================

    /**
     * API: Lấy TOP món bán chạy trong khoảng ngày.
     * --------------------------------------------------------------
     * URL ví dụ:
     *  - GET /api/reports/top-dishes
     *      → lấy top 10 món trong toàn bộ thời gian
     *
     *  - GET /api/reports/top-dishes?fromDate=2025-12-01&toDate=2025-12-31&limit=5
     *      → Lấy top 5 món từ 01/12 đến 31/12
     *
     * Tham số:
     *  - fromDate, toDate (yyyy-MM-dd) – có thể null
     *  - limit (optional, default = 10)
     *
     * Kết quả:
     *  - List<TopDishReportItem>:
     *      + dishId
     *      + dishName
     *      + totalQuantity
     *      + totalRevenue
     */
    @GetMapping("/top-dishes")
    public ResponseEntity<List<TopDishReportItem>> getTopDishes(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false, defaultValue = "10")
            int limit
    ) {
        List<TopDishReportItem> result = reportService.getTopDishes(fromDate, toDate, limit);
        return ResponseEntity.ok(result);
    }

    // ==================================================================
    // 3. BÁO CÁO NGUYÊN LIỆU TIÊU HAO
    // ==================================================================

    /**
     * API: Báo cáo NGUYÊN LIỆU TIÊU HAO trong khoảng ngày.
     * --------------------------------------------------------------
     * Quy ước trong ReportService:
     *  - Dựa vào bảng StockEntry
     *  - Chỉ lấy các bản ghi có quantity âm (tiêu hao / xuất kho)
     *
     * URL ví dụ:
     *  - GET /api/reports/ingredient-usage
     *  - GET /api/reports/ingredient-usage?fromDate=2025-12-01&toDate=2025-12-31
     *
     * Kết quả:
     *  - List<IngredientUsageReportItem>:
     *      + ingredientId
     *      + ingredientName
     *      + unit
     *      + totalUsed (số lượng đã sử dụng, luôn dương)
     */
    @GetMapping("/ingredient-usage")
    public ResponseEntity<List<IngredientUsageReportItem>> getIngredientUsage(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        List<IngredientUsageReportItem> result = reportService.getIngredientUsageReport(fromDate, toDate);
        return ResponseEntity.ok(result);
    }

    // ==================================================================
    // 4. BÁO CÁO NHẬP KHO NGUYÊN LIỆU
    // ==================================================================

    /**
     * API: Báo cáo NHẬP KHO nguyên liệu trong khoảng ngày.
     * --------------------------------------------------------------
     * Quy ước trong ReportService:
     *  - Dựa vào bảng StockEntry
     *  - Chỉ lấy các bản ghi quantity dương (nhập kho)
     *
     * URL ví dụ:
     *  - GET /api/reports/stock-entry
     *  - GET /api/reports/stock-entry?fromDate=2025-12-01&toDate=2025-12-31
     *
     * Kết quả:
     *  - List<StockEntryReportItem>:
     *      + ingredientId
     *      + ingredientName
     *      + unit
     *      + totalImportedAmount (tổng số lượng đã nhập)
     */
    @GetMapping("/stock-entry")
    public ResponseEntity<List<StockEntryReportItem>> getStockEntryReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        List<StockEntryReportItem> result = reportService.getStockEntryReport(fromDate, toDate);
        return ResponseEntity.ok(result);
    }

    // ============================================================
    // CÁC API JSON ĐÃ CÓ (getRevenueReport, getTopDishesReport, ...)
    // ============================================================

    // ... (giữ nguyên)

    // ============================================================
    // EXPORT DOANH THU
    // ============================================================

    @GetMapping("/revenue/export-excel")
    public ResponseEntity<byte[]> exportRevenueExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        byte[] data = reportService.exportRevenueToExcel(from, to);
        String fileName = buildFileName("revenue", "xlsx");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/revenue/export-pdf")
    public ResponseEntity<byte[]> exportRevenuePdf(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        byte[] data = reportService.exportRevenueToPdf(from, to);
        String fileName = buildFileName("revenue", "pdf");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    // ============================================================
    // EXPORT TOP DISH
    // ============================================================

    @GetMapping("/top-dishes/export-excel")
    public ResponseEntity<byte[]> exportTopDishesExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {

        byte[] data = reportService.exportTopDishesToExcel(from, to, limit);
        String fileName = buildFileName("top-dishes", "xlsx");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/top-dishes/export-pdf")
    public ResponseEntity<byte[]> exportTopDishesPdf(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {

        byte[] data = reportService.exportTopDishesToPdf(from, to, limit);
        String fileName = buildFileName("top-dishes", "pdf");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    // ============================================================
    // EXPORT NGUYÊN LIỆU TIÊU HAO
    // ============================================================

    @GetMapping("/ingredients/export-excel")
    public ResponseEntity<byte[]> exportIngredientUsageExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        byte[] data = reportService.exportIngredientUsageToExcel(from, to);
        String fileName = buildFileName("ingredients", "xlsx");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/ingredients/export-pdf")
    public ResponseEntity<byte[]> exportIngredientUsagePdf(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        byte[] data = reportService.exportIngredientUsageToPdf(from, to);
        String fileName = buildFileName("ingredients", "pdf");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    // ============================================================
    // 4. EXPORT NHẬP KHO NGUYÊN LIỆU
    // ============================================================

    @GetMapping("/stock-entry/export-excel")
    public ResponseEntity<byte[]> exportStockEntryExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        byte[] data = reportService.exportStockEntryToExcel(from, to);
        String fileName = buildFileName("stock-entry", "xlsx");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/stock-entry/export-pdf")
    public ResponseEntity<byte[]> exportStockEntryPdf(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        byte[] data = reportService.exportStockEntryToPdf(from, to);
        String fileName = buildFileName("stock-entry", "pdf");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    // ============================================================
    // HÀM TẠO TÊN FILE THEO RULE: reportType-yyyyMMdd_HHmmss.ext
    // ============================================================

    private String buildFileName(String prefix, String ext) {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return prefix + "-" + timestamp + "." + ext;
    }
}
