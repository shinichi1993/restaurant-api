package com.restaurant.api.dto.table;

import com.restaurant.api.enums.TableStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * TableResponse – Dữ liệu trả về cho FE khi lấy thông tin bàn.
 */
@Getter
@Setter
@Builder
public class TableResponse {

    private Long id;                // ID bàn
    private String name;            // Tên bàn
    private Integer capacity;       // Số ghế
    private TableStatus status;     // Trạng thái hiện tại
    private Long mergedRootId;      // ID bàn gốc nếu đã gộp
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
