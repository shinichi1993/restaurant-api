package com.restaurant.api.event;

import com.restaurant.api.entity.Order;
import com.restaurant.api.entity.OrderItem;
import lombok.Getter;

import java.util.List;

/**
 * OrderCreatedEvent
 * ------------------------------------------------------------
 * Event domain được phát ra khi tạo order thành công.
 * Event này sẽ được xử lý SAU KHI transaction commit.
 */
@Getter
public class OrderCreatedEvent {

    private final Order order;
    private final List<OrderItem> orderItems;
    private final boolean autoSendKitchen;

    public OrderCreatedEvent(Order order, List<OrderItem> orderItems, boolean autoSendKitchen) {
        this.order = order;
        this.orderItems = orderItems;
        this.autoSendKitchen = autoSendKitchen;
    }
}
