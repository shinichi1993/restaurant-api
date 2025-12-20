package com.restaurant.api.event;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * RealtimeEventPublisher – Phase 5.1 / Step 3
 * ============================================================
 * Class trung tâm để bắn sự kiện realtime qua WebSocket.
 *
 * Mục đích:
 *  - Không dùng SimpMessagingTemplate trực tiếp trong Service
 *  - Chuẩn hóa các topic realtime
 *  - Dễ refactor sang RabbitMQ/Kafka trong tương lai
 *
 * Nguyên tắc sử dụng:
 *  - Service gọi RealtimeEventPublisher
 *  - Không publish rải rác ở nhiều nơi
 */
@Component
@RequiredArgsConstructor
public class RealtimeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Bắn realtime cho Order
     * @param payload dữ liệu order gửi xuống client
     */
    public void publishOrder(Object payload) {
        messagingTemplate.convertAndSend("/topic/orders", payload);
    }

    /**
     * Bắn realtime cho OrderItem (món)
     * @param payload dữ liệu order item
     */
    public void publishOrderItem(Object payload) {
        messagingTemplate.convertAndSend("/topic/order-items", payload);
    }

    /**
     * Bắn realtime cho màn hình bếp
     * @param payload dữ liệu gửi bếp
     */
    public void publishKitchen(Object payload) {
        messagingTemplate.convertAndSend("/topic/kitchen", payload);
    }

    /**
     * Bắn realtime cho Notification
     * @param payload dữ liệu thông báo
     */
    public void publishNotification(Object payload) {
        messagingTemplate.convertAndSend("/topic/notifications", payload);
    }

    /**
     * Bắn realtime cho POS Table (Phase 5.3.5)
     * ------------------------------------------------------------
     * Dùng để thông báo rằng trạng thái bàn có thay đổi
     * (order tạo / thanh toán xong / thay đổi món quan trọng)
     */
    public void publishTable(Object payload) {
        messagingTemplate.convertAndSend("/topic/tables", payload);
    }
}
