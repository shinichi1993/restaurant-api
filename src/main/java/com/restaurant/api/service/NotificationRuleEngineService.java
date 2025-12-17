package com.restaurant.api.service;

import com.restaurant.api.dto.notification.CreateNotificationRequest;
import com.restaurant.api.entity.Ingredient;
import com.restaurant.api.enums.NotificationType;
import com.restaurant.api.repository.IngredientRepository;
import com.restaurant.api.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * NotificationRuleEngineService – Phase 4.3
 * ======================================================================
 * Service chạy định kỳ để tạo thông báo theo RULE (scheduler):
 *  1) Cảnh báo tồn kho thấp (LOW STOCK)
 *  2) Cảnh báo X ngày không có doanh thu (REVENUE ZERO)
 *
 * Lưu ý:
 *  - Logic bật/tắt rule + bật/tắt toàn bộ notification đã nằm trong NotificationService,
 *    nên ở đây chỉ cần tạo CreateNotificationRequest và gọi createNotification().
 *  - Comment tiếng Việt theo Rule 13.
 * ======================================================================
 */
@Service
@RequiredArgsConstructor
public class NotificationRuleEngineService {

    private final IngredientRepository ingredientRepository;
    private final InvoiceRepository invoiceRepository;
    private final SystemSettingService systemSettingService;
    private final NotificationService notificationService;
    private final NotificationRuleLogService notificationRuleLogService;

    // ==================================================================
    // RULE 1: LOW STOCK
    // - Chạy mỗi 30 phút (có thể chỉnh)
    // - Nếu ingredient.stockQuantity <= threshold → tạo 1 thông báo tổng hợp
    // ==================================================================

    /**
     * Job cảnh báo tồn kho thấp.
     * ------------------------------------------------------------
     * - Key bật/tắt: notification.rule.low_stock.enabled (đã check trong NotificationService)
     * - Ngưỡng: notification.rule.low_stock.threshold (NUMBER)
     * - Tạo thông báo dạng tổng hợp để tránh spam.
     */
    @Transactional(readOnly = true)
    public void runLowStockWarning() {

        // Đọc thresholdraw (mặc định 10)
        BigDecimal thresholdRaw = systemSettingService.getNumberSetting(
                "notification.rule.low_stock.threshold",
                new BigDecimal("10")
        );

        // ✅ Biến dùng trong lambda PHẢI final
        final BigDecimal threshold =
                (thresholdRaw != null ? thresholdRaw : new BigDecimal("10"));

        // Lấy toàn bộ nguyên liệu
        List<Ingredient> all = ingredientRepository.findAll();

        // Lọc nguyên liệu tồn kho thấp
        List<Ingredient> lowStock = all.stream()
                .filter(i -> i.getActive() != null && i.getActive())
                .filter(i -> i.getStockQuantity() != null)
                .filter(i -> i.getStockQuantity().compareTo(threshold) <= 0)
                .toList();

        if (lowStock.isEmpty()) {
            return;
        }

        // ======================================================
        // CHỐNG SPAM – OPTION 1: CHẶN TẠO THÔNG BÁO LẶP TRONG NGÀY
        // ------------------------------------------------------
        // - ruleKey: LOW_STOCK
        // - payload: threshold + số lượng item lowStock (để khi thay đổi dữ liệu đáng kể vẫn có thể bắn)
        // - Nếu cùng ngày + payload giống nhau → không tạo lại
        // ======================================================
        String payload = "threshold=" + threshold + "|count=" + lowStock.size();
        if (!notificationRuleLogService.canTrigger("LOW_STOCK", payload)) {
            return;
        }

        // Build message tổng hợp
        StringBuilder sb = new StringBuilder();
        sb.append("Có ").append(lowStock.size()).append(" nguyên liệu tồn kho thấp (<= ")
                .append(threshold).append("). Ví dụ: ");

        // Lấy tối đa 5 item ví dụ để message không quá dài
        int limit = Math.min(5, lowStock.size());
        for (int i = 0; i < limit; i++) {
            Ingredient ing = lowStock.get(i);
            sb.append(ing.getName())
                    .append(" (")
                    .append(ing.getStockQuantity())
                    .append(" ")
                    .append(ing.getUnit())
                    .append(")");
            if (i < limit - 1) sb.append(", ");
        }

        if (lowStock.size() > limit) {
            sb.append("...");
        }

        CreateNotificationRequest req = new CreateNotificationRequest();
        req.setTitle("Cảnh báo tồn kho thấp");
        req.setType(NotificationType.STOCK);
        req.setMessage(sb.toString());
        req.setLink(""); // nếu sau này có trang lọc nguyên liệu tồn kho thấp thì set link

        // NotificationService sẽ tự check rule bật/tắt
        notificationService.createNotification(req);

        // ======================================================
        // GHI LOG RULE ĐÃ TRIGGER (để chống spam)
        // ======================================================
        notificationRuleLogService.markTriggered("LOW_STOCK", payload);
    }

    // ==================================================================
    // RULE 2: REVENUE ZERO
    // - Chạy mỗi ngày 08:00
    // - Nếu X ngày liên tiếp không có hóa đơn paid → tạo cảnh báo
    // ==================================================================

    /**
     * Job cảnh báo không có doanh thu.
     * ------------------------------------------------------------
     * - Key bật/tắt: notification.rule.revenue_zero.enabled (default false)
     * - Số ngày: notification.rule.revenue_zero.days (default 1)
     *
     * Lưu ý:
     * - Dựa vào invoice.paidAt != null để xác định có doanh thu.
     */
    @Transactional(readOnly = true)
    public void runRevenueZeroWarning() {

        boolean enabled = systemSettingService.getBooleanSetting(
                "notification.rule.revenue_zero.enabled",
                false
        );
        if (!enabled) {
            return; // chặn luôn để nhẹ DB (mặc dù NotificationService cũng sẽ chặn)
        }

        BigDecimal daysRaw = systemSettingService.getNumberSetting(
                "notification.rule.revenue_zero.days",
                new BigDecimal("1")
        );
        int days = (daysRaw != null) ? daysRaw.intValue() : 1;
        if (days <= 0) days = 1;

        // Kiểm tra từ hôm nay lùi lại (days) ngày
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(days);

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        // Nếu trong khoảng [from, to) có invoice paid → có doanh thu
        long paidCount = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getPaidAt() != null)
                .filter(inv -> !inv.getPaidAt().isBefore(from) && inv.getPaidAt().isBefore(to))
                .count();

        if (paidCount > 0) {
            return;
        }

        // ======================================================
        // CHỐNG SPAM – OPTION 1
        // ------------------------------------------------------
        // - ruleKey: REVENUE_ZERO
        // - payload: days (vì điều kiện chính là số ngày)
        // - Nếu cùng ngày + payload giống nhau → không tạo lại
        // ======================================================
        String payload = "days=" + days;
        if (!notificationRuleLogService.canTrigger("REVENUE_ZERO", payload)) {
            return;
        }

        CreateNotificationRequest req = new CreateNotificationRequest();
        req.setTitle("Cảnh báo doanh thu");
        req.setType(NotificationType.SYSTEM);
        req.setMessage("Không có doanh thu trong " + days + " ngày gần đây.");
        req.setLink("");

        notificationService.createNotification(req);
        // ======================================================
        // GHI LOG RULE ĐÃ TRIGGER
        // ======================================================
        notificationRuleLogService.markTriggered("REVENUE_ZERO", payload);
    }
}
