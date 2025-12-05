package com.restaurant.api.enums;

/**
 * SettingValueType
 * ----------------------------------------------------
 * - Kiểu dữ liệu logic của một cấu hình system_setting
 * - Giúp Service kiểm tra và convert giá trị cho đúng chuẩn
 */
public enum SettingValueType {
    STRING,
    NUMBER,
    BOOLEAN,
    JSON
}
