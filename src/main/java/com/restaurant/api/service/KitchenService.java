package com.restaurant.api.service;

import com.restaurant.api.dto.kitchen.KitchenItemResponse;
import com.restaurant.api.dto.kitchen.KitchenOrderResponse;
import com.restaurant.api.dto.kitchen.UpdateKitchenItemStatusRequest;
import com.restaurant.api.entity.Dish;
import com.restaurant.api.entity.Order;
import com.restaurant.api.entity.OrderItem;
import com.restaurant.api.entity.RestaurantTable;
import com.restaurant.api.enums.OrderItemStatus;
import com.restaurant.api.enums.OrderStatus;
import com.restaurant.api.event.RealtimeEventPublisher;
import com.restaurant.api.repository.OrderItemRepository;
import com.restaurant.api.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * KitchenService
 * ------------------------------------------------------------
 * Service chuyên xử lý nghiệp vụ cho MÀN HÌNH BẾP (Kitchen).
 *
 * Chức năng chính:
 *  - Lấy danh sách món cần chế biến (theo trạng thái)
 *  - Cập nhật trạng thái 1 món (NEW → SENT_TO_KITCHEN → COOKING → DONE)
 *
 * Thiết kế:
 *  - Không xử lý thanh toán tại đây
 *  - Không trừ/hoàn kho tại đây (đã làm ở OrderService)
 *  - Chỉ tập trung vào trạng thái món + hỗ trợ update trạng thái Order
 *    (khi auto_order_serving_on_item_cooking = true).
 */
@Service
@RequiredArgsConstructor
public class KitchenService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final SystemSettingService systemSettingService;
    /**
     * Publisher bắn realtime qua WebSocket (Phase 5.2.4)
     * - Dùng topic: /topic/kitchen
     * - Payload: KitchenItemResponse (DTO đã có sẵn)
     */
    private final RealtimeEventPublisher realtimeEventPublisher;

    // ------------------------------------------------------------
    // HÀM ĐỌC CẤU HÌNH POS CHO BẾP
    // ------------------------------------------------------------

    /**
     * Đọc cấu hình:
     *  - Nếu true: khi có ít nhất 1 món chuyển sang COOKING,
     *    hệ thống sẽ tự chuyển trạng thái ORDER từ NEW → SERVING.
     *
     * Key: pos.auto_order_serving_on_item_cooking
     */
    private boolean isAutoOrderServingOnItemCooking() {
        return systemSettingService.getBooleanSetting(
                "pos.auto_order_serving_on_item_cooking",
                false
        );
    }

    /**
     * Đọc cấu hình: có cho phép hủy món hay không.
     * Key: pos.allow_cancel_item
     */
    private boolean isAllowCancelItem() {
        return systemSettingService.getBooleanSetting(
                "pos.allow_cancel_item",
                true // default: hệ thống cho phép hủy món
        );
    }

    /**
     * Kiểm tra cấu hình: có tự động gửi món xuống bếp
     * ngay sau khi tạo order hay không.
     */
    private boolean isAutoSendKitchen() {
        return systemSettingService.getBooleanSetting("pos.auto_send_kitchen", false);
    }

    // =====================================================================
    // 1. LẤY DANH SÁCH MÓN CHO BẾP
    // =====================================================================

    /**
     * Lấy danh sách món hiển thị trên màn hình bếp.
     * ------------------------------------------------------------
     * Quy ước:
     *  - Nếu statusParam = null:
     *      + Lấy các món có status ∈ {NEW, SENT_TO_KITCHEN, COOKING}
     *      + Bỏ qua CANCELED, DONE
     *  - Nếu statusParam != null:
     *      + Lọc đúng theo status được gửi lên
     *
     * Ngoài ra:
     *  - Chỉ lấy các món thuộc Order có trạng thái != CANCELED
     *    (order đã hủy thì không cần hiển thị trên bếp nữa)
     *
     * @param statusParam trạng thái filter (có thể null)
     * @return danh sách KitchenItemResponse
     */
    @Transactional(readOnly = true)
    public List<KitchenItemResponse> getKitchenItems(OrderItemStatus statusParam) {

        // Lấy toàn bộ OrderItem rồi lọc ở BE
        List<OrderItem> allItems = orderItemRepository.findAll();
        if (allItems.isEmpty()) {
            return Collections.emptyList();
        }

        // Nếu không truyền status → mặc định lấy các trạng thái đang xử lý
        List<OrderItemStatus> defaultStatuses = List.of(
                OrderItemStatus.NEW,
                OrderItemStatus.SENT_TO_KITCHEN,
                OrderItemStatus.COOKING
        );

        return allItems.stream()
                // Bỏ qua món không gắn order (phòng dữ liệu bẩn)
                .filter(oi -> oi.getOrder() != null)
                // Bỏ qua các order đã hủy
                .filter(oi -> oi.getOrder().getStatus() != OrderStatus.CANCELED)
                // Lọc theo status
                .filter(oi -> {
                    OrderItemStatus st = oi.getStatus();
                    if (statusParam != null) {
                        return st == statusParam;
                    }
                    return defaultStatuses.contains(st);
                })
                // Map sang DTO cho FE
                .sorted(Comparator.comparing(
                        (OrderItem oi) -> oi.getOrder().getCreatedAt()
                ))
                .map(this::toKitchenItemResponse)
                .collect(Collectors.toList());
    }

    /**
     * Hàm convert Entity OrderItem → KitchenItemResponse.
     * ------------------------------------------------------------
     * Gom đầy đủ thông tin:
     *  - Order, Table, Dish, Quantity, Status, CreatedAt
     */
    private KitchenItemResponse toKitchenItemResponse(OrderItem item) {
        Order order = item.getOrder();
        Dish dish = item.getDish();
        RestaurantTable table = order.getTable(); // có thể null

        return KitchenItemResponse.builder()
                .orderItemId(item.getId())
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .tableId(table != null ? table.getId() : null)
                .tableName(table != null ? table.getName() : null)
                .dishId(dish != null ? dish.getId() : null)
                .dishName(dish != null ? dish.getName() : null)
                .quantity(item.getQuantity())
                .status(item.getStatus())
                .note(item.getNote())
                .createdAt(item.getCreatedAt())
                .build();
    }

    // =====================================================================
    // 2. CẬP NHẬT TRẠNG THÁI 1 MÓN TRONG BẾP
    // =====================================================================

    /**
     * Cập nhật trạng thái của 1 OrderItem từ màn hình bếp.
     * ------------------------------------------------------------
     * Rule chuyển trạng thái hợp lệ:
     *  - NEW             → SENT_TO_KITCHEN / CANCELED
     *  - SENT_TO_KITCHEN → COOKING / CANCELED
     *  - COOKING         → DONE
     *
     *  Trạng thái DONE / CANCELED → không được phép chuyển nữa.
     *
     * Ngoài ra:
     *  - Nếu newStatus = COOKING và cấu hình
     *      pos.auto_order_serving_on_item_cooking = true
     *    thì:
     *      + Nếu ORDER đang ở NEW → tự chuyển ORDER → SERVING
     *
     * @param orderItemId ID bản ghi OrderItem cần update
     * @param req         request chứa trạng thái mới + ghi chú
     * @return KitchenItemResponse sau khi update
     */
    @Transactional
    public KitchenItemResponse updateItemStatus(Long orderItemId,
                                                UpdateKitchenItemStatusRequest req) {
        // 1) Load OrderItem
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy món trong OrderItem"));

        OrderItemStatus oldStatus = item.getStatus();
        OrderItemStatus newStatus = req.getNewStatus();

        if (newStatus == null) {
            throw new RuntimeException("Trạng thái mới không được để trống");
        }

        // ------------------------------------------------------------
        // Nếu auto_send_kitchen = true thì không cho phép gọi API NEW → SENT_TO_KITCHEN
        // ------------------------------------------------------------
        if (newStatus == OrderItemStatus.SENT_TO_KITCHEN && isAutoSendKitchen()) {
            throw new RuntimeException("Hệ thống tự động gửi món xuống bếp, không cần gửi thủ công.");
        }

        // 2) ĐỌC POS SETTINGS
        boolean allowCancelItem = isAllowCancelItem();
        boolean autoOrderServing = isAutoOrderServingOnItemCooking();

        // 3) Nếu yêu cầu hủy món nhưng hệ thống không cho phép hủy → chặn
        if (newStatus == OrderItemStatus.CANCELED && !allowCancelItem) {
            throw new RuntimeException("Cấu hình POS không cho phép hủy món.");
        }

        // 4) Kiểm tra rule chuyển trạng thái
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new RuntimeException(
                    "Không thể chuyển trạng thái món từ " + oldStatus + " sang " + newStatus);
        }

        // 5) Cập nhật trạng thái và ghi chú
        item.setStatus(newStatus);
        if (req.getNote() != null && !req.getNote().isBlank()) {
            item.setNote(req.getNote());
        }

        orderItemRepository.save(item);


        // 6) Nếu chuyển sang COOKING và bật auto_order_serving → update ORDER
        if (newStatus == OrderItemStatus.COOKING && autoOrderServing) {
            Order order = item.getOrder();
            if (order.getStatus() == OrderStatus.NEW) {
                order.setStatus(OrderStatus.SERVING);
                orderRepository.save(order);
            }
        }

        // 7) Convert sang DTO để trả về FE
        KitchenItemResponse resp = toKitchenItemResponse(item);

        /**
         * 8) BẮN REALTIME CHO BẾP (Phase 5.2.4)
         * ------------------------------------------------------------
         * Mục tiêu:
         *  - Khi 1 món đổi trạng thái (SENT_TO_KITCHEN/COOKING/DONE/CANCELED)
         *    thì các màn hình bếp đang mở sẽ nhận được update ngay lập tức.
         *
         * Payload dùng DTO KitchenItemResponse để:
         *  - FE dùng luôn không phải map lại
         *  - Đồng nhất với API /api/kitchen/items
 */
        realtimeEventPublisher.publishKitchen(resp);

        return resp;
    }

    /**
     * Kiểm tra rule chuyển trạng thái của OrderItem.
     * ------------------------------------------------------------
     * Hợp lệ:
     *  - NEW             → SENT_TO_KITCHEN / CANCELED
     *  - SENT_TO_KITCHEN → COOKING / CANCELED
     *  - COOKING         → DONE
     *
     *  Còn lại: không cho phép.
     */
    private boolean isValidStatusTransition(OrderItemStatus from, OrderItemStatus to) {
        if (from == OrderItemStatus.NEW) {
            return to == OrderItemStatus.SENT_TO_KITCHEN
                    || to == OrderItemStatus.CANCELED;
        }
        if (from == OrderItemStatus.SENT_TO_KITCHEN) {
            return to == OrderItemStatus.COOKING
                    || to == OrderItemStatus.CANCELED;
        }
        if (from == OrderItemStatus.COOKING) {
            return to == OrderItemStatus.DONE;
        }
        // DONE / CANCELED → không cho phép chuyển nữa
        return false;
    }

    // =====================================================================
// 3. LẤY DANH SÁCH ORDER + DANH SÁCH MÓN CHO MÀN HÌNH BẾP
// =====================================================================

    /**
     * Lấy danh sách ORDER cho màn hình bếp (KDS).
     * ----------------------------------------------------------------
     * Khác với getKitchenItems():
     *  - getKitchenItems() trả về từng món rời rạc (KitchenItemResponse)
     *  - Hàm này group theo ORDER → KitchenOrderResponse:
     *      + orderId, orderCode, tableName, createdAt
     *      + items: danh sách KitchenItemResponse thuộc order đó
     *
     * Quy ước filter:
     *  - Chỉ lấy OrderItem có status ∈ {NEW, SENT_TO_KITCHEN, COOKING}
     *  - Bỏ qua:
     *      + Order đã CANCELED
     *      + Món DONE hoặc CANCELED
     */
    @Transactional(readOnly = true)
    public List<KitchenOrderResponse> getKitchenOrders() {

        // 1) Lấy toàn bộ OrderItem rồi lọc theo rule
        List<OrderItem> allItems = orderItemRepository.findAll();
        if (allItems.isEmpty()) {
            return Collections.emptyList();
        }

        // Các trạng thái món cần hiển thị trên bếp
        List<OrderItemStatus> displayStatuses = List.of(
                OrderItemStatus.NEW,
                OrderItemStatus.SENT_TO_KITCHEN,
                OrderItemStatus.COOKING
        );

        List<OrderItem> filtered = allItems.stream()
                // Bỏ món không gắn order (phòng dữ liệu bẩn)
                .filter(oi -> oi.getOrder() != null)
                // Bỏ order đã hủy
                .filter(oi -> oi.getOrder().getStatus() != OrderStatus.CANCELED)
                // Chỉ lấy món đang ở trạng thái NEW/SENT_TO_KITCHEN/COOKING
                //.filter(oi -> displayStatuses.contains(oi.getStatus()))
                // Sắp xếp theo thời điểm tạo ORDER (order cũ nằm trên)
                .sorted(Comparator.comparing(
                        (OrderItem oi) -> oi.getOrder().getCreatedAt()
                ))
                .toList();

        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) Group theo ORDER
        Map<Order, List<OrderItem>> itemsByOrder = filtered.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrder));

        List<KitchenOrderResponse> result = new ArrayList<>();

        for (Map.Entry<Order, List<OrderItem>> entry : itemsByOrder.entrySet()) {
            Order order = entry.getKey();
            List<OrderItem> orderItems = entry.getValue();

            // Lấy tên bàn (có thể null)
            RestaurantTable table = order.getTable();
            String tableName = table != null ? table.getName() : null;

            // Map từng OrderItem → KitchenItemResponse (tái dùng hàm cũ)
            List<KitchenItemResponse> itemDTOs = orderItems.stream()
                    .map(this::toKitchenItemResponse)
                    .toList();

            KitchenOrderResponse dto = KitchenOrderResponse.builder()
                    .orderId(order.getId())
                    .orderCode(order.getOrderCode())
                    .tableName(tableName)
                    .createdAt(order.getCreatedAt())
                    .items(itemDTOs)
                    .build();

            result.add(dto);
        }

        // 3) Sắp xếp danh sách ORDER theo thời gian (cũ → mới)
        result.sort(Comparator.comparing(KitchenOrderResponse::getCreatedAt));

        return result;
    }
}
