package com.restaurant.api.service;

import com.restaurant.api.dto.report.*;
import com.restaurant.api.entity.*;
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
    // 5. EXPORT DOANH THU – EXCEL
    // ==================================================================

    /**
     * Xuất báo cáo doanh thu ra file Excel (.xlsx)
     * - Dùng lại dữ liệu từ getRevenueReport(...)
     */
    @Transactional(readOnly = true)
    public byte[] exportRevenueToExcel(LocalDate from, LocalDate to) {

        RevenueReportResponse report = getRevenueReport(from, to);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("DoanhThu");

            // Style header
            CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            int row = 0;

            // Tiêu đề
            Row r1 = sheet.createRow(row++);
            r1.createCell(0).setCellValue("BÁO CÁO DOANH THU");

            // Khoảng thời gian
            Row r2 = sheet.createRow(row++);
            if (from != null && to != null) {
                r2.createCell(0).setCellValue("Từ ngày " + from + " đến ngày " + to);
            } else {
                r2.createCell(0).setCellValue("Toàn bộ dữ liệu");
            }

            row++;

            // Dòng tổng hợp
            Row r3 = sheet.createRow(row++);
            r3.createCell(0).setCellValue("Tổng doanh thu");
            r3.createCell(1).setCellValue(report.getTotalRevenue().doubleValue());

            Row r4 = sheet.createRow(row++);
            r4.createCell(0).setCellValue("Tổng số đơn");
            r4.createCell(1).setCellValue(report.getTotalOrders());

            Row r5 = sheet.createRow(row++);
            r5.createCell(0).setCellValue("Doanh thu TB/ngày");
            r5.createCell(1).setCellValue(report.getAverageRevenuePerDay().doubleValue());

            row++;

            // Header bảng chi tiết
            Row header = sheet.createRow(row++);
            String[] cols = {"Ngày", "Doanh thu", "Số đơn"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            // Dữ liệu chi tiết
            for (RevenueByDayItem item : report.getItems()) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(item.getDate().toString());
                r.createCell(1).setCellValue(item.getRevenue().doubleValue());
                r.createCell(2).setCellValue(item.getOrderCount());
            }

            // Auto width
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            wb.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất Excel báo cáo doanh thu", e);
        }
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

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ===================== FONT UNICODE ======================
            String fontPath = "fonts/arial.ttf";
            Font unicodeTitle = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 14, Font.BOLD);
            Font unicodeHeader = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10, Font.BOLD);
            Font unicodeNormal = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10);

            // ======================== HEADER =========================
            Paragraph title = new Paragraph("BÁO CÁO DOANH THU", unicodeTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            doc.add(title);

            String range = (from != null && to != null)
                    ? "Từ ngày " + from + " đến ngày " + to
                    : "Toàn bộ dữ liệu";

            Paragraph pRange = new Paragraph(range, unicodeNormal);
            pRange.setAlignment(Element.ALIGN_CENTER);
            pRange.setSpacingAfter(15);
            doc.add(pRange);

            doc.add(new Paragraph("Tổng doanh thu: " + formatMoney(report.getTotalRevenue()), unicodeNormal));
            doc.add(new Paragraph("Tổng số đơn: " + report.getTotalOrders(), unicodeNormal));
            doc.add(new Paragraph("Doanh thu TB/ngày: " + formatMoney(report.getAverageRevenuePerDay()), unicodeNormal));

            // ======================== TABLE ==========================
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 3, 2});

            addHeaderCell(table, "Ngày", unicodeHeader);
            addHeaderCell(table, "Doanh thu", unicodeHeader);
            addHeaderCell(table, "Số đơn", unicodeHeader);

            for (RevenueByDayItem item : report.getItems()) {
                addBodyCell(table, item.getDate().toString(), unicodeNormal);
                addBodyCell(table, formatMoney(item.getRevenue()), unicodeNormal);
                addBodyCell(table, String.valueOf(item.getOrderCount()), unicodeNormal);
            }

            doc.add(table);

            Paragraph footer = new Paragraph(
                    "Ngày in: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    unicodeNormal
            );
            footer.setAlignment(Element.ALIGN_RIGHT);
            footer.setSpacingBefore(15);
            doc.add(footer);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất PDF báo cáo doanh thu", e);
        }
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

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("TopDish");

            CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            int row = 0;

            Row r1 = sheet.createRow(row++);
            r1.createCell(0).setCellValue("BÁO CÁO TOP MÓN BÁN CHẠY");

            Row r2 = sheet.createRow(row++);
            if (from != null && to != null) {
                r2.createCell(0).setCellValue("Từ ngày " + from + " đến ngày " + to);
            } else {
                r2.createCell(0).setCellValue("Toàn bộ dữ liệu");
            }

            row++;

            Row header = sheet.createRow(row++);
            String[] cols = {"Món ăn", "Số lượng", "Doanh thu"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            for (TopDishReportItem item : items) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(item.getDishName());
                r.createCell(1).setCellValue(item.getTotalQuantity());
                r.createCell(2).setCellValue(item.getTotalRevenue().doubleValue());
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            wb.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất Excel báo cáo top món", e);
        }
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

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ===================== FONT UNICODE ======================
            String fontPath = "fonts/arial.ttf";
            Font titleFont = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 14, Font.BOLD);
            Font headerFont = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10, Font.BOLD);
            Font normalFont = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10);

            Paragraph title = new Paragraph("BÁO CÁO TOP MÓN BÁN CHẠY", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            doc.add(title);

            String range = (from != null && to != null)
                    ? "Từ ngày " + from + " đến ngày " + to
                    : "Toàn bộ dữ liệu";
            Paragraph pRange = new Paragraph(range, normalFont);
            pRange.setAlignment(Element.ALIGN_CENTER);
            pRange.setSpacingAfter(15);
            doc.add(pRange);

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{5, 2, 3});

            addHeaderCell(table, "Món ăn", headerFont);
            addHeaderCell(table, "Số lượng", headerFont);
            addHeaderCell(table, "Doanh thu", headerFont);

            for (TopDishReportItem item : items) {
                addBodyCell(table, item.getDishName(), normalFont);
                addBodyCell(table, String.valueOf(item.getTotalQuantity()), normalFont);
                addBodyCell(table, formatMoney(item.getTotalRevenue()), normalFont);
            }

            doc.add(table);
            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất PDF báo cáo top món", e);
        }
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

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("IngredientUsage");

            CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            int row = 0;

            Row r1 = sheet.createRow(row++);
            r1.createCell(0).setCellValue("BÁO CÁO NGUYÊN LIỆU TIÊU HAO");

            Row r2 = sheet.createRow(row++);
            if (from != null && to != null) {
                r2.createCell(0).setCellValue("Từ ngày " + from + " đến ngày " + to);
            } else {
                r2.createCell(0).setCellValue("Toàn bộ dữ liệu");
            }

            row++;

            Row header = sheet.createRow(row++);
            String[] cols = {"Nguyên liệu", "Số lượng dùng", "Đơn vị"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            for (IngredientUsageReportItem item : items) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(item.getIngredientName());
                r.createCell(1).setCellValue(item.getTotalUsed().doubleValue());
                r.createCell(2).setCellValue(item.getUnit());
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            wb.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất Excel báo cáo nguyên liệu tiêu hao", e);
        }
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

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ===================== FONT UNICODE ======================
            String fontPath = "fonts/arial.ttf";
            Font titleFont = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 14, Font.BOLD);
            Font headerFont = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10, Font.BOLD);
            Font normalFont = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10);

            Paragraph title = new Paragraph("BÁO CÁO NGUYÊN LIỆU TIÊU HAO", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            doc.add(title);

            String range = (from != null && to != null)
                    ? "Từ ngày " + from + " đến ngày " + to
                    : "Toàn bộ dữ liệu";
            Paragraph rangeP = new Paragraph(range, normalFont);
            rangeP.setAlignment(Element.ALIGN_CENTER);
            rangeP.setSpacingAfter(15);
            doc.add(rangeP);

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{5, 3, 2});

            addHeaderCell(table, "Nguyên liệu", headerFont);
            addHeaderCell(table, "Số lượng dùng", headerFont);
            addHeaderCell(table, "Đơn vị", headerFont);

            for (IngredientUsageReportItem item : items) {
                addBodyCell(table, item.getIngredientName(), normalFont);
                addBodyCell(table, item.getTotalUsed().toPlainString(), normalFont);
                addBodyCell(table, item.getUnit(), normalFont);
            }

            doc.add(table);
            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất PDF báo cáo nguyên liệu tiêu hao", e);
        }
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

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("NhapKhoNguyenLieu");

            CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            int row = 0;

            Row r1 = sheet.createRow(row++);
            r1.createCell(0).setCellValue("BÁO CÁO NHẬP KHO NGUYÊN LIỆU");

            Row r2 = sheet.createRow(row++);
            if (from != null && to != null) {
                r2.createCell(0).setCellValue("Từ ngày " + from + " đến ngày " + to);
            } else {
                r2.createCell(0).setCellValue("Toàn bộ dữ liệu");
            }

            row++;

            Row header = sheet.createRow(row++);
            String[] cols = {"Nguyên liệu", "Tổng lượng nhập", "Đơn vị"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            for (StockEntryReportItem item : items) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(item.getIngredientName());
                r.createCell(1).setCellValue(item.getTotalImportedAmount().doubleValue());
                r.createCell(2).setCellValue(item.getUnit());
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            wb.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất Excel báo cáo nhập kho nguyên liệu", e);
        }
    }

    /**
     * Xuất PDF báo cáo NHẬP KHO nguyên liệu.
     */
    @Transactional(readOnly = true)
    public byte[] exportStockEntryToPdf(LocalDate from, LocalDate to) {

        List<StockEntryReportItem> items = getStockEntryReport(from, to);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // ===================== FONT UNICODE ======================
            String fontPath = "fonts/arial.ttf";
            Font titleFont = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 14, Font.BOLD);
            Font headerFont = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10, Font.BOLD);
            Font normalFont = FontFactory.getFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 10);

            Paragraph p1 = new Paragraph("BÁO CÁO NHẬP KHO NGUYÊN LIỆU", titleFont);
            p1.setAlignment(Element.ALIGN_CENTER);
            p1.setSpacingAfter(10);
            doc.add(p1);

            String range = (from != null && to != null)
                    ? "Từ ngày " + from + " đến ngày " + to
                    : "Toàn bộ dữ liệu";

            Paragraph p2 = new Paragraph(range, normalFont);
            p2.setAlignment(Element.ALIGN_CENTER);
            p2.setSpacingAfter(15);
            doc.add(p2);

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{5, 3, 2});

            addHeaderCell(table, "Nguyên liệu", headerFont);
            addHeaderCell(table, "Tổng lượng nhập", headerFont);
            addHeaderCell(table, "Đơn vị", headerFont);

            for (StockEntryReportItem item : items) {
                addBodyCell(table, item.getIngredientName(), normalFont);
                addBodyCell(table, item.getTotalImportedAmount().toPlainString(), normalFont);
                addBodyCell(table, item.getUnit(), normalFont);
            }

            doc.add(table);

            Paragraph footer = new Paragraph(
                    "Ngày in: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    normalFont
            );
            footer.setAlignment(Element.ALIGN_RIGHT);
            footer.setSpacingBefore(15);
            doc.add(footer);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất PDF báo cáo nhập kho nguyên liệu", e);
        }
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
