package com.restaurant.api.service;

import com.restaurant.api.dto.notification.CreateNotificationRequest;
import com.restaurant.api.entity.Ingredient;
import com.restaurant.api.entity.Order;
import com.restaurant.api.entity.OrderItem;
import com.restaurant.api.entity.Payment;
import com.restaurant.api.enums.NotificationType;
import com.restaurant.api.enums.OrderStatus;
import com.restaurant.api.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * NotificationRuleService – Phase 4.3
 * =====================================================================
 * Mục tiêu:
 *  - Là “Rule Engine” trung tâm cho thông báo.
 *  - Các nghiệp vụ Order/Payment/Stock KHÔNG gọi NotificationService trực tiếp nữa.
 *  - Tất cả thông báo phải đi qua đây để:
 *      + Bật/tắt theo SystemSetting
 *      + Chuẩn hóa title/message/type/link
 *      + Chống spam (dedup)
 *
 * Lưu ý:
 *  - Không tạo bảng mới cho rule (dùng SystemSettingService).
 *  - Toàn bộ comment tiếng Việt (Rule 13).
 * =====================================================================
 */
@Service
@RequiredArgsConstructor
public class NotificationRuleService {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final SystemSettingService systemSettingService;

    // ================================================================
    // Helper đọc setting
    // ================================================================

    private boolean isEnabled(String key, boolean defaultValue) {
        return systemSettingService.getBooleanSetting(key, defaultValue);
    }

    private BigDecimal getNumber(String key, BigDecimal defaultValue) {
        return systemSettingService.getNumberSetting(key, defaultValue);
    }

    /**
     * Chống spam:
     * - Nếu trong vòng N giờ đã có notification cùng title → bỏ qua.
     * - N mặc định = 24h
     */
    private boolean isDuplicatedTitleInHours(String title) {
        int hours = getNumber("notification.rule.dedup.hours", new BigDecimal("24")).intValue();
        if (hours <= 0) {
            // Nếu cấu hình <= 0 → coi như không dedup
            return false;
        }
        LocalDateTime from = LocalDateTime.now().minusHours(hours);
        return notificationRepository.existsByTitleAndCreatedAtAfter(title, from);
    }

    /**
     * Hàm tạo notification chuẩn hoá.
     * - Gửi cho toàn bộ user (userIds = null) theo thiết kế hiện tại của bạn.
     */
    @Transactional
    public void push(NotificationType type, String title, String message, String link) {

        if (title == null || title.trim().isEmpty()) return;
        if (message == null) message = "";

        // Dedup theo title (giảm spam)
        if (isDuplicatedTitleInHours(title)) {
            return;
        }

        CreateNotificationRequest req = new CreateNotificationRequest();
        req.setTitle(title);
        req.setType(type);
        req.setMessage(message);
        req.setLink(link);

        // userIds = null → NotificationService tự gửi cho toàn bộ user
        req.setUserIds(null);

        notificationService.createNotification(req);
    }

    // ================================================================
    // RULE 1: ORDER CREATED
    // ================================================================
    @Transactional
    public void onOrderCreated(Order order, List<OrderItem> items) {

        if (!isEnabled("notification.rule.order_created.enabled", true)) return;
        if (order == null) return;

        String title = "Order mới: " + safe(order.getOrderCode(), "#" + order.getId());
        String message = "Đơn hàng vừa được tạo. Tổng tiền: " + safeMoney(order.getTotalPrice());
        String link = "/orders/" + order.getId();

        push(NotificationType.ORDER, title, message, link);
    }

    // ================================================================
    // RULE 2: ORDER STATUS CHANGED
    // ================================================================
    @Transactional
    public void onOrderStatusChanged(Order order, OrderStatus oldStatus, OrderStatus newStatus) {

        if (!isEnabled("notification.rule.order_status_changed.enabled", true)) return;
        if (order == null || newStatus == null) return;

        // Ví dụ: chỉ bắn khi CANCELED hoặc PAID (tuỳ bạn)
        if (newStatus != OrderStatus.CANCELED && newStatus != OrderStatus.PAID) {
            return;
        }

        String title = "Order cập nhật: " + safe(order.getOrderCode(), "#" + order.getId());
        String message = "Trạng thái: " + safe(oldStatus) + " → " + safe(newStatus);
        String link = "/orders/" + order.getId();

        push(NotificationType.ORDER, title, message, link);
    }

    // ================================================================
    // RULE 3: PAYMENT SUCCESS
    // ================================================================
    @Transactional
    public void onPaymentSuccess(Payment payment) {

        if (!isEnabled("notification.rule.payment_success.enabled", true)) return;
        if (payment == null || payment.getOrder() == null) return;

        Long orderId = payment.getOrder().getId();

        String title = "Thanh toán thành công";
        String message = "Order #" + orderId + " đã thanh toán. Số tiền: " + safeMoney(payment.getAmount());
        String link = "/invoices/" + (payment.getInvoice() != null ? payment.getInvoice().getId() : "");

        push(NotificationType.PAYMENT, title, message, link);
    }

    // ================================================================
    // RULE 4: LOW STOCK – bắn theo từng nguyên liệu
    // ================================================================
    @Transactional
    public void onLowStock(List<Ingredient> lowIngredients, BigDecimal threshold) {

        if (!isEnabled("notification.rule.stock_low.enabled", true)) return;
        if (lowIngredients == null || lowIngredients.isEmpty()) return;

        // Chỉ tạo 1 thông báo tổng hợp để giảm spam
        String title = "Cảnh báo tồn kho thấp";
        StringBuilder sb = new StringBuilder();
        sb.append("Có ").append(lowIngredients.size()).append(" nguyên liệu tồn kho <= ")
                .append(threshold != null ? threshold.toPlainString() : "ngưỡng").append(". ");

        // Liệt kê tối đa 5 cái cho ngắn
        int limit = Math.min(5, lowIngredients.size());
        sb.append("Ví dụ: ");
        for (int i = 0; i < limit; i++) {
            Ingredient ing = lowIngredients.get(i);
            sb.append(ing.getName())
                    .append(" (")
                    .append(ing.getStockQuantity() != null ? ing.getStockQuantity().toPlainString() : "0")
                    .append(" ")
                    .append(ing.getUnit() != null ? ing.getUnit() : "")
                    .append(")");
            if (i < limit - 1) sb.append(", ");
        }
        if (lowIngredients.size() > limit) sb.append("...");

        String link = "/ingredients";

        push(NotificationType.STOCK, title, sb.toString(), link);
    }

    // ================================================================
    // RULE 5: REVENUE DAILY – doanh thu ngày hôm qua
    // ================================================================
    @Transactional
    public void onRevenueDailyCheck(BigDecimal yesterdayRevenue, BigDecimal minAmount) {

        if (!isEnabled("notification.rule.revenue_daily.enabled", true)) return;

        BigDecimal revenue = (yesterdayRevenue != null ? yesterdayRevenue : BigDecimal.ZERO);
        BigDecimal min = (minAmount != null ? minAmount : BigDecimal.ZERO);

        // Nếu doanh thu >= min → không bắn
        if (revenue.compareTo(min) >= 0) return;

        String title = "Cảnh báo doanh thu thấp";
        String message = "Doanh thu ngày hôm qua: " + safeMoney(revenue) +
                " (ngưỡng tối thiểu: " + safeMoney(min) + ").";
        String link = "/reports/revenue";

        push(NotificationType.SYSTEM, title, message, link);
    }

    // ================================================================
    // Helper format
    // ================================================================
    private String safe(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private String safe(String a, String b) {
        return (a != null && !a.trim().isEmpty()) ? a : b;
    }

    private String safeMoney(BigDecimal v) {
        BigDecimal x = (v != null ? v : BigDecimal.ZERO);
        // Format đơn giản, không phụ thuộc locale
        return x.setScale(0, BigDecimal.ROUND_HALF_UP).toPlainString() + " đ";
    }
}
