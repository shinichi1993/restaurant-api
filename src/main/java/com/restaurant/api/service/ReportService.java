package com.restaurant.api.service;

import com.restaurant.api.dto.report.*;
import com.restaurant.api.entity.*;
import com.restaurant.api.export.excel.IngredientUsageExcelExporter;
import com.restaurant.api.export.excel.RevenueExcelExporter;
import com.restaurant.api.export.excel.StockEntryExcelExporter;
import com.restaurant.api.export.excel.TopDishExcelExporter;
import com.restaurant.api.export.pdf.IngredientUsagePdfExporter;
import com.restaurant.api.export.pdf.RevenuePdfExporter;
import com.restaurant.api.export.pdf.StockEntryPdfExporter;
import com.restaurant.api.export.pdf.TopDishPdfExporter;
import com.restaurant.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// ========================== PDF (OpenPDF) ==========================
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.BaseFont;

import java.awt.Color;

/**
 * ReportService – Module 12
 * ======================================================================
 * Service xử lý toàn bộ nghiệp vụ BÁO CÁO:
 *  - Báo cáo doanh thu
 *  - Báo cáo top món bán chạy
 *  - Báo cáo nguyên liệu tiêu hao (xuất kho)
 *  - Báo cáo nhập kho
 *  - Export Excel / PDF cho 3 loại báo cáo:
 *      + Doanh thu
 *      + Top món
 *      + Nguyên liệu tiêu hao
 *
 * Tất cả comment tuân theo Rule 13 – tiếng Việt đầy đủ.
 * ======================================================================
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DishRepository dishRepository;
    private final StockEntryRepository stockEntryRepository;
    private final IngredientRepository ingredientRepository;
    // ================== THÊM MỚI – EXPORTER DOANH THU ==================
    /**
     * Bean Excel Exporter cho báo cáo doanh thu.
     * Dùng để tách phần format Excel ra khỏi Service nghiệp vụ.
     */
    private final RevenueExcelExporter revenueExcelExporter;
    private final TopDishExcelExporter topDishExcelExporter;
    private final IngredientUsageExcelExporter ingredientUsageExcelExporter;
    private final StockEntryExcelExporter stockEntryExcelExporter;

    private final RevenuePdfExporter revenuePdfExporter;
    private final TopDishPdfExporter topDishPdfExporter;
    private final IngredientUsagePdfExporter ingredientUsagePdfExporter;
    private final StockEntryPdfExporter stockEntryPdfExporter;

    // ==================================================================
    // 1. BÁO CÁO DOANH THU THEO KHOẢNG NGÀY
    // ==================================================================

    /**
     * Lấy báo cáo doanh thu theo khoảng ngày.
     * --------------------------------------------------------------
     * - Dữ liệu lấy từ bảng Invoice
     * - Chỉ tính invoice có paidAt != null
     * - Group theo ngày paidAt.toLocalDate()
     */
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(LocalDate fromDate, LocalDate toDate) {

        LocalDateTime fromDateTime = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;

        // Lấy toàn bộ hóa đơn
        List<Invoice> allInvoices = invoiceRepository.findAll();

        // Lọc theo ngày thanh toán
        List<Invoice> filtered = allInvoices.stream()
                .filter(inv -> inv.getPaidAt() != null)
                .filter(inv -> {
                    LocalDateTime paidAt = inv.getPaidAt();
                    if (fromDateTime != null && paidAt.isBefore(fromDateTime)) return false;
                    if (toDateTime != null && !paidAt.isBefore(toDateTime)) return false;
                    return true;
                })
                .toList();

        if (filtered.isEmpty()) {
            return RevenueReportResponse.builder()
                    .totalRevenue(BigDecimal.ZERO)
                    .totalOrders(0L)
                    .averageRevenuePerDay(BigDecimal.ZERO)
                    .items(Collections.emptyList())
                    .build();
        }

        // Group theo ngày
        Map<LocalDate, List<Invoice>> byDate = filtered.stream()
                .collect(Collectors.groupingBy(inv -> inv.getPaidAt().toLocalDate()));

        List<RevenueByDayItem> items = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalOrders = 0L;

        for (var entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<Invoice> list = entry.getValue();

            BigDecimal revenue = list.stream()
                    .map(Invoice::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long orderCount = list.size();

            totalRevenue = totalRevenue.add(revenue);
            totalOrders += orderCount;

            items.add(
                    RevenueByDayItem.builder()
                            .date(date)
                            .revenue(revenue)
                            .orderCount(orderCount)
                            .build()
            );
        }

        // Sắp xếp theo ngày tăng dần
        items.sort(Comparator.comparing(RevenueByDayItem::getDate));

        // Doanh thu TB / ngày
        int days = items.size();
        BigDecimal avg = (days > 0)
                ? totalRevenue.divide(BigDecimal.valueOf(days), 0, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        return RevenueReportResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageRevenuePerDay(avg)
                .items(items)
                .build();
    }

    // ==================================================================
    // 2. TOP MÓN BÁN CHẠY
    // ==================================================================

    /**
     * Lấy danh sách TOP món bán chạy trong khoảng ngày.
     * --------------------------------------------------------------
     * - Lọc Order trong khoảng fromDate–toDate theo createdAt
     * - Lấy OrderItem thuộc các order đó
     * - Gom nhóm theo dishId → tổng quantity
     * - Nhân với Dish.price để ra doanh thu
     */
    @Transactional(readOnly = true)
    public List<TopDishReportItem> getTopDishes(LocalDate fromDate, LocalDate toDate, int limit) {

        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;

        List<Order> allOrders = orderRepository.findAll();

        List<Order> filteredOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .filter(o -> {
                    LocalDateTime t = o.getCreatedAt();
                    if (from != null && t.isBefore(from)) return false;
                    if (to != null && !t.isBefore(to)) return false;
                    return true;
                })
                .toList();

        if (filteredOrders.isEmpty()) return Collections.emptyList();

        // Lấy list orderId
        Set<Long> orderIds = filteredOrders.stream()
                .map(Order::getId)
                .collect(Collectors.toSet());

        // Lấy toàn bộ OrderItem thuộc các order đó
        List<OrderItem> relatedItems = orderItemRepository.findAll().stream()
                .filter(i -> orderIds.contains(i.getOrderId()))
                .toList();

        if (relatedItems.isEmpty()) return Collections.emptyList();

        // Gom nhóm theo dishId → tổng số lượng
        Map<Long, Long> qtyByDish = relatedItems.stream()
                .collect(Collectors.groupingBy(
                        OrderItem::getDishId,
                        Collectors.summingLong(OrderItem::getQuantity)
                ));

        // Lấy thông tin món ăn
        Set<Long> dishIds = qtyByDish.keySet();
        Map<Long, Dish> dishMap = dishRepository.findAllById(dishIds).stream()
                .collect(Collectors.toMap(Dish::getId, d -> d));

        List<TopDishReportItem> items = new ArrayList<>();

        for (var entry : qtyByDish.entrySet()) {
            Long dishId = entry.getKey();
            Long qty = entry.getValue();

            Dish dish = dishMap.get(dishId);
            if (dish == null) continue;

            BigDecimal price = dish.getPrice() != null ? dish.getPrice() : BigDecimal.ZERO;
            BigDecimal revenue = price.multiply(BigDecimal.valueOf(qty));

            items.add(
                    TopDishReportItem.builder()
                            .dishId(dish.getId())
                            .dishName(dish.getName())
                            .totalQuantity(qty)
                            .totalRevenue(revenue)
                            .build()
            );
        }

        // Sắp xếp theo số lượng giảm dần, nếu bằng nhau thì theo doanh thu
        items.sort(
                Comparator.comparing(TopDishReportItem::getTotalQuantity).reversed()
                        .thenComparing(TopDishReportItem::getTotalRevenue).reversed()
        );

        if (limit > 0 && items.size() > limit) {
            return items.subList(0, limit);
        }
        return items;
    }

    // ==================================================================
    // 3. BÁO CÁO NGUYÊN LIỆU TIÊU HAO (xuất kho)
    // ==================================================================

    /**
     * Báo cáo nguyên liệu TIÊU HAO theo khoảng ngày.
     * --------------------------------------------------------------
     * - Dữ liệu lấy từ StockEntry
     * - Chỉ lấy quantity < 0 (xuất kho / tiêu hao)
     * - totalUsed trả về là số dương (abs)
     */
    @Transactional(readOnly = true)
    public List<IngredientUsageReportItem> getIngredientUsageReport(LocalDate fromDate, LocalDate toDate) {

        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;

        List<StockEntry> entries = stockEntryRepository.findAll();

        List<StockEntry> filtered = entries.stream()
                .filter(e -> e.getQuantity() != null && e.getQuantity().compareTo(BigDecimal.ZERO) < 0)
                .filter(e -> {
                    LocalDateTime t = e.getCreatedAt();
                    if (from != null && t.isBefore(from)) return false;
                    if (to != null && !t.isBefore(to)) return false;
                    return true;
                })
                .toList();

        if (filtered.isEmpty()) return Collections.emptyList();

        // Tổng số lượng dùng theo ingredientId
        Map<Long, BigDecimal> totalByIngredient = new HashMap<>();

        for (StockEntry e : filtered) {
            Ingredient ing = e.getIngredient();
            if (ing == null) continue;

            BigDecimal used = e.getQuantity().abs(); // chuyển dương
            totalByIngredient.merge(ing.getId(), used, BigDecimal::add);
        }

        List<IngredientUsageReportItem> items = new ArrayList<>();

        for (var entry : totalByIngredient.entrySet()) {
            Ingredient ing = ingredientRepository.findById(entry.getKey()).orElse(null);
            if (ing == null) continue;

            items.add(
                    IngredientUsageReportItem.builder()
                            .ingredientId(ing.getId())
                            .ingredientName(ing.getName())
                            .unit(ing.getUnit())
                            .totalUsed(entry.getValue())
                            .build()
            );
        }

        items.sort(Comparator.comparing(IngredientUsageReportItem::getTotalUsed).reversed());

        return items;
    }

    // ==================================================================
    // 4. BÁO CÁO NHẬP KHO (quantity > 0)
    // ==================================================================

    /**
     * Báo cáo NHẬP KHO nguyên liệu theo khoảng ngày.
     * --------------------------------------------------------------
     * - Lấy StockEntry có quantity > 0
     * - Group theo ingredientId → tổng quantity nhập
     */
    @Transactional(readOnly = true)
    public List<StockEntryReportItem> getStockEntryReport(LocalDate fromDate, LocalDate toDate) {

        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime to = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;

        List<StockEntry> entries = stockEntryRepository.findAll();

        List<StockEntry> filtered = entries.stream()
                .filter(e -> e.getQuantity() != null && e.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .filter(e -> {
                    LocalDateTime t = e.getCreatedAt();
                    if (from != null && t.isBefore(from)) return false;
                    if (to != null && !t.isBefore(to)) return false;
                    return true;
                })
                .toList();

        if (filtered.isEmpty()) return Collections.emptyList();

        Map<Long, BigDecimal> totalByIng = new HashMap<>();

        for (StockEntry e : filtered) {
            Ingredient ing = e.getIngredient();
            if (ing == null) continue;

            totalByIng.merge(ing.getId(), e.getQuantity(), BigDecimal::add);
        }

        List<StockEntryReportItem> items = new ArrayList<>();

        for (var entry : totalByIng.entrySet()) {
            Ingredient ing = ingredientRepository.findById(entry.getKey()).orElse(null);
            if (ing == null) continue;

            items.add(
                    StockEntryReportItem.builder()
                            .ingredientId(ing.getId())
                            .ingredientName(ing.getName())
                            .unit(ing.getUnit())
                            .totalImportedAmount(entry.getValue())
                            .build()
            );
        }

        items.sort(Comparator.comparing(StockEntryReportItem::getTotalImportedAmount).reversed());

        return items;
    }

    // ==================================================================
    // 5. EXPORT DOANH THU – EXCEL (DÙNG RevenueExcelExporter MỚI)
    // ==================================================================

    /**
     * Xuất báo cáo doanh thu ra file Excel (.xlsx)
     * ------------------------------------------------------------------
     * - Lấy dữ liệu báo cáo bằng getRevenueReport(from, to)
     * - Giao cho RevenueExcelExporter xử lý phần layout + style Excel
     * - Giữ đúng Rule 26: xử lý số liệu BigDecimal ở BE, FE nhận file
     */
    @Transactional(readOnly = true)
    public byte[] exportRevenueToExcel(LocalDate from, LocalDate to) {

        // 1. Lấy dữ liệu báo cáo doanh thu theo khoảng ngày
        RevenueReportResponse report = getRevenueReport(from, to);

        // 2. Giao cho ExcelExporter sinh file theo STYLE A
        return revenueExcelExporter.export(report, from, to);
    }

    // ==================================================================
    // 6. EXPORT DOANH THU – PDF
    // ==================================================================

    /**
     * Xuất báo cáo doanh thu ra PDF.
     */
    @Transactional(readOnly = true)
    public byte[] exportRevenueToPdf(LocalDate from, LocalDate to) {
        RevenueReportResponse report = getRevenueReport(from, to);
        return revenuePdfExporter.export(report, from, to);
    }

    // ==================================================================
    // 7. EXPORT TOP DISH – EXCEL
    // ==================================================================

    /**
     * Xuất báo cáo TOP MÓN BÁN CHẠY ra Excel.
     * Dùng lại dữ liệu từ getTopDishes(...)
     */
    @Transactional(readOnly = true)
    public byte[] exportTopDishesToExcel(LocalDate from, LocalDate to, int limit) {

        List<TopDishReportItem> items = getTopDishes(from, to, limit);

        return topDishExcelExporter.export(items, from, to);
    }


    // ==================================================================
    // 8. EXPORT TOP DISH – PDF
    // ==================================================================

    /**
     * Xuất báo cáo TOP MÓN BÁN CHẠY ra PDF.
     */
    @Transactional(readOnly = true)
    public byte[] exportTopDishesToPdf(LocalDate from, LocalDate to, int limit) {

        List<TopDishReportItem> items = getTopDishes(from, to, limit);
        // nếu TopDishPdfExporter của bạn đang có thêm tham số limit thì gọi export(items, from, to, limit);
        return topDishPdfExporter.export(items, from, to);
    }

    // ==================================================================
    // 9. EXPORT NGUYÊN LIỆU TIÊU HAO – EXCEL
    // ==================================================================

    /**
     * Xuất báo cáo NGUYÊN LIỆU TIÊU HAO ra Excel.
     */
    @Transactional(readOnly = true)
    public byte[] exportIngredientUsageToExcel(LocalDate from, LocalDate to) {
        List<IngredientUsageReportItem> items = getIngredientUsageReport(from, to);
        return ingredientUsageExcelExporter.export(items, from, to);
    }

    // ==================================================================
    // 10. EXPORT NGUYÊN LIỆU TIÊU HAO – PDF
    // ==================================================================

    /**
     * Xuất báo cáo NGUYÊN LIỆU TIÊU HAO ra PDF.
     */
    @Transactional(readOnly = true)
    public byte[] exportIngredientUsageToPdf(LocalDate from, LocalDate to) {

        List<IngredientUsageReportItem> items = getIngredientUsageReport(from, to);
        return ingredientUsagePdfExporter.export(items, from, to);
    }

    // ==================================================================
    // 7. EXPORT NHẬP KHO NGUYÊN LIỆU – EXCEL / PDF
    // ==================================================================

    /**
     * Xuất Excel báo cáo NHẬP KHO nguyên liệu.
     * Layout tách riêng, dễ nhìn (Option2).
     */
    @Transactional(readOnly = true)
    public byte[] exportStockEntryToExcel(LocalDate from, LocalDate to) {
        List<StockEntryReportItem> items = getStockEntryReport(from, to);
        return stockEntryExcelExporter.export(items, from, to);
    }

    /**
     * Xuất PDF báo cáo NHẬP KHO nguyên liệu.
     */
    @Transactional(readOnly = true)
    public byte[] exportStockEntryToPdf(LocalDate from, LocalDate to) {

        List<StockEntryReportItem> items = getStockEntryReport(from, to);
        return stockEntryPdfExporter.export(items, from, to);
    }

    // ==================================================================
    // 11. HÀM TIỆN ÍCH CHO PDF
    // ==================================================================

    /** Format tiền kiểu "120,000 đ" từ BigDecimal */
    private String formatMoney(BigDecimal value) {
        if (value == null) return "0 đ";
        return String.format("%,.0f đ", value.doubleValue());
    }

    /** Thêm ô header cho bảng PDF */
    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new Color(230, 230, 230));
        table.addCell(cell);
    }

    /** Thêm ô body cho bảng PDF */
    private void addBodyCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }
}
