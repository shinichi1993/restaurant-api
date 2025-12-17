package com.restaurant.api.service;

import com.restaurant.api.dto.notification.CreateNotificationRequest;
import com.restaurant.api.dto.order.*;
import com.restaurant.api.entity.*;
import com.restaurant.api.enums.AuditAction;
import com.restaurant.api.enums.NotificationType;
import com.restaurant.api.enums.OrderItemStatus;
import com.restaurant.api.enums.OrderStatus;
import com.restaurant.api.repository.*;
import com.restaurant.api.util.AuthUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OrderService
 * ------------------------------------------------------------
 * Service xử lý toàn bộ nghiệp vụ liên quan tới ĐƠN GỌI MÓN:
 *
 *  - Tạo order mới từ danh sách món (OrderCreateRequest)
 *  - Tính tổng tiền dựa trên giá món (Dish.price)
 *  - Tự động trừ kho theo Định lượng món (RecipeItem)
 *  - Hoàn kho khi hủy đơn (CANCELED)
 *  - Đổi trạng thái đơn: NEW → SERVING → PAID / CANCELED
 *  - Lấy danh sách order (lọc theo trạng thái / ngày)
 *  - Lấy chi tiết 1 order (bao gồm danh sách món)
 *
 *  Ghi chú:
 *  - Tiêu/hoàn kho sử dụng bảng StockEntry (Module 05)
 *    với quantity âm/dương để điều chỉnh tồn kho.
 *  - Hóa đơn & thanh toán sẽ xử lý ở Module 09–10.
 * ------------------------------------------------------------
 * Tất cả comment tuân theo Rule 13 (viết tiếng Việt đầy đủ).
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DishRepository dishRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final IngredientRepository ingredientRepository;
    private final StockEntryRepository stockEntryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    // ✅ Phase 4.3 – Rule Engine thông báo (không gọi NotificationService trực tiếp nữa)
    private final NotificationRuleService notificationRuleService;
    private final AuditLogService auditLogService;
    // ✅ Service quản lý bàn (Module 16)
    private final RestaurantTableService restaurantTableService;
    // ✅ Service đọc cấu hình hệ thống (Module 20)
    private final SystemSettingService systemSettingService;

    // -------------------------------------------------------
    // HÀM ĐỌC POS SETTINGS
    // -------------------------------------------------------
    /**
     * Kiểm tra cấu hình: có tự động gửi món xuống bếp
     * ngay sau khi tạo order hay không.
     */
    private boolean isAutoSendKitchen() {
        return systemSettingService.getBooleanSetting("pos.auto_send_kitchen", false);
    }

    /**
     * Có cho phép sửa món sau khi đã gửi bếp hay không.
     * (Sẽ dùng tiếp ở các hàm update sau này)
     */
    private boolean isAllowEditAfterSend() {
        return systemSettingService.getBooleanSetting("pos.allow_edit_after_send", false);
    }

    /**
     * Có cho phép hủy món sau khi đã gửi bếp hay không.
     */
    private boolean isAllowCancelItem() {
        return systemSettingService.getBooleanSetting("pos.allow_cancel_item", true);
    }

    /**
     * Chế độ POS đơn giản:
     * - Không cần quá nhiều thao tác, phù hợp quán nhỏ/takeaway.
     */
    private boolean isSimplePosMode() {
        return systemSettingService.getBooleanSetting("pos.simple_pos_mode", false);
    }

    /**
     * Trong chế độ POS đơn giản, có bắt buộc chọn bàn hay không.
     */
    private boolean isSimplePosRequireTable() {
        return systemSettingService.getBooleanSetting("pos.simple_pos_require_table", false);
    }

    /**
     * Đọc cấu hình: có tự động chuyển trạng thái ORDER sang SERVING
     * khi có món chuyển sang COOKING hay không.
     * ---------------------------------------------------------------
     * - Key: pos.auto_order_serving_on_item_cooking
     * - Default: false → giữ logic như hiện tại (BE hoặc FE tự set SERVING)
     *
     * Ghi chú:
     *  - Flag này chủ yếu dùng trong KitchenService khi update trạng thái món.
     *  - Đặt helper ở đây để thống nhất logic đọc setting POS.
     */
    private boolean isAutoOrderServingOnItemCooking() {
        return systemSettingService.getBooleanSetting(
                "pos.auto_order_serving_on_item_cooking",
                false
        );
    }

    // =================================================================
    // 1. TẠO ORDER MỚI
    // =================================================================

    /**
     * Tạo order mới từ request FE.
     * ------------------------------------------------------------
     * Bước xử lý:
     *  1. Validate request (phải có ít nhất 1 món)
     *  2. Validate theo POS Settings (simple_pos_mode, require_table)
     *  3. Load danh sách món từ DB, tính tổng tiền
     *  4. Lưu Order
     *  5. Lưu OrderItem (theo entity mới: order, dish, snapshotPrice, status)
     *  6. Trừ kho theo RecipeItem
     *  7. Gửi notification + audit log
     *
     * @param req      request tạo order (danh sách món + ghi chú + tableId)
     * @param username username user đang đăng nhập (lấy từ JWT)
     * @return OrderResponse đầy đủ (gồm danh sách món)
     */
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest req, String username) {

        // ------------------------------------------------------------
        // 1) VALIDATE CƠ BẢN
        // ------------------------------------------------------------
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new RuntimeException("Order phải có ít nhất 1 món");
        }

        // Lấy userId từ username (đảm bảo createdBy là id thật)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        Long userId = user.getId();

        // ------------------------------------------------------------
        // 2) ĐỌC CẤU HÌNH POS & VALIDATE THEO MODE
        // ------------------------------------------------------------
        boolean simplePosMode = isSimplePosMode();
        boolean simplePosRequireTable = isSimplePosRequireTable();

        Long tableId = req.getTableId();

        if (simplePosMode) {
            // Chế độ POS đơn giản
            if (simplePosRequireTable && tableId == null) {
                // Nếu simple_pos_require_table = true thì bắt buộc phải chọn bàn
                throw new RuntimeException("Chế độ POS đơn giản yêu cầu phải chọn bàn trước khi tạo order.");
            }
            // Nếu require_table = false → cho phép không gửi tableId (order mang tính "không gán bàn")
        }
        // Nếu không ở simplePosMode → giữ hành vi cũ:
        // tableId có thể null (order không gắn bàn) hoặc có (order theo bàn)

        // ------------------------------------------------------------
        // 3) LOAD DANH SÁCH MÓN & TÍNH TỔNG TIỀN
        // ------------------------------------------------------------
        // Lấy danh sách dishId từ request
        List<Long> dishIds = req.getItems()
                .stream()
                .map(OrderItemRequest::getDishId)
                .toList();

        // Load toàn bộ món từ DB 1 lần
        List<Dish> dishes = dishRepository.findAllById(dishIds);
        if (dishes.size() != dishIds.size()) {
            throw new RuntimeException("Có món ăn không tồn tại trong hệ thống");
        }

        // Map dishId → Dish để dùng nhanh
        Map<Long, Dish> dishMap = dishes.stream()
                .collect(Collectors.toMap(Dish::getId, d -> d));

        // Tính tổng tiền
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : req.getItems()) {
            Dish dish = dishMap.get(itemReq.getDishId());
            BigDecimal price = dish.getPrice();
            BigDecimal qty = BigDecimal.valueOf(itemReq.getQuantity());
            totalPrice = totalPrice.add(price.multiply(qty));
        }

        // ------------------------------------------------------------
        // 4) TẠO ENTITY ORDER (chưa có OrderItem)
        // ------------------------------------------------------------
        Order order = Order.builder()
                .orderCode(generateOrderCode())   // Mã đơn tự sinh
                .totalPrice(totalPrice)
                .status(OrderStatus.NEW)          // Trạng thái ban đầu
                .note(req.getNote())
                .createdBy(userId)
                .build();

        Order saved = orderRepository.save(order);

        // ------------------------------------------------------------
        // 5) GÁN BÀN CHO ORDER (nếu có tableId)
        // ------------------------------------------------------------
        if (tableId != null) {
            // Đánh dấu bàn đang được sử dụng (OCCUPIED)
            RestaurantTable table = restaurantTableService.markTableOccupied(tableId);

            // Gán bàn cho order
            saved.setTable(table);

            // Lưu lại order sau khi gán bàn
            orderRepository.save(saved);
        }

        // ------------------------------------------------------------
        // 6) TẠO DANH SÁCH ORDER ITEM THEO ENTITY MỚI
        // ------------------------------------------------------------
        // Đọc cấu hình tự động gửi bếp
        boolean autoSendKitchen = isAutoSendKitchen();

        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : req.getItems()) {

            Dish dish = dishMap.get(itemReq.getDishId());
            if (dish == null) {
                throw new RuntimeException("Món ăn với ID " + itemReq.getDishId() + " không tồn tại");
            }

            // Giá snapshot tại thời điểm order (theo Rule 26 – BigDecimal)
            BigDecimal snapshotPrice = dish.getPrice();

            // Trạng thái ban đầu của món:
            //  - Nếu auto_send_kitchen = true → coi như đã gửi bếp ngay lập tức
            //  - Nếu false → để NEW, chờ nhân viên bấm "Gửi bếp" sau
            OrderItemStatus initialStatus = autoSendKitchen
                    ? OrderItemStatus.SENT_TO_KITCHEN
                    : OrderItemStatus.NEW;

            OrderItem oi = OrderItem.builder()
                    .order(saved)                // Quan hệ ManyToOne tới Order
                    .dish(dish)                  // Quan hệ ManyToOne tới Dish
                    .snapshotPrice(snapshotPrice)// Giá snapshot
                    .quantity(itemReq.getQuantity())
                    .status(initialStatus)       // Trạng thái khởi tạo theo setting
                    .note(itemReq.getNote())                  // Tạm thời chưa dùng ghi chú món
                    .build();

            orderItems.add(oi);
        }

        orderItemRepository.saveAll(orderItems);

        // ------------------------------------------------------------
        // 7) TRỪ KHO THEO RECIPE (giữ nguyên logic cũ)
        // ------------------------------------------------------------
        consumeStockForOrder(saved, orderItems);

        // ------------------------------------------------------------
        // 8) GỬI THÔNG BÁO QUA RULE ENGINE (Phase 4.3)
        // ------------------------------------------------------------
        // - Không gọi notificationService.createNotification trực tiếp nữa.
        // - Rule Engine sẽ tự kiểm tra bật/tắt + chống spam.
        notificationRuleService.onOrderCreated(saved, orderItems);

        // Audit log tạo order
        auditLogService.log(
                AuditAction.ORDER_CREATE,
                "order",
                saved.getId(),
                null,
                saved
        );

        // ------------------------------------------------------------
        // 9) TRẢ VỀ DTO ORDER RESPONSE
        // ------------------------------------------------------------

        return toOrderResponse(saved, orderItems);
    }

    /**
     * Hàm sinh mã orderCode đơn giản.
     * Có thể nâng cấp sau (theo ngày / theo chi nhánh...).
     */
    private String generateOrderCode() {
        // Ví dụ: ORD + timestamp hiện tại
        return "ORD" + System.currentTimeMillis();
    }

    // =================================================================
    // 2. LẤY DANH SÁCH ORDER + CHI TIẾT 1 ORDER
    // =================================================================

    /**
     * Lấy danh sách order với điều kiện lọc:
     *  - status: nếu null thì lấy tất cả
     *  - from, to: nếu null thì không filter theo ngày
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(OrderStatus status, LocalDateTime from, LocalDateTime to) {
        List<Order> orders;

        if (status != null && from != null && to != null) {
            orders = orderRepository.findByStatusAndCreatedAtBetween(status, from, to);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status);
        } else if (from != null && to != null) {
            orders = orderRepository.findByCreatedAtBetween(from, to);
        } else {
            orders = orderRepository.findAll();
        }

        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        // Lấy toàn bộ orderId để load orderItem
        List<Long> orderIds = orders.stream().map(Order::getId).toList();

        // Lấy toàn bộ OrderItem thuộc các order này
        List<OrderItem> allItems = orderItemRepository.findAll()
                .stream()
                .filter(oi -> orderIds.contains(oi.getOrder().getId()))
                .toList();

        // Group orderItem theo order.id
        Map<Long, List<OrderItem>> itemsByOrder = allItems.stream()
                .collect(Collectors.groupingBy(oi -> oi.getOrder().getId()));

        // Convert từng order → OrderResponse
        List<OrderResponse> result = new ArrayList<>();
        for (Order o : orders) {
            List<OrderItem> items = itemsByOrder.getOrDefault(o.getId(), List.of());
            //Lọc các món CANCELED trước khi trả về
            List<OrderItem> filtered =
                    items.stream()
                            .filter(i -> i.getStatus() != OrderItemStatus.CANCELED)
                            .toList();

            result.add(toOrderResponse(o, filtered));

            //result.add(toOrderResponse(o, items));
        }

        return result;
    }

    /**
     * Lấy chi tiết 1 order (bao gồm danh sách món).
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng"));

        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);

        // Lọc các order là CANCELED trước khi trả về
        List<OrderItem> filtered =
                items.stream()
                        .filter(i -> i.getStatus() != OrderItemStatus.CANCELED)
                        .toList();

        return toOrderResponse(order, filtered);
        //return toOrderResponse(order, items);
    }

    // =================================================================
    // 3. ĐỔI TRẠNG THÁI ORDER (NEW / SERVING / PAID / CANCELED)
    // =================================================================

    /**
     * Cập nhật trạng thái order theo nghiệp vụ:
     *  - NEW      → SERVING / CANCELED
     *  - SERVING  → PAID / CANCELED
     *  - PAID     → (không cho phép đổi trạng thái)
     *  - CANCELED → (không cho phép đổi trạng thái)
     *
     *  Khi chuyển sang CANCELED → hoàn kho.
     */
    @Transactional
    public void updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng"));

        OrderStatus oldStatus = order.getStatus();

        System.out.println("Status hiện tại là: " + oldStatus);

        // Không cho phép đổi từ PAID / CANCELED
        if (oldStatus == OrderStatus.PAID || oldStatus == OrderStatus.CANCELED) {
            throw new RuntimeException("Đơn hàng đã hoàn tất hoặc đã hủy, không thể đổi trạng thái");
        }

        // Kiểm tra rule chuyển trạng thái
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new RuntimeException("Chuyển trạng thái không hợp lệ từ " + oldStatus + " sang " + newStatus);
        }

        // Nếu chuyển sang CANCELED → hoàn kho
        if (newStatus == OrderStatus.CANCELED) {
            List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);
            restoreStockForOrder(order, items);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        // ============================================================
        // Phase 4.3 – Rule Engine: thông báo đổi trạng thái order
        // ============================================================
        notificationRuleService.onOrderStatusChanged(order, oldStatus, newStatus);

        // ✅ Audit log cập nhật order
        auditLogService.log(
                AuditAction.ORDER_UPDATE,
                "order",
                order.getId(),
                null,
                order
        );
    }

    /**
     * Kiểm tra rule chuyển trạng thái:
     *  - NEW      → SERVING / CANCELED
     *  - SERVING  → PAID / CANCELED
     *  - Khác: không hợp lệ
     */
    private boolean isValidStatusTransition(OrderStatus oldStatus, OrderStatus newStatus) {
        if (oldStatus == OrderStatus.NEW) {
            return newStatus == OrderStatus.SERVING || newStatus == OrderStatus.CANCELED;
        }
        if (oldStatus == OrderStatus.SERVING) {
            return newStatus == OrderStatus.PAID || newStatus == OrderStatus.CANCELED;
        }
        return false;
    }

    // =================================================================
    // 4. XÓA ORDER (CHỈ KHI CHƯA PAID)
    // =================================================================

    /**
     * Xóa order:
     *  - Chỉ cho phép xóa nếu status = NEW hoặc SERVING
     *  - Khi xóa → hoàn kho (vì coi như order không tồn tại)
     *  - Không cho phép xóa nếu PAID / CANCELED (để giữ lịch sử)
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng"));

        // ----------------------------------------------------------------
        // 🔧 TÍCH HỢP CẤU HÌNH POS: pos.allow_cancel_item
        // ----------------------------------------------------------------
        //  - Nếu cấu hình = false → không cho phép xóa đơn (dù NEW/SERVING)
        //  - Mặc định (nếu chưa cấu hình) = true → giữ hành vi cũ
        // ----------------------------------------------------------------
        boolean allowCancelItem = systemSettingService.getBooleanSetting(
                "pos.allow_cancel_item",
                true // default: cho phép xóa như hiện tại
        );
        if (!allowCancelItem) {
            throw new RuntimeException("Hệ thống không cho phép hủy/xóa đơn hàng hiện tại.");
        }

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELED) {
            throw new RuntimeException("Không thể xóa đơn hàng đã thanh toán hoặc đã hủy");
        }

        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);

        // Hoàn kho trước rồi mới xóa order
        restoreStockForOrder(order, items);

        // 🔥 GIẢI PHÓNG BÀN
        RestaurantTable table = order.getTable();
        if (table != null) {
            restaurantTableService.markTableAvailable(table.getId());
        }

        // Xóa item + order
        orderItemRepository.deleteByOrder_Id(orderId);
        orderRepository.delete(order);

        // Audit log
        auditLogService.log(
                AuditAction.ORDER_CANCEL,
                "order",
                order.getId(),
                null,
                order
        );

    }


    // =================================================================
    // 5. HÀM XỬ LÝ KHO: TIÊU KHO & HOÀN KHO
    // =================================================================

    /**
     * Tiêu kho theo order:
     *  - Duyệt từng OrderItem
     *  - Lấy danh sách RecipeItem (định lượng nguyên liệu cho món)
     *  - Tính tổng số lượng nguyên liệu cần dùng = recipe.quantity × orderQuantity
     *  - Ghi vào bảng StockEntry với quantity âm (tiêu hao)
     */
    private void consumeStockForOrder(Order order, List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            Long dishId = item.getDish().getId();
            Integer orderQty = item.getQuantity();

            List<RecipeItem> recipes = recipeItemRepository.findByDishId(dishId);

            for (RecipeItem r : recipes) {
                Ingredient ing = r.getIngredient();

                BigDecimal perDish = r.getQuantity();
                BigDecimal totalConsume = perDish.multiply(BigDecimal.valueOf(orderQty));

                // 🔥 Trừ tồn kho thật
                ing.setStockQuantity(
                        ing.getStockQuantity().subtract(totalConsume)
                );
                ingredientRepository.save(ing);

                // 🔥 Ghi log kho âm
                StockEntry entry = StockEntry.builder()
                        .ingredient(ing)
                        .quantity(totalConsume.negate())
                        .note("Tiêu hao nguyên liệu cho order " + order.getOrderCode())
                        .build();

                stockEntryRepository.save(entry);
            }
        }
    }

    /**
     * Hoàn kho khi hủy / xóa order:
     *  - Duyệt từng OrderItem
     *  - Lấy RecipeItem tương ứng
     *  - Tính số lượng cần hoàn lại
     *  - Ghi StockEntry với quantity dương (tăng kho)
     */
    private void restoreStockForOrder(Order order, List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            Long dishId = item.getDish().getId();
            Integer orderQty = item.getQuantity();

            List<RecipeItem> recipes = recipeItemRepository.findByDishId(dishId);

            for (RecipeItem r : recipes) {
                Ingredient ing = r.getIngredient();

                BigDecimal perDish = r.getQuantity();
                BigDecimal totalReturn = perDish.multiply(BigDecimal.valueOf(orderQty));

                // 🔥 Hoàn kho thật
                ing.setStockQuantity(
                        ing.getStockQuantity().add(totalReturn)
                );
                ingredientRepository.save(ing);

                // 🔥 Ghi log kho dương
                StockEntry entry = StockEntry.builder()
                        .ingredient(ing)
                        .quantity(totalReturn)
                        .note("Hoàn kho do hủy/xóa order " + order.getOrderCode())
                        .build();

                stockEntryRepository.save(entry);
            }
        }
    }

    // =================================================================
    // 6. HÀM CHUYỂN ENTITY → DTO
    // =================================================================

    /**
     * Convert Order + danh sách OrderItem → OrderResponse
     */
    private OrderResponse toOrderResponse(Order order,
                                          List<OrderItem> items) {

        List<OrderItemResponse> itemResponses = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem item : items) {
            // ❗ BỎ QUA MÓN ĐÃ HỦY
            if (item.getStatus() == OrderItemStatus.CANCELED) {
                continue;
            }

            Dish dish = item.getDish();
            if (dish == null) {
                continue; // Phòng trường hợp dữ liệu lỗi
            }

            // Ưu tiên dùng snapshotPrice, nếu null thì fallback về dish.price
            BigDecimal price = item.getSnapshotPrice() != null
                    ? item.getSnapshotPrice()
                    : dish.getPrice();

            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            BigDecimal subtotal = price.multiply(qty);

            // ❗ Chỉ cộng tiền món hợp lệ (ko tính các món đã CANCELED)
            total = total.add(subtotal);

            OrderItemResponse itemRes = OrderItemResponse.builder()
                    .dishId(dish.getId())
                    .dishName(dish.getName())
                    .dishPrice(price)
                    .quantity(item.getQuantity())
                    .subtotal(subtotal)
                    .status(item.getStatus())
                    .note(item.getNote())
                    .build();

            itemResponses.add(itemRes);
        }

        // ❗ Cập nhật lại totalPrice — luôn đúng, không cần FE tính lại
        return OrderResponse.builder()
                .id(order.getId())
                .memberId(order.getMemberId())
                .orderCode(order.getOrderCode())
                .totalPrice(total)
                .status(order.getStatus())
                .note(order.getNote())
                .createdBy(order.getCreatedBy())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    /**
     * Lấy order đang mở của một bàn theo tableId.
     * ---------------------------------------------------------
     * Dùng cho POS:
     *  - Khi nhân viên chọn bàn trên tablet
     *  - Nếu bàn đã có order chưa thanh toán → load ra để hiển thị lại món
     *  - Nếu bàn chưa có order → trả về null
     *
     * Lưu ý:
     *  - Trạng thái "đang mở" tuỳ vào quy ước trong hệ thống:
     *      + NEW, SERVING   : mới tạo, chưa thanh toán
     */

    public OrderResponse getOrderByTableId(Long tableId) {

        // Trạng thái order được xem là "đang mở"
        List<OrderStatus> openStatuses = List.of(OrderStatus.NEW, OrderStatus.SERVING);

        // Tìm order đang mở (NEW hoặc SERVING)
        Optional<Order> optional = orderRepository
                .findFirstByTableIdAndStatusIn(tableId, openStatuses);

        // Nếu không có order → trả null để FE tự xử lý
        if (optional.isEmpty()) {
            return null;
        }

        Order order = optional.get();

        // Tái sử dụng hàm getOrder(id) để map sang OrderResponse
        // Vì trong đó đã có logic truy vấn bảng order_item và map sang OrderItemResponse
        return getOrderDetail(order.getId());
    }

    /**
     * Cập nhật lại danh sách món trong order.
     * ------------------------------------------------------------
     * Dùng cho POS:
     *  - Khi nhân viên thêm / bớt món rồi nhấn "Gửi Order"
     *  - Nếu bàn đã có order đang mở (NEW / SERVING)
     *    → hệ thống sửa lại danh sách món hiện tại
     *
     * OPTION 1 – NHIỀU ORDER_ITEM CHO CÙNG 1 MÓN:
     *  - 1 dishId có thể có nhiều OrderItem (VD: phần cũ đã gửi bếp,
     *    phần mới vẫn ở trạng thái NEW)
     *  - Không còn constraint UNIQUE (order_id, dish_id) ở DB
     *  - Trong code:
     *      + Group theo dishId → List<OrderItem>
     *      + So sánh số lượng mới (từ FE) với tổng số lượng hiện tại
     *      + Phần chênh lệch nếu là "gọi thêm" → tạo OrderItem mới
     *
     * Quy tắc chính:
     *  - Không được sửa order đã thanh toán (PAID) hoặc đã hủy (CANCELED)
     *  - Nếu trong 1 món có item COOKING / DONE / SENT_TO_KITCHEN (bị khóa):
     *      + Không cho GIẢM tổng quantity
     *      + newQty > oldQty → tạo OrderItem mới cho phần chênh lệch
     *      + newQty = oldQty → giữ nguyên, không sửa gì
     *  - Nếu tất cả item của món đều là NEW hoặc SENT_TO_KITCHEN (và
     *    cho phép sửa sau khi gửi bếp):
     *      + Có thể tăng / giảm quantity
     *      + newQty = 0 và allowCancelItem = true → set CANCELED cho tất cả
     * ------------------------------------------------------------
     * @param orderId  id order cần sửa
     * @param reqItems danh sách món FE gửi lên (mỗi dish 1 dòng, quantity tổng)
     */
    @Transactional
    public OrderResponse updateOrderItems(Long orderId, List<OrderItemRequest> reqItems) {

        // ----------------------------------------------------------------
        // 1. Lấy order + validate trạng thái
        // ----------------------------------------------------------------
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELED) {
            throw new RuntimeException("Không thể sửa đơn đã thanh toán hoặc đã hủy");
        }

        boolean allowEditAfterSend = isAllowEditAfterSend();
        boolean allowCancelItem = isAllowCancelItem();
        boolean autoSendKitchen = isAutoSendKitchen();

        // ----------------------------------------------------------------
        // 2. Lấy toàn bộ OrderItem hiện tại của order
        //    và group theo dishId → List<OrderItem>
        // ----------------------------------------------------------------
        List<OrderItem> existingItems = orderItemRepository.findByOrder_Id(orderId);

        // Map<dishId, List<OrderItem>> – cho phép nhiều item cùng 1 món
        Map<Long, List<OrderItem>> existingMap = existingItems.stream()
                .collect(Collectors.groupingBy(oi -> oi.getDish().getId()));

        List<OrderItem> toSave = new ArrayList<>();

        // Dùng để biết dishId nào vẫn còn trong request (sau này xử lý xoá)
        Set<Long> reqDishIds = reqItems.stream()
                .map(OrderItemRequest::getDishId)
                .collect(Collectors.toSet());

        // ============================================================
        // 3. Xử lý từng món trong request (mỗi dishId xuất hiện 1 lần)
        // ============================================================
        for (OrderItemRequest req : reqItems) {

            Long dishId = req.getDishId();
            int newQty = req.getQuantity();

            Dish dish = dishRepository.findById(dishId)
                    .orElseThrow(() -> new RuntimeException("Món không tồn tại"));

            // Danh sách OrderItem hiện có của món này (có thể rỗng)
            List<OrderItem> dishItems = existingMap.getOrDefault(dishId, new ArrayList<>());

            // Các item đang "active" (không bị hủy)
            List<OrderItem> activeItems = dishItems.stream()
                    .filter(oi -> oi.getStatus() != OrderItemStatus.CANCELED)
                    .collect(Collectors.toList());

            // --------------------------------------------------------
            // 3.1. Trường hợp món hoàn toàn mới (chưa có OrderItem nào)
            // --------------------------------------------------------
            if (activeItems.isEmpty()) {

                // Nếu quantity <= 0 → coi như không order món này
                if (newQty <= 0) {
                    continue;
                }

                // Trạng thái khởi tạo theo POS setting
                OrderItemStatus initialStatus = autoSendKitchen
                        ? OrderItemStatus.SENT_TO_KITCHEN
                        : OrderItemStatus.NEW;

                OrderItem newItem = OrderItem.builder()
                        .order(order)
                        .dish(dish)
                        .snapshotPrice(dish.getPrice())
                        .quantity(newQty)
                        .status(initialStatus)
                        .note(req.getNote())
                        .build();

                toSave.add(newItem);
                continue;
            }

            // --------------------------------------------------------
            // 3.2. Món đã tồn tại trong order → tính tổng quantity hiện tại
            // --------------------------------------------------------
            int currentTotalQty = activeItems.stream()
                    .mapToInt(OrderItem::getQuantity)
                    .sum();

            boolean hasCookingOrDone = activeItems.stream().anyMatch(oi ->
                    oi.getStatus() == OrderItemStatus.COOKING
                            || oi.getStatus() == OrderItemStatus.DONE
            );

            boolean hasSentLocked = activeItems.stream().anyMatch(oi ->
                    oi.getStatus() == OrderItemStatus.SENT_TO_KITCHEN && !allowEditAfterSend
            );

            // ========================================================
            // CASE A: Có item đang COOKING / DONE / SENT (không cho sửa)
            //  → xem như phần hiện tại là "khoá" số lượng
            //  → chỉ cho gọi thêm, không cho giảm
            // ========================================================
            if (hasCookingOrDone || hasSentLocked) {

                if (newQty < currentTotalQty) {
                    // Không được giảm phần đã gửi bếp / đang nấu / đã xong
                    throw new RuntimeException(
                            "Không thể giảm số lượng món đang chế biến / đã gửi bếp: " + dish.getName()
                    );
                }

                if (newQty == currentTotalQty) {
                    // Không thay đổi gì → giữ nguyên các OrderItem cũ
                    continue;
                }

                // newQty > currentTotalQty → khách gọi thêm
                int additional = newQty - currentTotalQty;

                OrderItemStatus initialStatus = autoSendKitchen
                        ? OrderItemStatus.SENT_TO_KITCHEN
                        : OrderItemStatus.NEW;

                OrderItem extraItem = OrderItem.builder()
                        .order(order)
                        .dish(dish)
                        .snapshotPrice(dish.getPrice())  // snapshot giá hiện tại
                        .quantity(additional)
                        .status(initialStatus)
                        .note(req.getNote())             // ghi chú cho phần gọi thêm (nếu có)
                        .build();

                toSave.add(extraItem);
                continue;
            }

            // ========================================================
            // CASE B: Không có item COOKING / DONE / SENT bị khóa
            //  → Tất cả đều ở trạng thái:
            //      NEW
            //      hoặc SENT_TO_KITCHEN nhưng allowEditAfterSend = true
            //  → Có thể tăng/giảm số lượng, hủy món nếu allowCancelItem
            // ========================================================

            // B1. newQty = 0 → hủy toàn bộ món này
            if (newQty == 0) {
                if (!allowCancelItem) {
                    throw new RuntimeException("Không được phép hủy món theo cấu hình POS");
                }

                for (OrderItem oi : activeItems) {
                    oi.setStatus(OrderItemStatus.CANCELED);
                    toSave.add(oi);
                }
                continue;
            }

            // B2. newQty > 0 → gộp về 1 item chính, các item còn lại hủy (nếu được)
            //     Mục tiêu:
            //       - Database không phình ra quá nhiều dòng NEW trùng nhau
            //       - FE luôn gửi 1 dòng / 1 dish → quantity tổng

            // Item chính (lấy item đầu tiên trong danh sách active)
            OrderItem mainItem = activeItems.get(0);
            mainItem.setQuantity(newQty);
            mainItem.setNote(req.getNote()); // cập nhật note mới (nếu cần)
            toSave.add(mainItem);

            // Các item thừa còn lại → nếu cho phép hủy thì set CANCELED
            for (int i = 1; i < activeItems.size(); i++) {
                OrderItem extra = activeItems.get(i);
                if (allowCancelItem) {
                    extra.setStatus(OrderItemStatus.CANCELED);
                }
                toSave.add(extra);
            }
        }

        // ============================================================
        // 4. Xử lý các OrderItem KHÔNG còn xuất hiện trong request
        //    (tức là FE không gửi dishId đó nữa) → coi như hủy món
        // ============================================================
        for (OrderItem ex : existingItems) {
            Long dishId = ex.getDish().getId();

            // Nếu dishId vẫn còn trong request → đã xử lý ở bước 3
            if (reqDishIds.contains(dishId)) {
                continue;
            }

            // Nếu không cho hủy món → chặn
            if (!allowCancelItem) {
                throw new RuntimeException("Không được phép hủy món theo cấu hình POS.");
            }

            // Không cho hủy món đã gửi bếp mà không cho sửa
            if (ex.getStatus() == OrderItemStatus.SENT_TO_KITCHEN && !allowEditAfterSend) {
                throw new RuntimeException("Không thể hủy món đã gửi bếp: " + ex.getDish().getName());
            }

            // Không cho hủy món đang nấu / đã xong
            if (ex.getStatus() == OrderItemStatus.COOKING || ex.getStatus() == OrderItemStatus.DONE) {
                throw new RuntimeException("Không thể hủy món đang chế biến: " + ex.getDish().getName());
            }

            // Thực tế: thay vì DELETE luôn, ta set CANCELED cho thống nhất
            ex.setStatus(OrderItemStatus.CANCELED);
            toSave.add(ex);
        }

        // ============================================================
        // 5. Lưu thay đổi + tính lại tổng tiền
        // ============================================================
        // Lưu toàn bộ item mới / item đã cập nhật
        if (!toSave.isEmpty()) {
            orderItemRepository.saveAll(toSave);
        }

        // Lấy lại toàn bộ OrderItem sau khi update để tính tổng tiền
        List<OrderItem> updatedItems = orderItemRepository.findByOrder_Id(orderId);

        BigDecimal total = updatedItems.stream()
                .filter(oi -> oi.getStatus() != OrderItemStatus.CANCELED)
                .map(oi -> oi.getSnapshotPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalPrice(total);
        orderRepository.save(order);

        // Trả về OrderResponse mới nhất
        return toOrderResponse(order, updatedItems);
    }

    /**
     * Tạo order nhanh cho chế độ Simple POS.
     * ----------------------------------------------------------------
     * Luồng dành riêng cho:
     *  - pos.simple_pos_mode = true
     *  - Quán nhỏ / takeaway: chọn món → thanh toán luôn.
     *
     * Khác với createOrder:
     *  - Request đơn giản hơn (SimpleOrderRequest)
     *  - Không xử lý logic update món phức tạp
     *  - Không gửi bếp (OrderItem luôn ở trạng thái NEW)
     *
     * Bước xử lý:
     *  1) Validate request (phải có ít nhất 1 món)
     *  2) Kiểm tra POS Settings: simple_pos_mode + simple_pos_require_table
     *  3) Load danh sách món, tính tổng tiền
     *  4) Tạo Order với status = SERVING (cho phép thanh toán ngay)
     *  5) Gán bàn (nếu có tableId) → markTableOccupied
     *  6) Tạo OrderItem (snapshotPrice, status NEW, note từ request)
     *  7) Trừ kho theo RecipeItem (giống createOrder)
     *  8) Gửi notification + audit log
     *  9) Trả về OrderResponse
     */
    @Transactional
    public OrderResponse simpleCreate(SimpleOrderRequest req, String username) {

        // ------------------------------------------------------------
        // 1) VALIDATE CƠ BẢN
        // ------------------------------------------------------------
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new RuntimeException("Order phải có ít nhất 1 món (Simple POS).");
        }

        // Lấy userId từ username để set createdBy
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        Long userId = user.getId();

        // ------------------------------------------------------------
        // 2) ĐỌC CẤU HÌNH POS & VALIDATE THEO SIMPLE MODE
        // ------------------------------------------------------------
        boolean simplePosMode = isSimplePosMode();
        boolean simplePosRequireTable = isSimplePosRequireTable();

        if (!simplePosMode) {
            // Nếu hệ thống chưa bật Simple POS → chặn luôn, tránh dùng nhầm API
            throw new RuntimeException("Hệ thống chưa bật chế độ POS đơn giản (Simple POS Mode).");
        }

        Long tableId = req.getTableId();

        if (simplePosRequireTable && tableId == null) {
            // Nếu simple_pos_require_table = true thì bắt buộc phải chọn bàn
            throw new RuntimeException("Chế độ POS đơn giản yêu cầu phải chọn bàn trước khi tạo order.");
        }
        // Nếu require_table = false → cho phép không gửi tableId (order mang tính "không gán bàn")

        // ------------------------------------------------------------
        // 3) LOAD DANH SÁCH MÓN & TÍNH TỔNG TIỀN
        // ------------------------------------------------------------
        List<Long> dishIds = req.getItems()
                .stream()
                .map(SimpleOrderItemRequest::getDishId)
                .toList();

        List<Dish> dishes = dishRepository.findAllById(dishIds);
        if (dishes.size() != dishIds.size()) {
            throw new RuntimeException("Có món ăn không tồn tại trong hệ thống (Simple POS).");
        }

        Map<Long, Dish> dishMap = dishes.stream()
                .collect(Collectors.toMap(Dish::getId, d -> d));

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (SimpleOrderItemRequest itemReq : req.getItems()) {
            Dish dish = dishMap.get(itemReq.getDishId());
            if (dish == null) {
                throw new RuntimeException("Món ăn với ID " + itemReq.getDishId() + " không tồn tại.");
            }
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new RuntimeException("Số lượng món phải lớn hơn 0 (Simple POS).");
            }

            BigDecimal price = dish.getPrice();
            BigDecimal qty = BigDecimal.valueOf(itemReq.getQuantity());
            totalPrice = totalPrice.add(price.multiply(qty));
        }

        // ------------------------------------------------------------
        // 4) TẠO ENTITY ORDER (status = SERVING để thanh toán ngay)
        // ------------------------------------------------------------
        Order order = Order.builder()
                .orderCode(generateOrderCode())   // Mã đơn tự sinh
                .totalPrice(totalPrice)
                .status(OrderStatus.SERVING)      // Simple POS: coi như đang phục vụ, có thể thanh toán luôn
                .note(null)                       // SimpleOrderRequest hiện chưa có note cho order
                .createdBy(userId)
                .build();

        Order saved = orderRepository.save(order);

        // ------------------------------------------------------------
        // 5) GÁN BÀN CHO ORDER (nếu có tableId)
        // ------------------------------------------------------------
        if (tableId != null) {
            // Đánh dấu bàn đang được sử dụng (OCCUPIED)
            RestaurantTable table = restaurantTableService.markTableOccupied(tableId);

            // Gán bàn cho order
            saved.setTable(table);

            // Lưu lại order sau khi gán bàn
            orderRepository.save(saved);
        }

        // ------------------------------------------------------------
        // 6) TẠO DANH SÁCH ORDER ITEM CHO SIMPLE POS
        // ------------------------------------------------------------
        List<OrderItem> orderItems = new ArrayList<>();

        for (SimpleOrderItemRequest itemReq : req.getItems()) {

            Dish dish = dishMap.get(itemReq.getDishId());
            if (dish == null) {
                throw new RuntimeException("Món ăn với ID " + itemReq.getDishId() + " không tồn tại.");
            }

            // Giá snapshot tại thời điểm order (theo Rule 26 – BigDecimal)
            BigDecimal snapshotPrice = dish.getPrice();

            // Simple POS: KHÔNG gửi bếp → luôn coi như đã hoàn thành
            // ------------------------------------------------------------
            // Lý do:
            //  - Món Simple POS chỉ dùng để in hóa đơn / xem lịch sử
            //  - Không cần hiển thị trên màn hình bếp (Kitchen)
            //  - KitchenService chỉ lấy các món có status NEW/SENT_TO_KITCHEN/COOKING
            //    nên DONE sẽ luôn bị bỏ qua.
            // ------------------------------------------------------------
            OrderItemStatus initialStatus = OrderItemStatus.DONE;

            OrderItem oi = OrderItem.builder()
                    .order(saved)                // Quan hệ ManyToOne tới Order
                    .dish(dish)                  // Quan hệ ManyToOne tới Dish
                    .snapshotPrice(snapshotPrice)// Giá snapshot
                    .quantity(itemReq.getQuantity())
                    .status(initialStatus)       // DONE → KHÔNG bao giờ lên Kitchen
                    .note(itemReq.getNote())     // Ghi chú món nếu có
                    .build();

            orderItems.add(oi);
        }

        orderItemRepository.saveAll(orderItems);

        // ------------------------------------------------------------
        // 7) TRỪ KHO THEO RECIPE (TÁI SỬ DỤNG HÀM CŨ)
        // ------------------------------------------------------------
        consumeStockForOrder(saved, orderItems);

        // ------------------------------------------------------------
        // 8) GỬI THÔNG BÁO QUA RULE ENGINE (Phase 4.3)
        // ------------------------------------------------------------
        // - Không gọi notificationService.createNotification trực tiếp nữa.
        // - Rule Engine sẽ tự kiểm tra bật/tắt + chống spam.
        notificationRuleService.onOrderCreated(saved, orderItems);

        // Audit log tạo order
        auditLogService.log(
                AuditAction.ORDER_CREATE,
                "order",
                saved.getId(),
                null,
                saved
        );

        // ------------------------------------------------------------
        // 9) TRẢ VỀ DTO ORDER RESPONSE
        // ------------------------------------------------------------
        return toOrderResponse(saved, orderItems);
    }

}
