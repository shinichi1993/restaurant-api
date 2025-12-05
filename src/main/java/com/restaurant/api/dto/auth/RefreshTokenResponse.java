package com.restaurant.api.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * DTO trả về khi refresh token
 * - Chỉ trả về accessToken mới
 */
@Data
@Builder
public class RefreshTokenResponse {

    private String accessToken; // Access token mới sau khi refresh
}
