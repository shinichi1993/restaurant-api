package com.restaurant.api.entity;

import com.restaurant.api.enums.MemberTier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity Member – Lưu thông tin hội viên Loyalty
 * ------------------------------------------------------------
 * Các trường chính:
 *  - name, phone, email, birthday
 *  - totalPoint     : điểm khả dụng hiện tại
 *  - lifetimePoint  : tổng điểm đã tích từ trước đến giờ
 *  - usedPoint      : tổng điểm đã dùng
 *  - tier           : cấp hạng (BRONZE / SILVER / GOLD / PLATINUM)
 */
@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thông tin cơ bản
    @Column(length = 255)
    private String name;

    @Column(length = 50, unique = true)
    private String phone;

    @Column(length = 255)
    private String email;

    // trạng thái active để soft-delete
    @Column(name = "active", nullable = false)
    private Boolean active = true; // mặc định là active

    /**
     * Ngày sinh của hội viên – dùng để tặng voucher sinh nhật.
     */
    private LocalDate birthday;

    /**
     * Cấp hạng hội viên:
     *  - BRONZE / SILVER / GOLD / PLATINUM
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private MemberTier tier;

    /**
     * totalPoint – Điểm hiện tại có thể dùng để đổi.
     */
    private Integer totalPoint;

    /**
     * lifetimePoint – Tổng điểm đã tích từ trước đến giờ.
     */
    private Integer lifetimePoint;

    /**
     * usedPoint – Tổng điểm đã sử dụng để đổi thưởng.
     */
    private Integer usedPoint;

    /**
     * Thời gian tạo bản ghi.
     */
    private LocalDateTime createdAt;

    /**
     * Thời gian cập nhật gần nhất.
     */
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (totalPoint == null) totalPoint = 0;
        if (lifetimePoint == null) lifetimePoint = 0;
        if (usedPoint == null) usedPoint = 0;
        if (tier == null) tier = MemberTier.BRONZE;
        if (active == null) active = true; // ✅ mặc định hội viên đang hoạt động
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
