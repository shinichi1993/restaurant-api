package com.restaurant.api.service;

import com.restaurant.api.dto.table.*;
import com.restaurant.api.entity.Order;
import com.restaurant.api.entity.RestaurantTable;
import com.restaurant.api.enums.OrderStatus;
import com.restaurant.api.enums.TableStatus;
import com.restaurant.api.repository.OrderRepository;
import com.restaurant.api.repository.RestaurantTableRepository;
import com.restaurant.api.dto.table.PosTableStatusResponse;
import com.restaurant.api.entity.OrderItem;
import com.restaurant.api.enums.OrderItemStatus;
import com.restaurant.api.repository.OrderItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Service xử lý nghiệp vụ cho Module 16 – Quản lý bàn.
 */
@Service
@RequiredArgsConstructor
public class RestaurantTableService {

    private final RestaurantTableRepository restaurantTableRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * Tạo bàn mới.
     */
    @Transactional
    public TableResponse createTable(TableRequest request) {
        // Validate đơn giản: tên bàn không được trống
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tên bàn không được để trống");
        }

        if (restaurantTableRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Tên bàn đã tồn tại, vui lòng chọn tên khác");
        }

        RestaurantTable table = RestaurantTable.builder()
                .name(request.getName())
                .capacity(request.getCapacity() != null ? request.getCapacity() : 1)
                .status(TableStatus.AVAILABLE) // Bàn mới luôn ở trạng thái trống
                .build();

        table = restaurantTableRepository.save(table);
        return toResponse(table);
    }

    /**
     * Cập nhật thông tin bàn (tên, số ghế).
     */
    @Transactional
    public TableResponse updateTable(Long id, TableRequest request) {
        RestaurantTable table = getTableOrThrow(id);

        // Nếu đổi tên bàn → kiểm tra trùng
        if (request.getName() != null && !request.getName().equals(table.getName())) {
            if (restaurantTableRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("Tên bàn đã tồn tại, vui lòng chọn tên khác");
            }
            table.setName(request.getName());
        }

        if (request.getCapacity() != null && request.getCapacity() > 0) {
            table.setCapacity(request.getCapacity());
        }

        return toResponse(table);
    }

    /**
     * Xóa bàn.
     * Chỉ cho phép xóa khi bàn KHÔNG có order đang mở (NEW, SERVING).
     */
    @Transactional
    public void deleteTable(Long id) {
        RestaurantTable table = getTableOrThrow(id);

        // Kiểm tra có order đang mở không
        List<OrderStatus> openStatuses = Arrays.asList(OrderStatus.NEW, OrderStatus.SERVING);
        boolean hasOpenOrder = orderRepository
                .findFirstByTableIdAndStatusIn(id, openStatuses)
                .isPresent();

        if (hasOpenOrder) {
            throw new IllegalStateException("Không thể xóa bàn vì đang có đơn hàng mở");
        }

        restaurantTableRepository.delete(table);
    }

    /**
     * Lấy danh sách tất cả bàn.
     */
    @Transactional(readOnly = true)
    public List<TableResponse> getAllTables() {
        return restaurantTableRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lấy chi tiết 1 bàn theo id.
     */
    @Transactional(readOnly = true)
    public TableResponse getTableById(Long id) {
        RestaurantTable table = getTableOrThrow(id);
        return toResponse(table);
    }

    /**
     * Gộp 2 bàn:
     * - sourceTable: chuyển sang MERGED
     * - targetTable: giữ trạng thái OCCUPIED, giữ order chính
     * - Nếu sourceTable đang có order mở → chuyển order sang targetTable
     */
    @Transactional
    public void mergeTables(MergeTableRequest request) {
        if (request.getSourceTableId().equals(request.getTargetTableId())) {
            throw new IllegalArgumentException("Không thể gộp cùng một bàn");
        }

        RestaurantTable source = getTableOrThrow(request.getSourceTableId());
        RestaurantTable target = getTableOrThrow(request.getTargetTableId());

        // Chỉ cho phép gộp nếu source đang AVAILABLE hoặc OCCUPIED
        // (tùy nghiệp vụ, ở đây giới hạn cho đơn giản)
        if (TableStatus.MERGED.equals(source.getStatus())) {
            throw new IllegalStateException("Bàn nguồn đã ở trạng thái MERGED");
        }

        // Nếu source đang có order mở → chuyển qua target
        List<OrderStatus> openStatuses = Arrays.asList(OrderStatus.NEW, OrderStatus.SERVING);
        orderRepository.findFirstByTableIdAndStatusIn(source.getId(), openStatuses)
                .ifPresent(order -> {
                    order.setTable(target);
                    // Target chắc chắn sẽ OCCUPIED nếu có order
                    target.setStatus(TableStatus.OCCUPIED);
                });

        // Đánh dấu source là MERGED và trỏ về target
        source.setStatus(TableStatus.MERGED);
        source.setMergedRootId(target.getId());
    }

    /**
     * Tách bàn:
     * - Áp dụng cho bàn đang MERGED → trả về AVAILABLE
     * - mergedRootId = null
     */
    @Transactional
    public void splitTable(Long tableId) {
        RestaurantTable table = getTableOrThrow(tableId);

        if (!TableStatus.MERGED.equals(table.getStatus())) {
            throw new IllegalStateException("Chỉ có thể tách những bàn đang ở trạng thái MERGED");
        }

        table.setStatus(TableStatus.AVAILABLE);
        table.setMergedRootId(null);
    }

    /**
     * Chuyển bàn:
     * - Lấy order đang mở ở oldTable (NEW/SERVING)
     * - Gán sang newTable
     * - Cập nhật trạng thái 2 bàn: old → AVAILABLE, new → OCCUPIED
     */
    @Transactional
    public void changeTable(ChangeTableRequest request) {
        if (request.getOldTableId().equals(request.getNewTableId())) {
            throw new IllegalArgumentException("Bàn mới phải khác bàn hiện tại");
        }

        RestaurantTable oldTable = getTableOrThrow(request.getOldTableId());
        RestaurantTable newTable = getTableOrThrow(request.getNewTableId());

        if (!newTable.isAvailable()) {
            throw new IllegalStateException("Bàn mới không ở trạng thái AVAILABLE, không thể chuyển");
        }

        List<OrderStatus> openStatuses = Arrays.asList(OrderStatus.NEW, OrderStatus.SERVING);
        Order order = orderRepository.findFirstByTableIdAndStatusIn(oldTable.getId(), openStatuses)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy order đang mở ở bàn cũ"));

        // Chuyển order sang bàn mới
        order.setTable(newTable);

        // Cập nhật trạng thái bàn
        oldTable.setStatus(TableStatus.AVAILABLE);
        newTable.setStatus(TableStatus.OCCUPIED);
    }

    /**
     * Cập nhật trạng thái bàn (trường hợp muốn chỉnh tay).
     * Bình thường luồng này sẽ do OrderService auto xử lý.
     */
    @Transactional
    public void updateTableStatus(UpdateTableStatusRequest request) {
        RestaurantTable table = getTableOrThrow(request.getTableId());
        table.setStatus(request.getNewStatus());
    }

    /**
     * Hàm hỗ trợ: set bàn sang OCCUPIED khi tạo order.
     * Gọi từ OrderService.createOrder(...)
     */
    @Transactional
    public RestaurantTable markTableOccupied(Long tableId) {
        RestaurantTable table = getTableOrThrow(tableId);
        if (!table.isAvailable()) {
            throw new IllegalStateException("Bàn không ở trạng thái AVAILABLE, không thể mở order mới");
        }
        table.setStatus(TableStatus.OCCUPIED);

        // 🔥 BẮT BUỘC SAVE + FLUSH để socket có
        restaurantTableRepository.saveAndFlush(table);

        return table;
    }

    /**
     * Hàm hỗ trợ: set bàn sang AVAILABLE khi thanh toán order.
     * Gọi từ OrderService.payOrder(...)
     */
    @Transactional
    public void markTableAvailable(Long tableId) {
        RestaurantTable table = getTableOrThrow(tableId);
        table.setStatus(TableStatus.AVAILABLE);
    }

    // ================== HÀM HỖ TRỢ NỘI BỘ ==================

    private RestaurantTable getTableOrThrow(Long id) {
        return restaurantTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bàn với id = " + id));
    }

    private TableResponse toResponse(RestaurantTable table) {
        return TableResponse.builder()
                .id(table.getId())
                .name(table.getName())
                .capacity(table.getCapacity())
                .status(table.getStatus())
                .mergedRootId(table.getMergedRootId())
                .createdAt(table.getCreatedAt())
                .updatedAt(table.getUpdatedAt())
                .build();
    }
    /**
     * Lấy danh sách bàn + thông tin ORDER hiện tại cho màn hình POS.
     * ------------------------------------------------------------
     * Quy ước:
     *  - Order đang mở = trạng thái NEW hoặc SERVING
     *  - Nếu không có order mở → trả về chỉ thông tin bàn
     *  - Nếu có → kèm theo thống kê món:
     *      + totalItems, newItems, cookingItems, doneItems
     *      + waitingForPayment = true nếu order.status = SERVING
     */
    @Transactional(readOnly = true)
    public List<PosTableStatusResponse> getPosTableStatuses() {

        // 1) Lấy toàn bộ bàn
        List<RestaurantTable> tables = restaurantTableRepository.findAll();

        // 2) Các trạng thái order được xem là "đang mở"
        List<OrderStatus> openStatuses = Arrays.asList(OrderStatus.NEW, OrderStatus.SERVING);

        // 3) Map từng bàn → PosTableStatusResponse
        return tables.stream()
                .map(table -> {

                    // Tìm order đang mở của bàn (nếu có)
                    Order order = orderRepository
                            .findFirstByTableIdAndStatusIn(table.getId(), openStatuses)
                            .orElse(null);

                    // Nếu KHÔNG có order mở → trả về thông tin bàn đơn thuần
                    if (order == null) {
                        return PosTableStatusResponse.builder()
                                .tableId(table.getId())
                                .tableName(table.getName())
                                .status(table.getStatus().name())
                                .capacity(table.getCapacity())
                                .orderId(null)
                                .orderCode(null)
                                .orderCreatedAt(null)
                                .totalItems(0L)
                                .newItems(0L)
                                .cookingItems(0L)
                                .doneItems(0L)
                                .waitingForPayment(false)
                                .build();
                    }

                    // Nếu CÓ order → lấy list item để thống kê
                    // Nếu CẦN loại bỏ món đã hủy khỏi thống kê:
                    List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId())
                            .stream()
                            .filter(i -> i.getStatus() != OrderItemStatus.CANCELED)
                            .toList();

                    long total = items.size();

                    long newCount = items.stream()
                            .filter(i -> i.getStatus() == OrderItemStatus.NEW)
                            .count();

                    long cookingCount = items.stream()
                            .filter(i -> i.getStatus() == OrderItemStatus.SENT_TO_KITCHEN
                                    || i.getStatus() == OrderItemStatus.COOKING)
                            .count();

                    long doneCount = items.stream()
                            .filter(i -> i.getStatus() == OrderItemStatus.DONE)
                            .count();


                    return PosTableStatusResponse.builder()
                            .tableId(table.getId())
                            .tableName(table.getName())
                            .status(table.getStatus().name())
                            .capacity(table.getCapacity())

                            .orderId(order.getId())
                            .orderCode(order.getOrderCode())
                            .orderCreatedAt(order.getCreatedAt())

                            .totalItems(total)
                            .newItems(newCount)
                            .cookingItems(cookingCount)
                            .doneItems(doneCount)

                            .waitingForPayment(order.getStatus() == OrderStatus.SERVING)
                            .build();
                })
                .toList();
    }
}
