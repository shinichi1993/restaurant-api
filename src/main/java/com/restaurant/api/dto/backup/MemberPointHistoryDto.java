package com.restaurant.api.dto.backup;

import lombok.*;

import java.time.LocalDateTime;

/**
 * MemberPointHistoryDto – DTO backup/restore lịch sử điểm
 * ------------------------------------------------------------------
 * Mapping cho entity MemberPointHistory
 *
 * Lưu ý:
 *  - Không dùng @ManyToOne
 *  - Chỉ lưu memberId để restore nhanh, tránh join nặng
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberPointHistoryDto {

    private Long id;

    // Hội viên liên quan
    private Long memberId;

    // Số điểm thay đổi (+ / -)
    private Integer changeAmount;

    // Số dư sau thay đổi
    private Integer balanceAfter;

    /**
     * Loại biến động:
     *  - EARN
     *  - REDEEM
     *  - ADJUST
     */
    private String type;

    // Mô tả nguyên nhân
    private String description;

    // Gắn với order / invoice (nếu có)
    private Long orderId;

    // Thời gian tạo bản ghi
    private LocalDateTime createdAt;
}
