package com.restaurant.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity RefreshToken – Lưu refresh token cho từng user
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // User sở hữu token

    @Column(nullable = false, columnDefinition = "TEXT")
    private String token; // Chuỗi refresh token

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate; // Thời điểm hết hạn
}
