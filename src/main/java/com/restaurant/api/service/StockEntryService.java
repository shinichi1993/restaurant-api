package com.restaurant.api.service;

import com.restaurant.api.dto.notification.CreateNotificationRequest;
import com.restaurant.api.dto.stockentry.StockEntryCreateRequest;
import com.restaurant.api.dto.stockentry.StockEntryResponse;
import com.restaurant.api.entity.Ingredient;
import com.restaurant.api.entity.StockEntry;
import com.restaurant.api.enums.AuditAction;
import com.restaurant.api.enums.NotificationType;
import com.restaurant.api.repository.IngredientRepository;
import com.restaurant.api.repository.StockEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * StockEntryService
 * ----------------------------------------------------------
 * Xử lý nghiệp vụ nhập kho và điều chỉnh kho nguyên liệu:
 *  - Nhập kho bình thường (chỉ cho số dương)
 *  - Điều chỉnh kho (cho phép số âm/dương, nhưng không cho tồn < 0)
 *  - Lấy danh sách lịch sử nhập kho / điều chỉnh
 *  - Lọc lịch sử theo khoảng ngày
 * ----------------------------------------------------------
 * Toàn bộ comment theo Rule 13 (tiếng Việt đầy đủ).
 */
@Service
@RequiredArgsConstructor
public class StockEntryService {

    private final StockEntryRepository stockEntryRepository;
    private final IngredientRepository ingredientRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    /**
     * Lấy toàn bộ lịch sử nhập kho / điều chỉnh,
     * sắp xếp theo thời gian mới nhất.
     */
    public List<StockEntryResponse> getAll() {
        return stockEntryRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lọc lịch sử nhập kho theo khoảng ngày [fromDate, toDate].
     * - Nếu 1 trong 2 tham số null → trả toàn bộ.
     */
    public List<StockEntryResponse> filterByDate(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            return getAll();
        }

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        return stockEntryRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Nhập kho bình thường:
     * - Chỉ cho phép quantity > 0
     * - Cộng tồn kho: ingredient.stockQuantity += quantity
     * - Lưu lịch sử vào stock_entry
     */
    @Transactional
    public StockEntryResponse createNormalEntry(StockEntryCreateRequest req) {

        if (req.getQuantity() == null || req.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số lượng nhập kho phải lớn hơn 0");
        }

        Ingredient ingredient = ingredientRepository.findById(req.getIngredientId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu"));

        // Cộng tồn kho
        BigDecimal newStock = ingredient.getStockQuantity().add(req.getQuantity());
        ingredient.setStockQuantity(newStock);
        ingredientRepository.save(ingredient);

        // Lưu phiếu nhập kho
        StockEntry entry = StockEntry.builder()
                .ingredient(ingredient)
                .quantity(req.getQuantity())
                .note(req.getNote() != null ? req.getNote() : "Nhập kho")
                .build();

        StockEntry saved = stockEntryRepository.save(entry);

        // =====================================================================
        // GỬI THÔNG BÁO: Nhập kho nguyên liệu
        // =====================================================================
        CreateNotificationRequest re = new CreateNotificationRequest();
        re.setTitle("Nhập kho");
        re.setType(NotificationType.STOCK);
        re.setMessage("Nhập kho nguyên liệu");
        re.setLink("");
        notificationService.createNotification(re);

        // ✅ Audit log
        auditLogService.log(
                AuditAction.STOCK_ENTRY_CREATE,
                "stock_entry",
                entry.getId(),
                null,
                entry
        );

        return toResponse(entry);
    }

    /**
     * Điều chỉnh kho:
     * - Cho phép quantity âm hoặc dương, nhưng != 0
     * - Tính tồn kho mới = tồn hiện tại + quantity
     * - Nếu tồn mới < 0 → báo lỗi
     */
    @Transactional
    public StockEntryResponse adjustStock(StockEntryCreateRequest req) {

        if (req.getQuantity() == null || req.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("Số lượng điều chỉnh phải khác 0");
        }

        Ingredient ingredient = ingredientRepository.findById(req.getIngredientId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu"));

        BigDecimal currentStock = ingredient.getStockQuantity() != null ? ingredient.getStockQuantity() : BigDecimal.ZERO;
        BigDecimal newStock = currentStock.add(req.getQuantity());

        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Tồn kho không đủ để điều chỉnh, không được âm");
        }

        // Cập nhật tồn kho mới
        ingredient.setStockQuantity(newStock);
        ingredientRepository.save(ingredient);

        // Lưu phiếu điều chỉnh
        String note = req.getNote();
        if (note == null || note.isBlank()) {
            note = "Điều chỉnh kho";
        }

        StockEntry entry = StockEntry.builder()
                .ingredient(ingredient)
                .quantity(req.getQuantity())
                .note(note)
                .build();

        StockEntry saved = stockEntryRepository.save(entry);

        // =====================================================================
        // GỬI THÔNG BÁO: Điều chỉnh kho nguyên liệu
        // =====================================================================
        CreateNotificationRequest re = new CreateNotificationRequest();
        re.setTitle("Điều chỉnh kho nguyên liệu");
        re.setType(NotificationType.STOCK);
        re.setMessage("Điều chỉnh kho nguyên liệu");
        re.setLink("");
        notificationService.createNotification(re);

        stockEntryRepository.save(entry);

        // ✅ Audit log
        auditLogService.log(
                AuditAction.STOCK_ENTRY_UPDATE,
                "stock_entry",
                entry.getId(),
                null,
                entry
        );

        return toResponse(entry);
    }

    /**
     * Hàm convert Entity → DTO trả cho FE.
     */
    private StockEntryResponse toResponse(StockEntry entry) {
        return StockEntryResponse.builder()
                .id(entry.getId())
                .ingredientId(entry.getIngredient().getId())
                .ingredientName(entry.getIngredient().getName())
                .quantity(entry.getQuantity())
                .note(entry.getNote())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
