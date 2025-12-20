package com.restaurant.api.dto.table;

import com.restaurant.api.enums.PosTableChangeReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * PosTableChangedMessage
 * ============================================================
 * Payload gửi realtime khi trạng thái bàn thay đổi.
 *
 * Dùng cho topic: /topic/tables
 */
@Getter
@AllArgsConstructor
public class PosTableChangedMessage {

    /**
     * ID của bàn bị thay đổi
     */
    private Long tableId;

    /**
     * Lý do thay đổi (enum)
     */
    private PosTableChangeReason reason;
}
