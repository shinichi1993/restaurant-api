package com.restaurant.api.dto.setting;

import lombok.Data;

/**
 * DTO nhận dữ liệu từ FE khi cập nhật danh sách cấu hình
 * ----------------------------------------------------
 * - FE sẽ gửi lên một mảng gồm nhiều cấu hình cần update
 * - Mỗi phần tử chứa id hoặc settingKey + settingValue mới
 */
@Data
public class SystemSettingUpdateRequest {

    /**
     * ID bản ghi system_setting.
     * Có thể null nếu FE chỉ gửi lên settingKey.
     */
    private Long id;

    /**
     * Khóa cấu hình (restaurant.name, vat.rate...).
     * Bắt buộc để tìm đúng record cần update.
     */
    private String settingKey;

    /**
     * Giá trị mới cần cập nhật, dạng text.
     * BE sẽ kiểm tra và convert dựa trên kiểu valueType hiện tại.
     */
    private String settingValue;
}
