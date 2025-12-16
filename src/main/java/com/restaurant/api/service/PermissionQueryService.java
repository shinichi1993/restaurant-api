package com.restaurant.api.service;

import com.restaurant.api.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PermissionQueryService
 * --------------------------------------------------
 * Service truy vấn permission của user
 *
 * MỤC ĐÍCH THIẾT KẾ:
 * - Phục vụ Spring Security (JwtFilter, UserDetailsService)
 * - KHÔNG load entity Permission (tránh Lazy)
 * - CHỈ trả về List<String> permission codes
 *
 * Áp dụng cho:
 * - Module 4.1 – Permission & Role Advanced
 * - Menu FE
 * - PermissionRoute
 * - usePermission hook
 * --------------------------------------------------
 */
@Service
@RequiredArgsConstructor
public class PermissionQueryService {

    private final UserRoleRepository userRoleRepository;

    /**
     * Lấy danh sách permission code của user theo username
     *
     * @param username username lấy từ JWT (subject)
     * @return danh sách permission code (VD: DISH_VIEW, REPORT_REVENUE)
     *
     * ⚠️ BẮT BUỘC:
     * - @Transactional để đảm bảo query chạy trong session
     * - Trả về String → không gây LazyInitializationException
     */
    @Transactional(readOnly = true)
    public List<String> getPermissionCodesByUsername(String username) {
        return userRoleRepository.findPermissionCodesByUsername(username);
    }
}
