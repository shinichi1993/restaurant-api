package com.restaurant.api.security;

import com.restaurant.api.entity.User;
import com.restaurant.api.enums.UserStatus;
import com.restaurant.api.repository.UserRepository;
import com.restaurant.api.service.PermissionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserDetailsServiceImpl
 * ---------------------------------------------------------------------
 * Spring Security dùng để load thông tin user + authorities (permission).
 *
 * Authorities = permission codes (VD: DISH_CREATE, REPORT_REVENUE...)
 * ---------------------------------------------------------------------
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermissionQueryService permissionQueryService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + username));

        // Nếu tài khoản bị khóa thì chặn luôn (đồng bộ với AuthService)
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new UsernameNotFoundException("Tài khoản đang bị khóa: " + username);
        }

        // Load permission codes từ DB
        List<String> permissions = permissionQueryService.getPermissionCodesByUsername(username);

        // Map sang GrantedAuthority
        List<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // Trả về UserDetails chuẩn của Spring
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}
