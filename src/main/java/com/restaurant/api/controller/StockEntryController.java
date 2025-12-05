package com.restaurant.api.controller;

import com.restaurant.api.dto.stockentry.StockEntryCreateRequest;
import com.restaurant.api.dto.stockentry.StockEntryResponse;
import com.restaurant.api.service.StockEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * StockEntryController – API quản lý nhập kho / điều chỉnh kho.
 * --------------------------------------------------------------
 * API chính:
 *  - GET /api/stock-entries
 *      → Lấy toàn bộ lịch sử (order mới nhất trước)
 *
 *  - GET /api/stock-entries/filter?from=yyyy-MM-dd&to=yyyy-MM-dd
 *      → Lọc lịch sử theo khoảng ngày
 *
 *  - POST /api/stock-entries
 *      → Nhập kho bình thường (quantity > 0)
 *
 *  - POST /api/stock-entries/adjust
 *      → Điều chỉnh kho (quantity có thể âm, không cho tồn < 0)
 */
@RestController
@RequestMapping("/api/stock-entries")
@RequiredArgsConstructor
public class StockEntryController {

    private final StockEntryService stockEntryService;

    /**
     * API lấy toàn bộ lịch sử nhập kho / điều chỉnh kho.
     */
    @GetMapping
    public ResponseEntity<List<StockEntryResponse>> getAll() {
        return ResponseEntity.ok(stockEntryService.getAll());
    }

    /**
     * API lọc lịch sử theo khoảng ngày.
     * - from, to format: yyyy-MM-dd
     * - Nếu thiếu 1 trong 2 tham số → trả toàn bộ.
     */
    @GetMapping("/filter")
    public ResponseEntity<List<StockEntryResponse>> filterByDate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return ResponseEntity.ok(stockEntryService.filterByDate(from, to));
    }

    /**
     * API nhập kho bình thường.
     * - Chỉ cho phép quantity > 0
     */
    @PostMapping
    public ResponseEntity<StockEntryResponse> createNormalEntry(
            @Valid @RequestBody StockEntryCreateRequest req
    ) {
        return ResponseEntity.ok(stockEntryService.createNormalEntry(req));
    }

    /**
     * API điều chỉnh kho.
     * - Cho phép quantity âm/dương, nhưng != 0
     * - Không cho phép tồn kho bị âm sau điều chỉnh.
     */
    @PostMapping("/adjust")
    public ResponseEntity<StockEntryResponse> adjustStock(
            @Valid @RequestBody StockEntryCreateRequest req
    ) {
        return ResponseEntity.ok(stockEntryService.adjustStock(req));
    }
}
