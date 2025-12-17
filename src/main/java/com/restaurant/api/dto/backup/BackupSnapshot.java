package com.restaurant.api.dto.backup;

import lombok.*;

import java.util.List;

/**
 * BackupSnapshot – Object gom toàn bộ dữ liệu backup
 * ------------------------------------------------------------
 * Mục tiêu:
 *  - Tạo cấu trúc dữ liệu rõ ràng
 *  - Serialize ra từng file JSON trong ZIP
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupSnapshot {
    private BackupMetadata metadata;

    // Security
    private List<RoleDto> roles;
    private List<PermissionDto> permissions;
    private List<RolePermissionDto> rolePermissions;
    private List<UserDto> users;
    private List<UserRoleDto> userRoles;

    // Loyalty
    private List<MemberDto> members;
    private List<MemberPointHistoryDto> memberPointHistories;

    // POS Table
    private List<RestaurantTableDto> tables;

    // Order flow
    private List<OrderDto> orders;
    private List<OrderItemDto> orderItems;

    // Invoice/Payment
    private List<InvoiceDto> invoices;
    private List<InvoiceItemDto> invoiceItems;
    private List<PaymentDto> payments;

    // Settings
    private List<SystemSettingDto> settings;

    // Notification/Audit
    private List<NotificationDto> notifications;
    private List<NotificationUserStatusDto> notificationUserStatuses;
    private List<AuditLogDto> auditLogs;
}
