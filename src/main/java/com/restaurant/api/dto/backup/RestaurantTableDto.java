package com.restaurant.api.dto.backup;

import com.restaurant.api.enums.TableStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * RestaurantTableDto – DTO backup/restore cho bàn nhà hàng
 * ------------------------------------------------------------------
 * Mapping cho entity RestaurantTable (Module 16)
 *
 * Lưu ý:
 *  - Chỉ backup dữ liệu cấu hình bàn
 *  - Không backup trạng thái runtime ngoài enum TableStatus
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantTableDto {

    private Long id;

    // Tên bàn (B1, B2, T1-B3...)
    private String name;

    // Sức chứa
    private Integer capacity;

    // Trạng thái bàn
    private TableStatus status;

    /**
     * mergedRootId:
     *  - Nếu bàn đã gộp, lưu ID bàn gốc
     *  - Null nếu không gộp
     */
    private Long mergedRootId;

    // Thời gian
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
