package com.restaurant.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity MemberPointHistory – Lịch sử cộng/trừ điểm của hội viên
 * ----------------------------------------------------------------
 * Dùng để truy vết toàn bộ biến động điểm:
 *  - Khi tích điểm (EARN)
 *  - Khi dùng điểm (REDEEM)
 *  - Khi admin điều chỉnh (ADJUST)
 */
@Entity
@Table(name = "member_point_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberPointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID hội viên liên quan.
     * Không dùng @ManyToOne để tránh join nặng, chỉ lưu memberId.
     */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /**
     * changeAmount – Số điểm thay đổi:
     *  - > 0 : cộng điểm
     *  - < 0 : trừ điểm
     */
    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount;

    /**
     * balanceAfter – Tổng điểm còn lại sau khi áp dụng changeAmount.
     */
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    /**
     * type – Loại biến động:
     *  - EARN   : Tích điểm
     *  - REDEEM : Dùng điểm
     *  - ADJUST : Điều chỉnh thủ công
     */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    /**
     * Mô tả ngắn gọn nguyên nhân thay đổi điểm.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * orderId – Nếu có gắn với 1 hóa đơn / order cụ thể.
     */
    @Column(name = "order_id")
    private Long orderId;

    /**
     * Thời gian tạo bản ghi lịch sử.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
