package com.restaurant.api.service;

import com.restaurant.api.dto.notification.CreateNotificationRequest;
import com.restaurant.api.dto.order.*;
import com.restaurant.api.entity.*;
import com.restaurant.api.enums.AuditAction;
import com.restaurant.api.enums.NotificationType;
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
    private final AuditLogService auditLogService;
    // ✅ Service quản lý bàn (Module 16)
    private final RestaurantTableService restaurantTableService;
    // ✅ Service đọc cấu hình hệ thống (Module 20)
    private final SystemSettingService systemSettingService;


    // =================================================================
    // 1. TẠO ORDER MỚI
    // =================================================================

    /**
     * Tạo order mới từ request FE.
     * ------------------------------------------------------------
     * Bước xử lý:
     *  1. Validate request (phải có ít nhất 1 món)
     *  2. Load danh sách món từ DB, tính tổng tiền
     *  3. Lưu Order + OrderItem
     *  4. Gọi hàm trừ kho theo RecipeItem
     *
     * @param req      request tạo order (danh sách món + ghi chú)
     * @param username   ID user đang đăng nhập (người tạo đơn)
     * @return OrderResponse đầy đủ (gồm danh sách món)
     */
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest req, String username) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new RuntimeException("Order phải có ít nhất 1 món");
        }

        // 👉 Nếu bạn muốn lưu userId thật, tra từ username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        Long userId = user.getId();   // 🟢 userId đã đúng 100%

        // 1) Lấy danh sách dishId từ request
        List<Long> dishIds = req.getItems()
                .stream()
                .map(OrderItemRequest::getDishId)
                .toList();

        // 2) Load toàn bộ món từ DB 1 lần
        List<Dish> dishes = dishRepository.findAllById(dishIds);
        if (dishes.size() != dishIds.size()) {
            throw new RuntimeException("Có món ăn không tồn tại trong hệ thống");
        }

        // Map dishId → Dish để dùng nhanh
        Map<Long, Dish> dishMap = dishes.stream()
                .collect(Collectors.toMap(Dish::getId, d -> d));

        // 3) Tính tổng tiền
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : req.getItems()) {
            Dish dish = dishMap.get(itemReq.getDishId());
            BigDecimal price = dish.getPrice();
            BigDecimal qty = BigDecimal.valueOf(itemReq.getQuantity());
            totalPrice = totalPrice.add(price.multiply(qty));
        }

        // 4) Tạo entity Order (chưa lưu OrderItem)
        Order order = Order.builder()
                .orderCode(generateOrderCode())   // Mã đơn tự sinh
                .totalPrice(totalPrice)
                .status(OrderStatus.NEW)
                .note(req.getNote())
                .createdBy(userId)
                .build();

        Order saved = orderRepository.save(order);

        // =====================================================================
        // MODULE 16 – GÁN BÀN CHO ORDER (nếu FE gửi tableId)
        // =====================================================================
        if (req.getTableId() != null) {
            // Đánh dấu bàn đang được sử dụng (OCCUPIED)
            RestaurantTable table = restaurantTableService.markTableOccupied(req.getTableId());

            // Gán bàn cho order
            saved.setTable(table);

            // Lưu lại order sau khi gán bàn
            orderRepository.save(saved);
        }

        // =====================================================================
        // GỬI THÔNG BÁO: Tạo order mới
        // =====================================================================
        CreateNotificationRequest re = new CreateNotificationRequest();
        re.setTitle("Tạo order mới");
        re.setType(NotificationType.ORDER);
        re.setMessage("Tạo order mới");
        re.setLink("");
        notificationService.createNotification(re);

        // 5) Tạo danh sách OrderItem
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemRequest itemReq : req.getItems()) {
            OrderItem oi = OrderItem.builder()
                    .orderId(order.getId())
                    .dishId(itemReq.getDishId())
                    .quantity(itemReq.getQuantity())
                    .build();
            orderItems.add(oi);
        }
        orderItemRepository.saveAll(orderItems);

        // 6) Trừ kho theo RecipeItem (tiêu nguyên liệu)
        consumeStockForOrder(order, orderItems);

        // =====================================================================
        // GỬI THÔNG BÁO: Tiêu nguyên liệu
        // =====================================================================
        CreateNotificationRequest res = new CreateNotificationRequest();
        res.setTitle("Tiêu nguyên liệu");
        res.setType(NotificationType.ORDER);
        res.setMessage("Tiêu nguyên liệu khi order");
        res.setLink("");
        notificationService.createNotification(res);

        // ✅ Audit log tạo order
        auditLogService.log(
                AuditAction.ORDER_CREATE,
                "order",
                order.getId(),
                null,
                order
        );

        // 7) Trả về DTO order đầy đủ
        return toOrderResponse(order, orderItems, dishMap);
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
        List<OrderItem> allItems = orderItemRepository.findAll()
                .stream()
                .filter(oi -> orderIds.contains(oi.getOrderId()))
                .toList();

        // Lấy toàn bộ dishId để map thông tin món
        Set<Long> dishIds = allItems.stream()
                .map(OrderItem::getDishId)
                .collect(Collectors.toSet());
        Map<Long, Dish> dishMap = dishRepository.findAllById(dishIds)
                .stream()
                .collect(Collectors.toMap(Dish::getId, d -> d));

        // Group orderItem theo orderId
        Map<Long, List<OrderItem>> itemsByOrder = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        // Convert từng order → OrderResponse
        List<OrderResponse> result = new ArrayList<>();
        for (Order o : orders) {
            List<OrderItem> items = itemsByOrder.getOrDefault(o.getId(), List.of());
            result.add(toOrderResponse(o, items, dishMap));
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

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        // Lấy danh sách dish 1 lần
        Set<Long> dishIds = items.stream()
                .map(OrderItem::getDishId)
                .collect(Collectors.toSet());
        Map<Long, Dish> dishMap = dishRepository.findAllById(dishIds)
                .stream()
                .collect(Collectors.toMap(Dish::getId, d -> d));

        return toOrderResponse(order, items, dishMap);
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
            List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
            restoreStockForOrder(order, items);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        // =====================================================================
        // GỬI THÔNG BÁO: Update order
        // =====================================================================
        CreateNotificationRequest re = new CreateNotificationRequest();
        re.setTitle("Update order");
        re.setType(NotificationType.ORDER);
        re.setMessage("Chuyển trạng thái order thành " + newStatus);
        re.setLink("");
        notificationService.createNotification(re);

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

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        // Hoàn kho trước rồi mới xóa order
        restoreStockForOrder(order, items);

        orderItemRepository.deleteByOrderId(orderId);
        orderRepository.delete(order);

        // ✅ Audit log cancel order
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
            Long dishId = item.getDishId();
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
            Long dishId = item.getDishId();
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
     * Convert Order + danh sách OrderItem + Map Dish → OrderResponse
     */
    private OrderResponse toOrderResponse(Order order,
                                          List<OrderItem> items,
                                          Map<Long, Dish> dishMap) {

        List<OrderItemResponse> itemResponses = new ArrayList<>();

        for (OrderItem item : items) {
            Dish dish = dishMap.get(item.getDishId());
            if (dish == null) {
                continue; // Không tìm thấy món, bỏ qua (tránh crash)
            }

            BigDecimal price = dish.getPrice();
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            BigDecimal subtotal = price.multiply(qty);

            OrderItemResponse itemRes = OrderItemResponse.builder()
                    .dishId(dish.getId())
                    .dishName(dish.getName())
                    .dishPrice(price)
                    .quantity(item.getQuantity())
                    .subtotal(subtotal)
                    .build();

            itemResponses.add(itemRes);
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .totalPrice(order.getTotalPrice())
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
     *  - Khi nhân viên chọn thêm các món mới rồi nhấn "Gửi Order"
     *  - Nếu bàn đã có order đang mở (NEW / SERVING)
     *    → hệ thống sửa lại danh sách món hiện tại
     *
     * Quy trình:
     *  1. Lấy username từ JWT (không cần FE gửi)
     *  2. Tìm order theo orderId
     *  3. Kiểm tra order hợp lệ (không được sửa nếu PAID hoặc CANCELED)
     *  4. Xóa danh sách OrderItem cũ
     *  5. Thêm danh sách món mới
     *  6. Tính lại tổng tiền
     *  7. Lưu order + trả về OrderResponse đầy đủ
     */
    @Transactional
    public OrderResponse updateOrderItems(Long orderId, List<OrderItemRequest> newItems) {

        // 1) Load order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELED) {
            throw new RuntimeException("Không thể sửa đơn đã thanh toán hoặc đã hủy");
        }

        // ----------------------------------------------------------------
        // 🔧 TÍCH HỢP CẤU HÌNH POS: pos.allow_edit_after_send
        // ----------------------------------------------------------------
        //  - Nếu cấu hình = false:
        //      + Chỉ cho phép sửa món khi order đang ở trạng thái NEW
        //      + Khi đã chuyển sang SERVING (coi như đã gửi bếp) → khóa sửa
        //  - Nếu cấu hình = true:
        //      + Cho phép sửa cả khi SERVING (giữ hành vi linh hoạt hơn)
        // ----------------------------------------------------------------
        boolean allowEditAfterSend = systemSettingService.getBooleanSetting(
                "pos.allow_edit_after_send",
                false // default: KHÔNG cho phép sửa sau khi gửi bếp
        );
        if (!allowEditAfterSend && order.getStatus() != OrderStatus.NEW) {
            throw new RuntimeException("Không được sửa món sau khi đơn đã gửi bếp/đang phục vụ.");
        }

        // 2) Xóa toàn bộ item cũ
        orderItemRepository.deleteByOrderId(orderId);

        // 3) Tính tổng tiền mới
        BigDecimal total = BigDecimal.ZERO;

        List<OrderItem> toSave = new ArrayList<>();

        for (OrderItemRequest req : newItems) {

            Dish dish = dishRepository.findById(req.getDishId())
                    .orElseThrow(() -> new RuntimeException("Món không tồn tại"));

            BigDecimal subtotal = dish.getPrice().multiply(BigDecimal.valueOf(req.getQuantity()));
            total = total.add(subtotal);

            OrderItem item = OrderItem.builder()
                    .orderId(orderId)
                    .dishId(req.getDishId())
                    .quantity(req.getQuantity())
                    .build();

            toSave.add(item);
        }

        // 4) Lưu lại toàn bộ item mới
        orderItemRepository.saveAll(toSave);

        // 5) Cập nhật tổng tiền order
        order.setTotalPrice(total);
        orderRepository.save(order);

        // 6) Trả về OrderResponse
        return getOrderDetail(orderId);
    }
}
