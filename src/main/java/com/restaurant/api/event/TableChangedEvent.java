package com.restaurant.api.event;

import com.restaurant.api.enums.PosTableChangeReason;

/**
 * TableChangedEvent
 * ------------------------------------------------------------
 * Domain event báo hiệu trạng thái bàn đã thay đổi.
 * Event này CHỈ dùng nội bộ, không gửi thẳng cho FE.
 */
public record TableChangedEvent(
        Long tableId,
        PosTableChangeReason reason
) {}
