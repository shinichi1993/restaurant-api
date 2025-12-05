package com.restaurant.api.service;

import com.restaurant.api.dto.notification.CreateNotificationRequest;
import com.restaurant.api.dto.notification.NotificationResponse;
import com.restaurant.api.entity.User;
import com.restaurant.api.entity.Notification;
import com.restaurant.api.entity.NotificationUserStatus;
import com.restaurant.api.enums.NotificationStatus;
import com.restaurant.api.repository.NotificationRepository;
import com.restaurant.api.repository.NotificationUserStatusRepository;
import com.restaurant.api.repository.UserRepository;
import com.restaurant.api.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.*;

/**
 * NotificationService
 * =====================================================================
 * Đây là service trung tâm xử lý tất cả nghiệp vụ thông báo:
 *
 *  - Tạo thông báo mới
 *  - Gán thông báo cho LIST user (theo userIds)
 *  - Nếu userIds = null → gán cho toàn bộ user
 *  - Lấy danh sách thông báo của user login
 *  - Đánh dấu đã đọc 1 thông báo
 *  - Đánh dấu đọc tất cả thông báo
 *
 * Tất cả đều tuân theo Rule 26 (data chuẩn) & Rule 13 (comment tiếng Việt)
 * =====================================================================
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationUserStatusRepository userStatusRepository;
    private final UserRepository userRepository; // bảng app_user
    private final UserService userService; // bảng app_user

    // ============================================================
    // 1. TẠO THÔNG BÁO CHUNG + GÁN CHO NHIỀU USER
    // ============================================================
    @Transactional
    public void createNotification(CreateNotificationRequest req) {

        // 1. Tạo bản ghi notification gốc
        Notification notification = Notification.builder()
                .title(req.getTitle())
                .message(req.getMessage())
                .type(req.getType())
                .link(req.getLink())
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        // 2. Xác định danh sách user nhận thông báo
        List<User> usersToNotify;

        if (req.getUserIds() == null || req.getUserIds().isEmpty()) {
            // Nếu NULL → gửi cho toàn bộ user
            usersToNotify = userRepository.findAll();
        } else {
            usersToNotify = userRepository.findAllById(req.getUserIds());
        }

        // 3. Tạo notification_user_status cho từng user
        List<NotificationUserStatus> list = new ArrayList<>();

        for (User user : usersToNotify) {
            NotificationUserStatus status = NotificationUserStatus.builder()
                    .notification(notification)
                    .user(user)
                    .status(NotificationStatus.UNREAD) // mặc định chưa đọc
                    .createdAt(LocalDateTime.now())
                    .build();
            list.add(status);
        }

        userStatusRepository.saveAll(list);
    }

    // ============================================================
    // 2. LẤY DANH SÁCH THÔNG BÁO THEO USER LOGIN
    // ============================================================
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser() {

        String username = AuthUtil.getCurrentUsername();
        Long userId = userService.getUserIdByUsername(username);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // lấy danh sách notification_user_status cho user
        List<NotificationUserStatus> statuses =
                userStatusRepository.findByUserOrderByCreatedAtDesc(user);

        List<NotificationResponse> result = new ArrayList<>();

        for (NotificationUserStatus s : statuses) {

            Notification n = s.getNotification();

            NotificationResponse dto = NotificationResponse.builder()
                    .id(n.getId())
                    .title(n.getTitle())
                    .message(n.getMessage())
                    .type(n.getType())
                    .link(n.getLink())
                    .createdAt(n.getCreatedAt())
                    .status(s.getStatus())
                    .readAt(s.getReadAt())
                    .build();

            result.add(dto);
        }

        return result;
    }

    // ============================================================
    // 3. ĐÁNH DẤU ĐÃ ĐỌC 1 THÔNG BÁO
    // ============================================================
    @Transactional
    public void markAsRead(Long notificationId) {

        String username = AuthUtil.getCurrentUsername();
        Long userId = userService.getUserIdByUsername(username);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        NotificationUserStatus status =
                userStatusRepository.findByNotificationAndUser(notification, user)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy trạng thái thông báo của user"));

        // chỉ update khi đang ở trạng thái UNREAD
        if (status.getStatus() == NotificationStatus.UNREAD) {
            status.setStatus(NotificationStatus.READ);
            status.setReadAt(LocalDateTime.now());
            userStatusRepository.save(status);
        }
    }

    // ============================================================
    // 4. ĐÁNH DẤU ĐỌC TẤT CẢ THÔNG BÁO
    // ============================================================
    @Transactional
    public void markAllAsRead() {

        String username = AuthUtil.getCurrentUsername();
        Long userId = userService.getUserIdByUsername(username);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        List<NotificationUserStatus> statuses =
                userStatusRepository.findByUserAndStatusOrderByCreatedAtDesc(
                        user, NotificationStatus.UNREAD
                );

        for (NotificationUserStatus s : statuses) {
            s.setStatus(NotificationStatus.READ);
            s.setReadAt(LocalDateTime.now());
        }

        userStatusRepository.saveAll(statuses);
    }

    /**
     * Lấy danh sách thông báo CHƯA ĐỌC của user hiện tại
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications() {

        // Lấy user hiện tại từ Security
        String username = AuthUtil.getCurrentUsername();
        Long userId = userService.getUserIdByUsername(username);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // ❗ Lấy từ bảng notification_user_status, KHÔNG lấy từ notification
        List<NotificationUserStatus> statuses =
                userStatusRepository.findByUserAndStatusOrderByCreatedAtDesc(
                        user,
                        NotificationStatus.UNREAD
                );

        return statuses.stream()
                .map(s -> {
                    Notification n = s.getNotification();
                    return NotificationResponse.builder()
                            .id(n.getId())
                            .title(n.getTitle())
                            .message(n.getMessage())
                            .type(n.getType())
                            .link(n.getLink())
                            .createdAt(n.getCreatedAt())
                            .status(s.getStatus())
                            .readAt(s.getReadAt())
                            .build();
                })
                .toList();
    }
}
