package com.restaurant.api.dto.table;

import lombok.Getter;
import lombok.Setter;

/**
 * TableRequest – Dùng cho tạo/sửa bàn.
 */
@Getter
@Setter
public class TableRequest {

    private String name;     // Tên bàn
    private Integer capacity; // Số khách tối đa
}
