package com.restaurant.api.repository;

import com.restaurant.api.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn bảng system_setting
 * ----------------------------------------------------
 * - Cung cấp các method để tìm kiếm setting theo key, group
 * - Các module khác sẽ dùng service, không gọi repository trực tiếp
 */
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    /**
     * Tìm cấu hình theo key.
     */
    Optional<SystemSetting> findBySettingKey(String settingKey);

    /**
     * Lấy danh sách cấu hình theo group (RESTAURANT, POS, LOYALTY...).
     */
    List<SystemSetting> findBySettingGroup(String settingGroup);
}
