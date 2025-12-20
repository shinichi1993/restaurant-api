package com.restaurant.api.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * TableChangedEventListener
 * ------------------------------------------------------------
 * Lắng nghe TableChangedEvent và bắn realtime
 * CHỈ SAU KHI TRANSACTION COMMIT THÀNH CÔNG.
 */
@Component
@RequiredArgsConstructor
public class TableChangedEventListener {

    private final RealtimeEventPublisher realtimeEventPublisher;

    /**
     * Bắn realtime POS Table sau khi DB đã commit.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TableChangedEvent event) {

        // Gửi payload xuống FE qua websocket
        realtimeEventPublisher.publishTable(event);
    }
}
