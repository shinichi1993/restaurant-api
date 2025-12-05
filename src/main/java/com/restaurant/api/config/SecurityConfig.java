package com.restaurant.api.config;

import com.restaurant.api.security.UserDetailsServiceImpl;
import com.restaurant.api.security.filter.CustomAuthEntryPoint;
import com.restaurant.api.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig – Cấu hình bảo mật cho hệ thống
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;
    // Xử lý lỗi Authentication (401 Unauthorized)
    // Được sử dụng trong exceptionHandling()
    private final CustomAuthEntryPoint customAuthEntryPoint;

    /**
     * Bean mã hóa mật khẩu dùng BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provider xác thực sử dụng UserDetailsService + PasswordEncoder
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Bean AuthenticationManager dùng cho AuthService
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Cấu hình SecurityFilterChain chính
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Bật CORS và dùng cấu hình từ corsConfigurationSource()
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Tắt CSRF vì dùng JWT (không dùng session)
                .csrf(csrf -> csrf.disable())

                // Session stateless vì dùng JWT
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Phân quyền API
                .authorizeHttpRequests(auth -> auth
                        // Cho phép OPTIONS cho tất cả request → rất quan trọng
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Cho phép auth API
                        .requestMatchers(
                                "/api/auth/**",
                                "/actuator/health"
                        ).permitAll()

                        // Tất cả còn lại cần auth
                        .anyRequest().authenticated()
                )

                // Xử lý lỗi auth
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthEntryPoint)
                )

                // Gắn filter JWT vào trước UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Cấu hình CORS cho toàn bộ API – cho phép FE localhost:5173 gọi sang
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // FE đang chạy ở http://localhost:5173
        config.setAllowedOrigins(List.of(
                "https://restaurant-fe-production.up.railway.app",
                "http://localhost:5173"));

        // Các method FE sẽ dùng
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Các header được phép gửi kèm (quan trọng: Authorization)
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // Header trả về cho FE
        config.setExposedHeaders(List.of("Authorization"));

        // Cho phép gửi kèm cookie / thông tin xác thực nếu sau này cần
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

}
