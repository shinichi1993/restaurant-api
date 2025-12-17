package com.restaurant.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.restaurant.api.dto.backup.*;
import com.restaurant.api.entity.*;
import com.restaurant.api.enums.AuditAction;
import com.restaurant.api.enums.NotificationType;
import com.restaurant.api.enums.OrderStatus;
import com.restaurant.api.repository.*;
import com.restaurant.api.util.AuthUtil;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * RestoreService – Phase 4.4
 * ============================================================
 * NHIỆM VỤ:
 *  - Restore toàn bộ database từ file backup ZIP
 *
 * NGUYÊN TẮC BẮT BUỘC:
 *  - KHÔNG dùng saveAll() cho bảng có @GeneratedValue
 *  - JDBC INSERT để GIỮ NGUYÊN ID
 *  - Bảng CHA insert trước, bảng CON insert sau
 *  - Sau restore phải reset sequence
 *
 * Áp dụng Rule 13: comment tiếng Việt đầy đủ
 */
@Service
@RequiredArgsConstructor
public class RestoreService {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    private final UserRepository userRepository;

    /**
     * restoreFromZip
     * ------------------------------------------------------------
     * Hàm ENTRY POINT cho chức năng RESTORE DATABASE (Phase 4.4)
     *
     * Luồng xử lý CHUẨN:
     *  1) Lấy user đang đăng nhập từ JWT (không nhận từ FE)
     *  2) Đọc toàn bộ file ZIP backup
     *  3) Validate cấu trúc backup (thiếu file → FAIL NGAY)
     *  4) Chặn restore nếu hệ thống còn order đang mở (NEW / SERVING)
     *  5) TRUNCATE toàn bộ bảng nghiệp vụ (đúng thứ tự FK)
     *  6) Restore dữ liệu theo thứ tự CHA → CON
     *  7) Ghi AuditLog + Notification hệ thống
     *
     * Lưu ý quan trọng:
     *  - Toàn bộ hàm chạy trong @Transactional
     *  - Nếu bất kỳ bước nào lỗi → ROLLBACK toàn bộ
     */
    @Transactional
    public void restoreFromZip(InputStream zipStream) {
        Long actorUserId = null;

        try {
            // ====================================================
            // 1️⃣ Lấy user đang login từ JWT
            // ====================================================
            String username = AuthUtil.getCurrentUsername();
            if (username == null) {
                throw new IllegalStateException("Không xác định được user từ JWT");
            }

            User actor = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy user: " + username));
            actorUserId = actor.getId();

            // ====================================================
            // 2️⃣ Chuẩn bị ObjectMapper
            // ====================================================
            objectMapper.registerModule(new JavaTimeModule());

            // ====================================================
            // 3️⃣ Đọc ZIP
            // ====================================================
            Map<String, byte[]> entries = readZipEntries(zipStream);

            // ====================================================
            // 4️⃣ Validate cấu trúc backup
            // ====================================================
            validateRequiredEntries(entries);

            // ====================================================
            // 5️⃣ Chặn restore nếu còn order mở
            // ====================================================
            blockIfHasOpenOrders();

            // ====================================================
            // 6️⃣ TRUNCATE toàn bộ bảng nghiệp vụ
            // ====================================================
            truncateAllBusinessTables();

            // ====================================================
            // 7️⃣ Restore dữ liệu (CORE)
            // ====================================================
            restoreAll(entries);

            // ====================================================
            // 8️⃣ Ghi audit + notification
            // ====================================================
            createAuditAndNotification(actorUserId, true, null);

        } catch (Exception e) {
            e.printStackTrace();
            createAuditAndNotification(actorUserId, false, e.getMessage());
            throw new RuntimeException("Restore thất bại – rollback toàn bộ", e);
        }
    }

    // ============================================================
    // ZIP & VALIDATE
    // ============================================================

    private Map<String, byte[]> readZipEntries(InputStream is) throws Exception {
        Map<String, byte[]> map = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                map.put(entry.getName(), baos.toByteArray());
                zis.closeEntry();
            }
        }
        return map;
    }

    private void validateRequiredEntries(Map<String, byte[]> entries) {
        List<String> required = List.of(
                "metadata.json",
                "system_setting.json",
                "role.json", "permission.json", "role_permission.json",
                "app_user.json", "user_role.json",
                "member.json", "member_point_history.json",
                "restaurant_table.json",
                "dish.json",
                "orders.json", "order_item.json",
                "invoice.json", "invoice_item.json", "payment.json",
                "notification.json", "notification_user_status.json",
                "audit_log.json"
        );

        List<String> missing = required.stream()
                .filter(k -> !entries.containsKey(k))
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("File backup thiếu: " + String.join(", ", missing));
        }
    }

    /**
     * blockIfHasOpenOrders
     * ------------------------------------------------------------
     * Chặn thao tác restore nếu hệ thống đang có Order đang mở.
     *
     * Lý do:
     *  - Restore DB khi còn order NEW / SERVING sẽ gây:
     *      + Mất dữ liệu đang thao tác
     *      + Lỗi FK / logic nghiệp vụ
     *
     * Cách làm:
     *  - Query trực tiếp DB bằng JDBC (KHÔNG dùng cache Hibernate)
     *  - Chỉ cần tồn tại 1 order NEW hoặc SERVING → CHẶN
     */

    private void blockIfHasOpenOrders() {
        entityManager.clear();
        boolean hasOpen = jdbcTemplate.queryForObject(
                "select exists(select 1 from orders where status in ('NEW','SERVING'))",
                Boolean.class
        );
        if (hasOpen) {
            throw new IllegalStateException("Không thể restore vì đang có Order NEW/SERVING");
        }
    }

    /**
     * truncateAllBusinessTables
     * ------------------------------------------------------------
     * Xóa TOÀN BỘ dữ liệu nghiệp vụ trước khi restore.
     *
     * Nguyên tắc:
     *  - TRUNCATE theo thứ tự CON → CHA để tránh lỗi FK
     *  - Dùng RESTART IDENTITY để reset sequence
     *  - CASCADE để xóa sạch toàn bộ ràng buộc liên quan
     *
     * ⚠️ KHÔNG được đổi thứ tự bảng nếu chưa hiểu FK
     */

    private void truncateAllBusinessTables() {
        List<String> tables = List.of(
                // Notification + Audit (phụ thuộc user)
                "notification_user_status",
                "notification",
                "audit_log",

                // Payment / Invoice
                "payment",
                "invoice_item",
                "invoice",
                // Order
                "order_item",
                "orders",
                // Loyalty
                "member_point_history",
                "member",
                // Security
                "user_role",
                "role_permission",
                "app_user",
                "permission",
                "role",
                // Master data
                "dish",
                "restaurant_table",
                "system_setting"
        );

        for (String t : tables) {
            jdbcTemplate.execute("TRUNCATE TABLE " + t + " RESTART IDENTITY CASCADE");
        }
    }

    /**
     * restoreAll
     * ------------------------------------------------------------
     * Restore toàn bộ dữ liệu từ file backup theo thứ tự PHỤ THUỘC FK.
     *
     * Nguyên tắc BẮT BUỘC:
     *  - Bảng CHA insert trước
     *  - Bảng CON insert sau
     *  - KHÔNG dùng saveAll() cho bảng có IDENTITY
     *  - Dùng JDBC INSERT để GIỮ NGUYÊN ID từ backup
     *
     * Thứ tự tổng quát:
     *  SYSTEM → SECURITY → MASTER → ORDER → INVOICE → NOTIFICATION → AUDIT
     */

    private void restoreAll(Map<String, byte[]> entries) throws Exception {

        // ---------- READ DTO ----------
        List<SystemSettingDto> settings = readList(entries, "system_setting.json", new TypeReference<>() {});
        List<RoleDto> roles = readList(entries, "role.json", new TypeReference<>() {});
        List<PermissionDto> permissions = readList(entries, "permission.json", new TypeReference<>() {});
        List<RolePermissionDto> rolePerms = readList(entries, "role_permission.json", new TypeReference<>() {});
        List<UserDto> users = readList(entries, "app_user.json", new TypeReference<>() {});
        List<UserRoleDto> userRoles = readList(entries, "user_role.json", new TypeReference<>() {});
        List<MemberDto> members = readList(entries, "member.json", new TypeReference<>() {});
        List<MemberPointHistoryDto> mph = readList(entries, "member_point_history.json", new TypeReference<>() {});
        List<RestaurantTableDto> tables = readList(entries, "restaurant_table.json", new TypeReference<>() {});
        List<DishDto> dishes = readList(entries, "dish.json", new TypeReference<>() {});
        List<OrderDto> orders = readList(entries, "orders.json", new TypeReference<>() {});
        List<OrderItemDto> orderItems = readList(entries, "order_item.json", new TypeReference<>() {});
        List<InvoiceDto> invoices = readList(entries, "invoice.json", new TypeReference<>() {});
        List<InvoiceItemDto> invoiceItems = readList(entries, "invoice_item.json", new TypeReference<>() {});
        List<PaymentDto> payments = readList(entries, "payment.json", new TypeReference<>() {});
        List<NotificationDto> notifications = readList(entries, "notification.json", new TypeReference<>() {});
        List<NotificationUserStatusDto> nus = readList(entries, "notification_user_status.json", new TypeReference<>() {});
        List<AuditLogDto> auditLogs = readList(entries, "audit_log.json", new TypeReference<>() {});

        // ---------- INSERT CHA ----------
        batchInsertSystemSetting(settings);
        batchInsertRole(roles);
        batchInsertPermission(permissions);

        // ---------- INSERT CON ----------
        batchInsertRolePermission(rolePerms);
        batchInsertUser(users);
        batchInsertUserRole(userRoles);
        batchInsertMember(members);
        batchInsertMemberPointHistory(mph);
        batchInsertRestaurantTable(tables);
        batchInsertDish(dishes);
        batchInsertOrder(orders);
        batchInsertOrderItem(orderItems);
        batchInsertInvoice(invoices);
        batchInsertInvoiceItem(invoiceItems);
        batchInsertPayment(payments);
        batchInsertNotification(notifications);
        batchInsertNotificationUserStatus(nus);
        batchInsertAuditLog(auditLogs);

        // ---------- RESET SEQUENCE ----------
        resetAllSequences();
    }

    // ------------------------------------------------------------
    // Đọc toàn bộ file JSON → DTO
    // (CHỈ đọc dữ liệu, CHƯA ghi vào DB)
    // ------------------------------------------------------------
    private <T> List<T> readList(Map<String, byte[]> entries, String fileName, TypeReference<List<T>> ref) throws Exception {
        return objectMapper.readValue(entries.get(fileName), ref);
    }

    // ------------------------------------------------------------
    // ROLE & PERMISSION
    //  - Là bảng CHA của toàn bộ hệ thống phân quyền
    //  - BẮT BUỘC insert trước role_permission
    // ------------------------------------------------------------

    private void batchInsertPermission(List<PermissionDto> list) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO permission (id, code, name, description, created_at, updated_at) VALUES (?,?,?,?,?,?)",
                list,
                200,
                (ps, d) -> {
                    ps.setLong(1, d.getId());
                    ps.setString(2, d.getCode());
                    ps.setString(3, d.getName());
                    ps.setString(4, d.getDescription());
                    ps.setObject(5, d.getCreatedAt());
                    ps.setObject(6, d.getUpdatedAt());
                }
        );
    }

    private void batchInsertRole(List<RoleDto> list) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO role (id, code, name, description, created_at, updated_at) VALUES (?,?,?,?,?,?)",
                list,
                200,
                (ps, d) -> {
                    ps.setLong(1, d.getId());
                    ps.setString(2, d.getCode());
                    ps.setString(3, d.getName());
                    ps.setString(4, d.getDescription());
                    ps.setObject(5, d.getCreatedAt());
                    ps.setObject(6, d.getUpdatedAt());
                }
        );
    }

    /**
     * batchInsertRolePermission
     * ------------------------------------------------------------
     * Insert bảng role_permission (bảng trung gian N-N).
     *
     * Yêu cầu:
     *  - role_id PHẢI tồn tại trong bảng role
     *  - permission_id PHẢI tồn tại trong bảng permission
     *
     * Nếu thứ tự insert sai → lỗi FK ngay lập tức
     */
    private void batchInsertRolePermission(List<RolePermissionDto> list) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO role_permission (id, role_id, permission_id) VALUES (?,?,?)",
                list,
                200,
                (ps, d) -> {
                    ps.setLong(1, d.getId());
                    ps.setLong(2, d.getRoleId());
                    ps.setLong(3, d.getPermissionId());
                }
        );
    }

    // ============================================================================
    // JDBC BATCH INSERT – Phase 4.4
    // ============================================================================
    // Mục tiêu:
    //  - Insert dữ liệu theo đúng ID từ file backup (insert thủ công cột id)
    //  - Tránh Hibernate @GeneratedValue tự sinh ID lệch -> lỗi FK
    //  - Insert theo thứ tự CHA → CON
    // ============================================================================

    /**
     * Insert bảng system_setting bằng JDBC
     * ------------------------------------------------------------
     * - Bắt buộc dùng JDBC để giữ nguyên ID từ file backup
     * - Kiểu dữ liệu PHẢI KHỚP 100% với Flyway
     * - KHÔNG đoán kiểu, KHÔNG dựa DTO
     */
    private void batchInsertSystemSetting(List<SystemSettingDto> list) {

        jdbcTemplate.batchUpdate(
                """
                INSERT INTO system_setting
                (id,
                 setting_group,
                 setting_key,
                 setting_value,
                 value_type,
                 description,
                 created_at,
                 updated_at,
                 created_by,
                 updated_by)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """,
                list,
                200,
                (ps, s) -> {

                    // id BIGSERIAL → Long
                    ps.setLong(1, s.getId());

                    // VARCHAR
                    ps.setString(2, s.getSettingGroup());
                    ps.setString(3, s.getSettingKey());

                    // TEXT
                    ps.setString(4, s.getSettingValue());

                    // ENUM → VARCHAR
                    ps.setString(5, s.getValueType().name());

                    // TEXT (nullable)
                    ps.setString(6, s.getDescription());

                    // TIMESTAMP
                    ps.setObject(7, s.getCreatedAt());
                    ps.setObject(8, s.getUpdatedAt());

                    // BIGINT (nullable)
                    if (s.getCreatedBy() != null) {
                        ps.setLong(9, s.getCreatedBy());
                    } else {
                        ps.setNull(9, java.sql.Types.BIGINT);
                    }

                    if (s.getUpdatedBy() != null) {
                        ps.setLong(10, s.getUpdatedBy());
                    } else {
                        ps.setNull(10, java.sql.Types.BIGINT);
                    }
                }
        );
    }

    /**
     * Insert APP_USER
     */
    private void batchInsertUser(List<UserDto> list) {
        if (list == null || list.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                "INSERT INTO app_user (id, username, password, full_name, status, created_at, updated_at) VALUES (?,?,?,?,?,?,?)",
                list,
                200,
                (ps, u) -> {
                    ps.setLong(1, u.getId());
                    ps.setString(2, u.getUsername());
                    ps.setString(3, u.getPassword());
                    ps.setString(4, u.getFullName());
                    ps.setString(5, u.getStatus().name());
                    ps.setObject(6, u.getCreatedAt());
                    ps.setObject(7, u.getUpdatedAt());
                }
        );
    }

    /**
     * Insert USER_ROLE (FK tới app_user + role)
     */
    private void batchInsertUserRole(List<UserRoleDto> list) {
        if (list == null || list.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                "INSERT INTO user_role (id, user_id, role_id) VALUES (?,?,?)",
                list,
                200,
                (ps, ur) -> {
                    ps.setLong(1, ur.getId());
                    ps.setLong(2, ur.getUserId());
                    ps.setLong(3, ur.getRoleId());
                }
        );
    }

    /**
     * Insert MEMBER
     */
    private void batchInsertMember(List<MemberDto> list) {
        if (list == null || list.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                "INSERT INTO member " +
                        "(id, name, phone, email, active, birthday, tier, total_point, used_point, lifetime_point, created_at, updated_at) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                list,
                200,
                (ps, m) -> {
                    ps.setLong(1, m.getId());
                    ps.setString(2, m.getName());
                    ps.setString(3, m.getPhone());
                    ps.setString(4, m.getEmail());
                    ps.setObject(5, m.getActive()); // Boolean
                    ps.setObject(6, m.getBirthday());
                    ps.setString(7, m.getTier().name());
                    ps.setInt(8, m.getTotalPoint());
                    ps.setInt(9, m.getUsedPoint());
                    ps.setInt(10, m.getLifetimePoint());
                    ps.setObject(11, m.getCreatedAt());
                    ps.setObject(12, m.getUpdatedAt());
                }
        );
    }

    /**
     * Insert MEMBER_POINT_HISTORY
     */
    private void batchInsertMemberPointHistory(List<MemberPointHistoryDto> list) {
        if (list == null || list.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                "INSERT INTO member_point_history " +
                        "(id, member_id, change_amount, balance_after, type, description, order_id, created_at) " +
                        "VALUES (?,?,?,?,?,?,?,?)",
                list,
                200,
                (ps, h) -> {
                    ps.setLong(1, h.getId());
                    ps.setLong(2, h.getMemberId());
                    ps.setInt(3, h.getChangeAmount());
                    ps.setInt(4, h.getBalanceAfter());
                    ps.setString(5, h.getType());
                    ps.setString(6, h.getDescription());
                    ps.setObject(7, h.getOrderId());
                    ps.setObject(8, h.getCreatedAt());
                }
        );
    }

    /**
     * Insert RESTAURANT_TABLE
     */
    private void batchInsertRestaurantTable(List<RestaurantTableDto> list) {
        if (list == null || list.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                "INSERT INTO restaurant_table " +
                        "(id, name, capacity, status, merged_root_id, created_at, updated_at) " +
                        "VALUES (?,?,?,?,?,?,?)",
                list,
                200,
                (ps, t) -> {
                    ps.setLong(1, t.getId());
                    ps.setString(2, t.getName());
                    ps.setInt(3, t.getCapacity());
                    ps.setString(4, t.getStatus().name()); // enum TableStatus
                    ps.setObject(5, t.getMergedRootId());
                    ps.setObject(6, t.getCreatedAt());
                    ps.setObject(7, t.getUpdatedAt());
                }
        );
    }

    /**
     * Insert DISH (FK category_id)
     * Lưu ý: phải có category_id hợp lệ. Nếu backup chưa export category -> sẽ lỗi FK.
     */
    private void batchInsertDish(List<DishDto> list) {
        if (list == null || list.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                "INSERT INTO dish " +
                        "(id, name, category_id, price, image_url, status, created_at, updated_at) " +
                        "VALUES (?,?,?,?,?,?,?,?)",
                list,
                200,
                (ps, d) -> {
                    ps.setLong(1, d.getId());
                    ps.setString(2, d.getName());
                    ps.setLong(3, d.getCategoryId());
                    ps.setBigDecimal(4, d.getPrice());
                    ps.setString(5, d.getImageUrl());
                    ps.setString(6, d.getStatus());
                    ps.setObject(7, d.getCreatedAt());
                    ps.setObject(8, d.getUpdatedAt());
                }
        );
    }

    /**
     * Insert bảng orders theo cấu trúc DB hiện tại
     * ---------------------------------------------------
     * Bao gồm:
     *  - table_id
     *  - member_id
     *
     * ⚠ Không dựa vào entity, dựa vào DB THỰC TẾ
     */
    private void batchInsertOrder(List<OrderDto> list) {

        jdbcTemplate.batchUpdate(
                """
                INSERT INTO orders
                (id,
                 order_code,
                 total_price,
                 status,
                 note,
                 created_by,
                 table_id,
                 member_id,
                 created_at,
                 updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """,
                list,
                200,
                (ps, o) -> {

                    // 1. ID
                    ps.setLong(1, o.getId());

                    // 2. Order code
                    ps.setString(2, o.getOrderCode());

                    // 3. Total price
                    ps.setBigDecimal(3, o.getTotalPrice());

                    // 4. Status (ENUM → VARCHAR)
                    ps.setString(4, o.getStatus().name());

                    // 5. Note (nullable)
                    ps.setString(5, o.getNote());

                    // 6. Created by (nullable)
                    if (o.getCreatedBy() != null) {
                        ps.setLong(6, o.getCreatedBy());
                    } else {
                        ps.setNull(6, java.sql.Types.BIGINT);
                    }

                    // 7. Table ID (nullable)
                    if (o.getTableId() != null) {
                        ps.setLong(7, o.getTableId());
                    } else {
                        ps.setNull(7, java.sql.Types.BIGINT);
                    }

                    // 8. Member ID (nullable)
                    if (o.getMemberId() != null) {
                        ps.setLong(8, o.getMemberId());
                    } else {
                        ps.setNull(8, java.sql.Types.BIGINT);
                    }

                    // 9. Created at
                    ps.setObject(9, o.getCreatedAt());

                    // 10. Updated at
                    ps.setObject(10, o.getUpdatedAt());
                }
        );
    }

    /**
     * Insert ORDER_ITEM (FK order_id + dish_id)
     */
    private void batchInsertOrderItem(List<OrderItemDto> list) {
        if (list == null || list.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                "INSERT INTO order_item " +
                        "(id, order_id, dish_id, quantity, snapshot_price, status, note, created_at, updated_at) " +
                        "VALUES (?,?,?,?,?,?,?,?,?)",
                list,
                200,
                (ps, i) -> {
                    ps.setLong(1, i.getId());
                    ps.setLong(2, i.getOrderId());
                    ps.setLong(3, i.getDishId());
                    ps.setInt(4, i.getQuantity());
                    ps.setBigDecimal(5, i.getSnapshotPrice());
                    ps.setString(6, i.getStatus().name()); // enum OrderItemStatus
                    ps.setString(7, i.getNote());
                    ps.setObject(8, i.getCreatedAt());
                    ps.setObject(9, i.getUpdatedAt());
                }
        );
    }

    /**
     * Insert bảng invoice theo DB THỰC TẾ
     * ---------------------------------------------------
     * ⚠ KHÔNG dựa entity
     * ⚠ KHÔNG dựa Flyway cũ
     * ⚠ Dựa 100% information_schema
     */
    private void batchInsertInvoice(List<InvoiceDto> list) {

        jdbcTemplate.batchUpdate(
                """
                INSERT INTO invoice (
                    id,
                    order_id,
                    total_amount,
                    payment_method,
                    paid_at,
                    created_at,
                    updated_at,
                    discount_amount,
                    voucher_code,
                    loyalty_earned_point,
                    original_total_amount,
                    voucher_discount_amount,
                    default_discount_amount,
                    amount_before_vat,
                    vat_rate,
                    vat_amount,
                    customer_paid,
                    change_amount
                )
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                list,
                200,
                (ps, i) -> {

                    // 1. ID
                    ps.setLong(1, i.getId());

                    // 2. Order ID
                    ps.setLong(2, i.getOrderId());

                    // 3. Total amount
                    ps.setBigDecimal(3, i.getTotalAmount());

                    // 4. Payment method (ENUM → VARCHAR)
                    ps.setString(4, i.getPaymentMethod());

                    // 5. Paid at
                    ps.setObject(5, i.getPaidAt());

                    // 6. Created at
                    ps.setObject(6, i.getCreatedAt());

                    // 7. Updated at
                    ps.setObject(7, i.getUpdatedAt());

                    // 8. Discount amount
                    ps.setBigDecimal(8, i.getDiscountAmount());

                    // 9. Voucher code
                    ps.setString(9, i.getVoucherCode());

                    // 10. Loyalty earned point
                    if (i.getLoyaltyEarnedPoint() != null) {
                        ps.setInt(10, i.getLoyaltyEarnedPoint());
                    } else {
                        ps.setNull(10, java.sql.Types.INTEGER);
                    }

                    // 11. Original total amount
                    ps.setBigDecimal(11, i.getOriginalTotalAmount());

                    // 12. Voucher discount amount
                    ps.setBigDecimal(12, i.getVoucherDiscountAmount());

                    // 13. Default discount amount
                    ps.setBigDecimal(13, i.getDefaultDiscountAmount());

                    // 14. Amount before VAT
                    ps.setBigDecimal(14, i.getAmountBeforeVat());

                    // 15. VAT rate
                    ps.setBigDecimal(15, i.getVatRate());

                    // 16. VAT amount
                    ps.setBigDecimal(16, i.getVatAmount());

                    // 17. Customer paid
                    ps.setBigDecimal(17, i.getCustomerPaid());

                    // 18. Change amount
                    ps.setBigDecimal(18, i.getChangeAmount());
                }
        );
    }

    /**
     * Insert INVOICE_ITEM (FK invoice_id)
     */
    private void batchInsertInvoiceItem(List<InvoiceItemDto> list) {
        if (list == null || list.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                "INSERT INTO invoice_item " +
                        "(id, invoice_id, dish_id, dish_name, dish_price, quantity, subtotal, created_at) " +
                        "VALUES (?,?,?,?,?,?,?,?)",
                list,
                200,
                (ps, it) -> {
                    ps.setLong(1, it.getId());
                    ps.setLong(2, it.getInvoiceId());
                    ps.setLong(3, it.getDishId());
                    ps.setString(4, it.getDishName());
                    ps.setBigDecimal(5, it.getDishPrice());
                    ps.setInt(6, it.getQuantity());
                    ps.setBigDecimal(7, it.getSubtotal());
                    ps.setObject(8, it.getCreatedAt());
                }
        );
    }

    /**
     * Insert bảng payment theo DB THỰC TẾ (Phase 4.4)
     * ---------------------------------------------------
     * - KHÔNG dựa entity
     * - KHÔNG dựa Flyway cũ
     * - Dựa information_schema
     * - Giữ nguyên ID từ backup
     */
    private void batchInsertPayment(List<PaymentDto> list) {

        jdbcTemplate.batchUpdate(
                """
                INSERT INTO payment (
                    id,
                    order_id,
                    invoice_id,
                    amount,
                    method,
                    note,
                    paid_at,
                    created_by,
                    created_at,
                    updated_at,
                    customer_paid,
                    change_amount
                )
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                list,
                200,
                (ps, p) -> {

                    // 1. id
                    ps.setLong(1, p.getId());

                    // 2. order_id
                    ps.setLong(2, p.getOrderId());

                    // 3. invoice_id
                    ps.setLong(3, p.getInvoiceId());

                    // 4. amount
                    ps.setBigDecimal(4, p.getAmount());

                    // 5. method (VARCHAR – DTO là String)
                    ps.setString(5, p.getMethod());

                    // 6. note (nullable)
                    ps.setString(6, p.getNote());

                    // 7. paid_at
                    ps.setObject(7, p.getPaidAt());

                    // 8. created_by (nullable)
                    if (p.getCreatedBy() != null) {
                        ps.setLong(8, p.getCreatedBy());
                    } else {
                        ps.setNull(8, java.sql.Types.BIGINT);
                    }

                    // 9. created_at
                    ps.setObject(9, p.getCreatedAt());

                    // 10. updated_at
                    ps.setObject(10, p.getUpdatedAt());

                    // 11. customer_paid
                    ps.setBigDecimal(11, p.getCustomerPaid());

                    // 12. change_amount
                    ps.setBigDecimal(12, p.getChangeAmount());
                }
        );
    }

    /**
     * Insert NOTIFICATION
     */
    private void batchInsertNotification(List<NotificationDto> list) {
        if (list == null || list.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                "INSERT INTO notification (id, title, message, type, link, created_at) VALUES (?,?,?,?,?,?)",
                list,
                200,
                (ps, n) -> {
                    ps.setLong(1, n.getId());
                    ps.setString(2, n.getTitle());
                    ps.setString(3, n.getMessage());
                    ps.setString(4, n.getType().name()); // enum NotificationType
                    ps.setString(5, n.getLink());
                    ps.setObject(6, n.getCreatedAt());
                }
        );
    }

    /**
     * Insert NOTIFICATION_USER_STATUS (FK notification_id + user_id)
     */
    private void batchInsertNotificationUserStatus(List<NotificationUserStatusDto> list) {
        if (list == null || list.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                "INSERT INTO notification_user_status " +
                        "(id, notification_id, user_id, status, read_at, created_at) " +
                        "VALUES (?,?,?,?,?,?)",
                list,
                200,
                (ps, s) -> {
                    ps.setLong(1, s.getId());
                    ps.setLong(2, s.getNotificationId());
                    ps.setLong(3, s.getUserId());
                    ps.setString(4, s.getStatus().name()); // enum NotificationStatus
                    ps.setObject(5, s.getReadAt());
                    ps.setObject(6, s.getCreatedAt());
                }
        );
    }

    /**
     * Insert AUDIT_LOG (FK user_id có thể null)
     */
    private void batchInsertAuditLog(List<AuditLogDto> list) {
        if (list == null || list.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                "INSERT INTO audit_log " +
                        "(id, action, entity, entity_id, ip_address, user_agent, before_data, after_data, user_id, created_at) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?)",
                list,
                200,
                (ps, a) -> {
                    ps.setLong(1, a.getId());
                    ps.setString(2, a.getAction().name()); // enum AuditAction
                    ps.setString(3, a.getEntity());
                    ps.setObject(4, a.getEntityId());
                    ps.setString(5, a.getIpAddress());
                    ps.setString(6, a.getUserAgent());
                    ps.setString(7, a.getBeforeData());
                    ps.setString(8, a.getAfterData());
                    ps.setObject(9, a.getUserId());
                    ps.setObject(10, a.getCreatedAt());
                }
        );
    }

    /**
     * resetAllSequences
     * ------------------------------------------------------------
     * Reset sequence cho các bảng có @GeneratedValue.
     *
     * Lý do:
     *  - Restore dùng ID thủ công (insert id)
     *  - Nếu không reset sequence:
     *      + Lần insert tiếp theo sẽ bị trùng key
     *      + Hệ thống sẽ crash sau restore
     */

    private void resetAllSequences() {
        List<String> tables = List.of(
                "role", "permission", "role_permission",
                "app_user", "user_role",
                "member", "member_point_history",
                "restaurant_table",
                "dish",
                "orders", "order_item",
                "invoice", "invoice_item", "payment",
                "notification", "notification_user_status",
                "audit_log"
        );

        for (String t : tables) {
            jdbcTemplate.execute(
                    "select setval(pg_get_serial_sequence('" + t + "', 'id'), coalesce((select max(id) from " + t + "),1))"
            );
        }
    }

    /**
     * createAuditAndNotification
     * ------------------------------------------------------------
     * Ghi log hệ thống cho thao tác Backup / Restore.
     *
     * Lưu ý:
     *  - Audit & Notification KHÔNG được làm hỏng restore
     *  - Nếu lỗi khi ghi log → BỎ QUA (catch & ignore)
     */

    private void createAuditAndNotification(Long actorUserId, boolean success, String errorMsg) {
        try {
            AuditLog log = new AuditLog();
            if (actorUserId != null) {
                log.setUser(entityManager.getReference(User.class, actorUserId));
            }
            log.setAction(AuditAction.BACKUP_RESTORE);
            log.setEntity("system");
            log.setAfterData(success ? "{\"result\":\"SUCCESS\"}" : "{\"result\":\"FAILED\",\"error\":\"" + safe(errorMsg) + "\"}");
            log.setCreatedAt(LocalDateTime.now());
            entityManager.persist(log);

            Notification n = Notification.builder()
                    .title("Restore dữ liệu")
                    .message(success ? "Restore dữ liệu thành công" : "Restore thất bại: " + safe(errorMsg))
                    .type(NotificationType.SYSTEM)
                    .createdAt(LocalDateTime.now())
                    .build();
            entityManager.persist(n);

        } catch (Exception ignore) {}
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }
}
