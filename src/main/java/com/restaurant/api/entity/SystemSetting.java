package com.restaurant.api.entity;

import com.restaurant.api.enums.SettingValueType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.math.BigDecimal;

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
     */
    @Column(name = "setting_group", nullable = false, length = 100)
    private String settingGroup;

    /**
     * Nhóm cấu hình (RESTAURANT, POS, LOYALTY, REPORT, INVOICE...)
     * Dùng để hiển thị dạng tab / group trên FE.
     */
    @Column(name = "setting_group_label ", nullable = false, length = 100)
    private String settingGroupLabel;

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

    /**
     * Tên hiển thị trên FE (label).
     * - Dùng để FE render động, không hard-code label nữa.
     */
    @Column(name = "label", length = 255)
    private String label;

    /**
     * Kiểu input FE sẽ render:
     * - INPUT, NUMBER, SWITCH, SELECT (mở rộng sau)
     */
    @Column(name = "input_type", length = 30)
    private String inputType;

    /**
     * Thứ tự hiển thị trong group/tab.
     */
    @Column(name = "order_index")
    private Integer orderIndex;

    /**
     * Min/Max cho input kiểu NUMBER (nếu cần).
     */
    @Column(name = "min_value", precision = 19, scale = 4)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 19, scale = 4)
    private BigDecimal maxValue;

    /**
     * Ẩn/hiện setting trên FE.
     */
    @Column(name = "visible")
    private Boolean visible;

    /**
     * Cho phép chỉnh sửa trên FE (disable input nếu false).
     */
    @Column(name = "editable")
    private Boolean editable;

    /**
     * Dependency hiển thị:
     * - Nếu dependsOnKey != null thì setting chỉ hiển thị khi
     *   giá trị của dependsOnKey == dependsOnValue
     *
     * Ví dụ:
     * - pos.simple_pos_require_table phụ thuộc pos.simple_pos_mode = true
     */
    @Column(name = "depends_on_key", length = 150)
    private String dependsOnKey;

    @Column(name = "depends_on_value", length = 50)
    private String dependsOnValue;
}
