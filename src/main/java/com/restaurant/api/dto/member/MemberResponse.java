package com.restaurant.api.dto.member;

import com.restaurant.api.enums.MemberTier;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MemberResponse – DTO trả về cho FE khi xem thông tin hội viên
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {

    private Long id;

    private String name;
    private String phone;
    private String email;
    private LocalDate birthday;

    private MemberTier tier;
    private Boolean active;

    private Integer totalPoint;
    private Integer lifetimePoint;
    private Integer usedPoint;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
