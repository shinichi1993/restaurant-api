package com.restaurant.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.restaurant.api.dto.backup.*;
import com.restaurant.api.entity.*;
import com.restaurant.api.repository.*;
import com.restaurant.api.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * BackupService – Phase 4.4
 * ------------------------------------------------------------
 * Nhiệm vụ:
 *  - Export dữ liệu toàn hệ thống ra từng file JSON
 *  - Nén thành 1 file ZIP (download về)
 *
 * Lưu ý quan trọng:
 *  - Không export trực tiếp entity có quan hệ LAZY để tránh vòng lặp
 *  - Dùng DTO theo dạng "phẳng" (id, fkId) để restore an toàn
 */
@Service
@RequiredArgsConstructor
public class BackupService {

    private final ObjectMapper objectMapper;

    // Security
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    // Settings
    private final SystemSettingRepository systemSettingRepository;

    // POS/Table
    private final RestaurantTableRepository restaurantTableRepository;

    // Order flow
    private final DishRepository dishRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    // Invoice/Payment
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PaymentRepository paymentRepository;

    // Notification/Audit
    private final NotificationRepository notificationRepository;
    private final NotificationUserStatusRepository notificationUserStatusRepository;
    private final AuditLogRepository auditLogRepository;

    // Loyalty
    private final MemberRepository memberRepository;
    private final MemberPointHistoryRepository memberPointHistoryRepository;

    /**
     * Tạo file backup ZIP (byte[])
     */
    public byte[] exportBackupZip() {
        try {
            // 🔐 Lấy username trực tiếp từ JWT (SecurityContext)
            String username = AuthUtil.getCurrentUsername();
            if (username == null) {
                throw new IllegalStateException("Không xác định được user từ JWT");
            }

            // Đảm bảo serialize LocalDateTime ổn định
            objectMapper.registerModule(new JavaTimeModule());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {

                // 1) metadata.json
                BackupMetadata meta = BackupMetadata.builder()
                        .backupAt(LocalDateTime.now())
                        .createdBy(username)
                        .flywayVersion("UNKNOWN") // có thể map từ flyway nếu m có endpoint/version
                        .appVersion("1.0.0")
                        .build();

                writeJson(zos, "metadata.json", meta);

                // 2) Export từng bảng theo DTO phẳng
                writeJson(zos, "system_setting.json", systemSettingRepository.findAll().stream().map(this::toDto).toList());

                writeJson(zos, "role.json", roleRepository.findAll().stream().map(this::toDto).toList());
                writeJson(zos, "permission.json", permissionRepository.findAll().stream().map(this::toDto).toList());
                //writeJson(zos, "role_permission.json", rolePermissionRepository.findAll().stream().map(this::toDto).toList());
                // ------------------------------------------------------------
                // Chỉ backup role_permission hợp lệ
                // (permission phải còn tồn tại trong bảng permission)
                // ------------------------------------------------------------
                List<Long> validPermissionIds =
                        permissionRepository.findAll()
                                .stream()
                                .map(Permission::getId)
                                .toList();

                writeJson(
                        zos,
                        "role_permission.json",
                        rolePermissionRepository.findAll()
                                .stream()
                                // ⭐ CHẶN role_permission trỏ tới permission không tồn tại
                                .filter(rp -> validPermissionIds.contains(rp.getPermission().getId()))
                                .map(this::toDto)
                                .toList()
                );

                writeJson(zos, "app_user.json", userRepository.findAll().stream().map(this::toDto).toList());
                writeJson(zos, "user_role.json", userRoleRepository.findAll().stream().map(this::toDto).toList());

                writeJson(zos, "member.json", memberRepository.findAll().stream().map(this::toDto).toList());
                writeJson(zos, "member_point_history.json", memberPointHistoryRepository.findAll().stream().map(this::toDto).toList());

                writeJson(zos, "restaurant_table.json", restaurantTableRepository.findAll().stream().map(this::toDto).toList());

                writeJson(zos, "dish.json", dishRepository.findAll().stream().map(this::toDto).toList());
                writeJson(zos, "orders.json", orderRepository.findAll().stream().map(this::toDto).toList());
                writeJson(zos, "order_item.json", orderItemRepository.findAll().stream().map(this::toDto).toList());

                writeJson(zos, "invoice.json", invoiceRepository.findAll().stream().map(this::toDto).toList());
                writeJson(zos, "invoice_item.json", invoiceItemRepository.findAll().stream().map(this::toDto).toList());
                writeJson(zos, "payment.json", paymentRepository.findAll().stream().map(this::toDto).toList());

                writeJson(zos, "notification.json", notificationRepository.findAll().stream().map(this::toDto).toList());
                writeJson(zos, "notification_user_status.json", notificationUserStatusRepository.findAll().stream().map(this::toDto).toList());
                writeJson(zos, "audit_log.json", auditLogRepository.findAll().stream().map(this::toDto).toList());
            }

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Backup thất bại: không tạo được file ZIP", e);
        }
    }

    /**
     * Ghi 1 file JSON vào ZIP
     */
    private void writeJson(ZipOutputStream zos, String entryName, Object data) throws Exception {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        byte[] bytes = objectMapper.writeValueAsBytes(data);
        zos.write(bytes);
        zos.closeEntry();
    }

    // -------------------------
    // Mapper Entity -> DTO
    // -------------------------

    private RoleDto toDto(Role e) {
        return RoleDto.builder()
                .id(e.getId()).code(e.getCode()).name(e.getName()).description(e.getDescription())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    private PermissionDto toDto(Permission e) {
        return PermissionDto.builder()
                .id(e.getId()).code(e.getCode()).name(e.getName()).description(e.getDescription())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    private RolePermissionDto toDto(RolePermission e) {
        return RolePermissionDto.builder()
                .id(e.getId())
                .roleId(e.getRole().getId())
                .permissionId(e.getPermission().getId())
                .build();
    }

    private UserDto toDto(User e) {
        return UserDto.builder()
                .id(e.getId()).username(e.getUsername()).password(e.getPassword())
                .fullName(e.getFullName()).status(e.getStatus())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    private UserRoleDto toDto(UserRole e) {
        return UserRoleDto.builder()
                .id(e.getId())
                .userId(e.getUser().getId())
                .roleId(e.getRole().getId())
                .build();
    }

    private SystemSettingDto toDto(SystemSetting e) {
        return SystemSettingDto.builder()
                .id(e.getId())
                .settingGroup(e.getSettingGroup())
                .settingKey(e.getSettingKey())
                .settingValue(e.getSettingValue())
                .valueType(e.getValueType())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    private RestaurantTableDto toDto(RestaurantTable e) {
        return RestaurantTableDto.builder()
                .id(e.getId()).name(e.getName()).capacity(e.getCapacity())
                .status(e.getStatus()).mergedRootId(e.getMergedRootId())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    private DishDto toDto(Dish d) {
        return DishDto.builder()
                .id(d.getId())
                .name(d.getName())
                .price(d.getPrice())
                .status(d.getStatus())
                .imageUrl(d.getImageUrl()) // ⭐ thêm
                .categoryId(
                        d.getCategory() == null
                                ? null
                                : d.getCategory().getId()
                )
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private OrderDto toDto(Order e) {
        return OrderDto.builder()
                .id(e.getId()).orderCode(e.getOrderCode()).totalPrice(e.getTotalPrice())
                .status(e.getStatus()).note(e.getNote())
                .createdBy(e.getCreatedBy()).memberId(e.getMemberId())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .tableId(e.getTable() == null ? null : e.getTable().getId())
                .build();
    }

    private OrderItemDto toDto(OrderItem e) {
        return OrderItemDto.builder()
                .id(e.getId())
                .orderId(e.getOrder().getId())
                .dishId(e.getDish().getId())
                .quantity(e.getQuantity())
                .snapshotPrice(e.getSnapshotPrice())
                .status(e.getStatus())
                .note(e.getNote())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private InvoiceDto toDto(Invoice e) {
        return InvoiceDto.builder()
                .id(e.getId()).orderId(e.getOrderId())
                .totalAmount(e.getTotalAmount())
                .discountAmount(e.getDiscountAmount())
                .originalTotalAmount(e.getOriginalTotalAmount())
                .voucherDiscountAmount(e.getVoucherDiscountAmount())
                .defaultDiscountAmount(e.getDefaultDiscountAmount())
                .amountBeforeVat(e.getAmountBeforeVat())
                .vatRate(e.getVatRate())
                .vatAmount(e.getVatAmount())
                .voucherCode(e.getVoucherCode())
                .paymentMethod(e.getPaymentMethod())
                .paidAt(e.getPaidAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .loyaltyEarnedPoint(e.getLoyaltyEarnedPoint())
                .customerPaid(e.getCustomerPaid())
                .changeAmount(e.getChangeAmount())
                .build();
    }

    private InvoiceItemDto toDto(InvoiceItem e) {
        return InvoiceItemDto.builder()
                .id(e.getId())
                .invoiceId(e.getInvoice().getId())
                .dishId(e.getDishId())
                .dishName(e.getDishName())
                .dishPrice(e.getDishPrice())
                .quantity(e.getQuantity())
                .subtotal(e.getSubtotal())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private PaymentDto toDto(Payment e) {
        return PaymentDto.builder()
                .id(e.getId())
                .orderId(e.getOrder().getId())
                .invoiceId(e.getInvoice().getId())
                .amount(e.getAmount())
                .customerPaid(e.getCustomerPaid())
                .changeAmount(e.getChangeAmount())
                // ✅ enum → string
                .method(
                        e.getMethod() == null
                                ? null
                                : e.getMethod().name()
                )
                .note(e.getNote())
                .paidAt(e.getPaidAt())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private NotificationDto toDto(Notification e) {
        return NotificationDto.builder()
                .id(e.getId()).title(e.getTitle()).message(e.getMessage())
                .type(e.getType()).link(e.getLink()).createdAt(e.getCreatedAt())
                .build();
    }

    private NotificationUserStatusDto toDto(NotificationUserStatus e) {
        return NotificationUserStatusDto.builder()
                .id(e.getId())
                .notificationId(e.getNotification().getId())
                .userId(e.getUser().getId())
                .status(e.getStatus())
                .readAt(e.getReadAt())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private AuditLogDto toDto(AuditLog e) {
        return AuditLogDto.builder()
                .id(e.getId())
                .userId(e.getUser() == null ? null : e.getUser().getId())
                .action(e.getAction())
                .entity(e.getEntity())
                .entityId(e.getEntityId())
                .ipAddress(e.getIpAddress())
                .userAgent(e.getUserAgent())
                .beforeData(e.getBeforeData())
                .afterData(e.getAfterData())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private MemberDto toDto(Member e) {
        return MemberDto.builder()
                .id(e.getId()).name(e.getName()).phone(e.getPhone()).email(e.getEmail())
                .active(e.getActive()).birthday(e.getBirthday())
                .tier(e.getTier())
                .totalPoint(e.getTotalPoint()).lifetimePoint(e.getLifetimePoint()).usedPoint(e.getUsedPoint())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    private MemberPointHistoryDto toDto(MemberPointHistory e) {
        return MemberPointHistoryDto.builder()
                .id(e.getId())
                .memberId(e.getMemberId())
                .changeAmount(e.getChangeAmount())
                .balanceAfter(e.getBalanceAfter())
                .type(e.getType())
                .description(e.getDescription())
                .orderId(e.getOrderId())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
