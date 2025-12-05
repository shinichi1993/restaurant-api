package com.restaurant.api.entity;

import com.restaurant.api.enums.SettingValueType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity SystemSetting
 * ----------------------------------------------------
 * - Đại diện cho bảng system_setting trong DB
 * - Dùng để lưu toàn bộ cấu hình động của hệ thống
 * - Các module khác (Order, Payment, POS, Report...)
 *   sẽ đọc cấu hình từ entity này thông qua service
 */
@Entity
@Table(name = "system_setting")
@Getter
@Setter
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nhóm cấu hình (RESTAURANT, POS, LOYALTY, REPORT, INVOICE...)
     * Dùng để hiển thị dạng tab / group trên FE.
     */
    @Column(name = "setting_group", nullable = false, length = 100)
    private String settingGroup;

    /**
     * Khóa cấu hình duy nhất, ví dụ:
     * - restaurant.name
     * - vat.rate
     * - pos.auto_send_kitchen
     */
    @Column(name = "setting_key", nullable = false, length = 150, unique = true)
    private String settingKey;

    /**
     * Giá trị cấu hình lưu dạng text.
     * BE sẽ tự convert sang kiểu dữ liệu phù hợp dựa trên valueType.
     */
    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    /**
     * Kiểu dữ liệu logic của cấu hình:
     * - STRING
     * - NUMBER
     * - BOOLEAN
     * - JSON
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 50)
    private SettingValueType valueType;

    /**
     * Mô tả ý nghĩa cấu hình, dùng để hiển thị trên màn hình Settings
     * giúp người dùng hiểu rõ tác dụng của từng setting.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Thời gian tạo bản ghi.
     * DB đã có default NOW(), nhưng mapping để đọc ra cho FE xem.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Thời gian cập nhật gần nhất.
     * Sẽ được set lại trong Service khi update cấu hình.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * ID người tạo cấu hình.
     * Tạm thời chưa sử dụng, sau có thể nối với bảng User.
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * ID người cập nhật cấu hình gần nhất.
     * Sau này có thể lấy từ user đang đăng nhập.
     */
    @Column(name = "updated_by")
    private Long updatedBy;
}
