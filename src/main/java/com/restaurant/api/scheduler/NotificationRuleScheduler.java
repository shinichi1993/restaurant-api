package com.restaurant.api.scheduler;

import com.restaurant.api.dto.report.RevenueReportResponse;
import com.restaurant.api.entity.Ingredient;
import com.restaurant.api.repository.IngredientRepository;
import com.restaurant.api.service.NotificationRuleEngineService;
import com.restaurant.api.service.NotificationRuleService;
import com.restaurant.api.service.ReportService;
import com.restaurant.api.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * NotificationRuleScheduler – Phase 4.3
 * =====================================================================
 * Các rule chạy nền:
 *  1) Low stock: kiểm tra tồn kho thấp
 *  2) Revenue daily: kiểm tra doanh thu ngày hôm qua
 *
 * Lưu ý:
 *  - Dùng SystemSettingService để bật/tắt rule & chỉnh threshold
 *  - Toàn bộ comment tiếng Việt (Rule 13)
 * =====================================================================
 */
@Component
@RequiredArgsConstructor
public class NotificationRuleScheduler {

    private final IngredientRepository ingredientRepository;
    private final SystemSettingService systemSettingService;
    private final NotificationRuleService notificationRuleService;
    private final ReportService reportService;
    private final NotificationRuleEngineService notificationRuleEngineService;

    /**
     * Rule: LOW STOCK
     * -------------------------------------------------------------
     * Chạy mỗi 30 phút.
     * - Threshold đọc từ setting: notification.rule.stock_low.threshold (default 10)
     * - Lọc ingredient active=true (nếu entity có field active)
     *   -> vì bạn chưa gửi entity Ingredient ở đây, nên mình lọc mềm:
     *      + nếu không có active, vẫn chạy được (lọc theo stockQuantity thôi)
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void runLowStockCheck() {
        notificationRuleEngineService.runLowStockWarning();
    }

    /**
     * Rule: REVENUE DAILY CHECK
     * -------------------------------------------------------------
     * Chạy mỗi ngày lúc 08:00 sáng.
     * - Lấy doanh thu của NGÀY HÔM QUA theo ReportService (source of truth)
     * - Nếu revenue < min_amount → tạo thông báo
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void runRevenueDailyCheck() {
        notificationRuleEngineService.runRevenueZeroWarning();
    }
}
