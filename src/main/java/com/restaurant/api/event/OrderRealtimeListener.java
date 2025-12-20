package com.restaurant.api.event;

import com.restaurant.api.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Map;

/**
 * OrderRealtimeListener
 * ------------------------------------------------------------
 * Lắng nghe OrderCreatedEvent và bắn realtime
 * CHỈ SAU KHI transaction COMMIT thành công.
 */
@Component
@RequiredArgsConstructor
public class OrderRealtimeListener {

    private final RealtimeEventPublisher realtimeEventPublisher;

    /**
     * Bắn realtime cho bếp sau khi transaction commit
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {

        // 1) Realtime cho Order (LUÔN CÓ)
        realtimeEventPublisher.publishOrder(
                Map.of(
                        "event", "ORDER_CREATED",
                        "orderId", event.getOrder().getId()
                )
        );

        // 2) Realtime cho Kitchen (CHỈ KHI auto_send_kitchen)
        if (event.isAutoSendKitchen()) {
            for (OrderItem oi : event.getOrderItems()) {
                realtimeEventPublisher.publishKitchen(
                        Map.of(
                                "event", "ORDER_CREATED",
                                "orderItemId", oi.getId(),
                                "orderId", event.getOrder().getId()
                        )
                );
            }
        }
    }
}
