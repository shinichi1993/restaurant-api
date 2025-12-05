package com.restaurant.api.dto.invoice;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * InvoiceItemResponse
 * ------------------------------------------------------------
 * DTO trả ra FE cho từng dòng chi tiết trong hóa đơn.
 *
 * Mỗi invoice_item tương ứng với 1 món ăn trên hóa đơn:
 *  - dishId      : ID món ăn
 *  - dishName    : Tên món (snapshot tại thời điểm tạo hóa đơn)
 *  - dishPrice   : Giá món (snapshot)
 *  - quantity    : Số lượng món
 *  - subtotal    : Thành tiền = dishPrice × quantity
 *
 * DTO này KHÔNG chứa logic, chỉ là lớp vận chuyển dữ liệu.
 * ------------------------------------------------------------
 */
@Data
@Builder
public class InvoiceItemResponse {

    /**
     * ID món ăn (FK tới bảng dish, nhưng trên FE chỉ dùng để hiển thị/trace)
     */
    private Long dishId;

    /**
     * Tên món ăn tại thời điểm in hóa đơn.
     * Nếu sau này đổi tên món trong menu thì hóa đơn cũ vẫn giữ nguyên.
     */
    private String dishName;

    /**
     * Đơn giá của món tại thời điểm in hóa đơn.
     * Dùng BigDecimal để đảm bảo độ chính xác số học.
     */
    private BigDecimal dishPrice;

    /**
     * Số lượng món trong hóa đơn.
     */
    private Integer quantity;

    /**
     * Thành tiền cho dòng này = dishPrice × quantity.
     */
    private BigDecimal subtotal;
}
