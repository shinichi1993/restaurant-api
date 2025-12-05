package com.restaurant.api.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * AuthUtil – tiện ích lấy thông tin user từ JWT
 * =====================================================
 * Dùng để:
 *  - Lấy userId từ token mà không cần FE truyền lên
 *
 * Yêu cầu:
 *  - Khi tạo JWT access token, phần "sub" hoặc "username"
 *    phải chứa username (email).
 */
public class AuthUtil {

    /**
     * Lấy username từ JWT (được Spring Security parse sẵn).
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;

        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }
}
