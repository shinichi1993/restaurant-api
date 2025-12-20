package com.restaurant.api.config;

import com.restaurant.api.security.UserDetailsServiceImpl;
import com.restaurant.api.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import com.restaurant.api.entity.User;
import com.restaurant.api.repository.UserRepository;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

import java.util.List;

/**
 * WebSocketAuthChannelInterceptor – Phase 5.1 / Step 2
 * =====================================================================================
 * Mục tiêu:
 *  - Xác thực (authenticate) cho kết nối WebSocket dựa trên JWT giống hệt REST API.
 *
 * Vì sao cần interceptor?
 *  - Với REST: request đi qua JwtFilter → set SecurityContext.
 *  - Với WebSocket: lúc CONNECT không đi qua JwtFilter REST như request HTTP bình thường,
 *    nên ta phải tự set Authentication tại thời điểm client CONNECT.
 *
 * Cách hoạt động:
 *  1) Client gửi frame CONNECT kèm header: Authorization: Bearer <token>
 *  2) Interceptor bắt CONNECT → lấy token
 *  3) Dùng lại logic validate + build Authentication (đang có ở REST)
 *  4) Gắn Authentication vào SecurityContextHolder + accessor.setUser(authentication)
 *
 * Lưu ý:
 *  - Dùng CHUNG JWT với REST, không tạo token riêng.
 *  - Nếu token không hợp lệ: không set auth (tuỳ policy, có thể throw để chặn CONNECT).
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * TODO (PHẢI MAP THEO PROJECT HIỆN TẠI):
     * ------------------------------------------------------------
     * Đây là "đầu mối" JWT của project m.
     * M có thể đang dùng:
     *  - JwtTokenProvider
     *  - JwtUtils
     *  - JwtService
     *  - AuthTokenFilter helper
     *
     * Yêu cầu: phải có hàm/logic để:
     *  - validate token
     *  - build Authentication từ token
     *
     * Cách lấy: copy đúng đoạn trong JwtFilter REST (phần C).
     */
    // private final JwtTokenProvider jwtTokenProvider; // Ví dụ
    // private final JwtUtils jwtUtils;                 // Ví dụ

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // Lấy accessor để đọc thông tin frame STOMP (CONNECT/SUBSCRIBE/SEND...)
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Chỉ xử lý đúng thời điểm client CONNECT
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            // Native header là header do client gửi trong STOMP CONNECT frame
            List<String> authHeaders = accessor.getNativeHeader("Authorization");

            // Nếu client không gửi Authorization thì bỏ qua (chưa set auth)
            if (authHeaders == null || authHeaders.isEmpty()) {
                return message;
            }

            String bearerToken = authHeaders.get(0);

            // Chuẩn header: "Bearer <token>"
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                return message;
            }

            String token = bearerToken.substring(7);

            // (1) Build Authentication từ token (COPY từ REST JWT filter)
            Authentication authentication = buildAuthenticationFromJwt(token);

            // Nếu token hợp lệ và build được Authentication → set SecurityContext
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Gắn user vào accessor để về sau có thể lấy accessor.getUser()
                accessor.setUser(authentication);
            }
        }

        return message;
    }

    /**
     * buildAuthenticationFromJwt – hàm “cầu nối” cho WebSocket
     * ------------------------------------------------------------
     * Mục tiêu:
     *  - Tái sử dụng 100% logic JWT của REST
     *  - Không viết lại decode/verify thủ công
     *
     * Cách implement:
     *  - Mở JwtFilter (REST) ra và copy phần:
     *      + validate token
     *      + getAuthentication(token)
     *  - Paste vào đây.
     *
     * Chính vì project m có thể đặt tên class JWT khác nhau,
     * nên t để hàm này làm chỗ map duy nhất.
     */
    private Authentication buildAuthenticationFromJwt(String token) {

        try {
            // 1. Lấy username từ JWT
            String username = jwtService.extractUsername(token);

            if (username == null) {
                return null;
            }

            // 2. Lấy user từ DB
            User user = userRepository.findByUsername(username).orElse(null);

            if (user == null) {
                return null;
            }

            // 3. Kiểm tra token hợp lệ
            if (!jwtService.isTokenValid(token, user)) {
                return null;
            }

            // 4. Load UserDetails (để lấy authorities/permissions)
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 5. Tạo Authentication giống hệt REST
            return new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

        } catch (Exception e) {
            // Token lỗi / hết hạn / parse lỗi
            return null;
        }
    }

}
