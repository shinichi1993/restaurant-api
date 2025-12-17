package com.restaurant.api.dto.backup;

import com.restaurant.api.enums.MemberTier;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MemberDto – DTO backup/restore cho entity Member
 * ------------------------------------------------------------------
 * Mục đích:
 *  - Backup dữ liệu hội viên dưới dạng DTO phẳng
 *  - Tránh dùng trực tiếp entity Member khi serialize JSON
 *
 * Mapping 1-1 với entity Member (Phase 3 – Loyalty)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDto {

    private Long id;

    // Thông tin cơ bản
    private String name;
    private String phone;
    private String email;

    // Trạng thái active (soft delete)
    private Boolean active;

    // Ngày sinh
    private LocalDate birthday;

    // Cấp hạng hội viên
    private MemberTier tier;

    // Điểm loyalty
    private Integer totalPoint;
    private Integer lifetimePoint;
    private Integer usedPoint;

    // Thời gian
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
