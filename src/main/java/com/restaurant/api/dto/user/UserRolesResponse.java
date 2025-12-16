package com.restaurant.api.dto.user;

import lombok.*;

import java.util.List;

/**
 * UserRolesResponse
 * ------------------------------------------------------------------
 * Response trả về danh sách role codes của user.
 * ------------------------------------------------------------------
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRolesResponse {
    private Long userId;
    private String username;
    private List<String> roles; // VD: ["ADMIN","STAFF"]
}
