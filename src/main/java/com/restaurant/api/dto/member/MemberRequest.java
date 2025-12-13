package com.restaurant.api.dto.member;

import lombok.*;
import java.time.LocalDate;

/**
 * MemberRequest – DTO tạo/sửa hội viên
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRequest {

    private Long id;             // null khi tạo mới, != null khi cập nhật
    private String name;
    private String phone;
    private String email;
    private LocalDate birthday;
}
