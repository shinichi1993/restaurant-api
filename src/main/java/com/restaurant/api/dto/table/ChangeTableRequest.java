package com.restaurant.api.dto.table;

import lombok.Getter;
import lombok.Setter;

/**
 * ChangeTableRequest – Request chuyển order từ bàn cũ sang bàn mới.
 */
@Getter
@Setter
public class ChangeTableRequest {

    private Long oldTableId; // Bàn hiện tại
    private Long newTableId; // Bàn muốn chuyển sang
}
