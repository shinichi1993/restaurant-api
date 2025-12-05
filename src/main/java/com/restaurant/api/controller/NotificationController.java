package com.restaurant.api.controller;

import com.restaurant.api.dto.notification.CreateNotificationRequest;
import com.restaurant.api.dto.notification.NotificationResponse;
import com.restaurant.api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * NotificationController – Module 14 – Quản lý THÔNG BÁO
 * =====================================================================
 * Controller này chịu trách nhiệm:
 *
 *  1. Tạo thông báo mới (dùng nội bộ / Postman test)
 *  2. Lấy danh sách thông báo của 1 user
 *  3. Đánh dấu 1 thông báo là "đã đọc"
 *  4. Đánh dấu TẤT CẢ thông báo của user là "đã đọc"
 *
 * Lưu ý quan trọng:
 *  - Hiện tại để đơn giản cho việc test, API nhận userId qua query param.
 *    Sau này có thể nâng cấp:
 *      + Lấy userId từ JWT (SecurityContext)
 *      + Không cho phép truyền userId tự do từ FE.
 *
 *  - Toàn bộ comment dùng TIẾNG VIỆT theo Rule 13.
 * =====================================================================
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ============================================================
    // 1. TẠO THÔNG BÁO MỚI (DÙNG NỘI BỘ / POSTMAN TEST)
    // ============================================================

    /**
     * API: Tạo mới một thông báo và gán cho danh sách user.
     * -------------------------------------------------------------
     * Method: POST
     * URL   : /api/notifications
     *
     * Request body (JSON) – CreateNotificationRequest:
     * {
     *   "title": "Đơn hàng mới",
     *   "message": "Order #123 vừa được tạo",
     *   "type": "ORDER",
     *   "link": "/orders/123",
     *   "userIds": [1, 2, 3]      // có thể null → gửi cho toàn bộ user
     * }
     *
     * Dùng cho:
     *  - Test module Notification bằng Postman.
     *  - Sau này các service khác (Order, Stock...) có thể gọi nội bộ,
     *    hoặc refactor thành event.
     */
    @PostMapping
    public ResponseEntity<Void> createNotification(
            @RequestBody CreateNotificationRequest request
    ) {
        notificationService.createNotification(request);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // 2. LẤY DANH SÁCH THÔNG BÁO CỦA 1 USER
    // ============================================================

    /**
     * API: Lấy danh sách thông báo của 1 user.
     * -------------------------------------------------------------
     * Method: GET
     * URL   : /api/notifications?userId=1
     *
     * Tham số:
     *  - userId: ID của user (tạm thời truyền từ FE để dễ test).
     *
     * Kết quả:
     *  [
     *    {
     *      "id": 10,
     *      "title": "Đơn hàng mới",
     *      "message": "Order #123 vừa được tạo",
     *      "type": "ORDER",
     *      "link": "/orders/123",
     *      "status": "UNREAD",
     *      "createdAt": "2025-12-02T21:53:00",
     *      "readAt": null
     *    },
     *    ...
     *  ]
     *
     * Ghi chú:
     *  - Danh sách đã được sort DESC theo thời gian tạo (trong Service).
     *  - Sau này có thể thêm phân trang (page, size) nếu cần.
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotificationsForUser() {
        List<NotificationResponse> notifications =
                notificationService.getNotificationsForUser();

        return ResponseEntity.ok(notifications);
    }

    // ============================================================
    // 3. ĐÁNH DẤU ĐÃ ĐỌC 1 THÔNG BÁO
    // ============================================================

    /**
     * API: Đánh dấu 1 thông báo là "đã đọc" đối với user cụ thể.
     * -------------------------------------------------------------
     * Method: POST
     * URL   : /api/notifications/{id}/read?userId=1
     *
     * Tham số:
     *  - id     : ID thông báo (notificationId)
     *  - userId : ID user đang đọc thông báo
     *
     * Quy trình:
     *  - Tìm NotificationUserStatus theo (notification, user)
     *  - Nếu đang UNREAD → đổi sang READ + set readAt = now
     *  - Nếu đã READ rồi → không làm gì thêm (idempotent)
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable("id") Long notificationId
    ) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // 4. ĐÁNH DẤU ĐỌC TẤT CẢ THÔNG BÁO CỦA USER
    // ============================================================

    /**
     * API: Đánh dấu "đã đọc" TẤT CẢ thông báo của 1 user.
     * -------------------------------------------------------------
     * Method: POST
     * URL   : /api/notifications/read-all?userId=1
     *
     * Quy trình:
     *  - Tìm tất cả NotificationUserStatus có status = UNREAD
     *    của user tương ứng
     *  - Cập nhật:
     *      + status = READ
     *      + readAt = now
     *
     * Dùng cho:
     *  - Nút "Đánh dấu đã đọc tất cả" trên UI dropdown notification.
     */
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

    // =====================================================================
    // 4. Lấy danh sách thông báo chưa đọc
    // =====================================================================
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnread() {
        List<NotificationResponse> data = notificationService.getUnreadNotifications();
        return ResponseEntity.ok(data);
    }
}
