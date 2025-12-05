package com.restaurant.api.dto.table;

import com.restaurant.api.enums.TableStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * UpdateTableStatusRequest – Request cập nhật trạng thái bàn.
 * (Dùng trong trường hợp cần chỉnh tay, nhưng bình thường sẽ auto theo Order)
 */
@Getter
@Setter
public class UpdateTableStatusRequest {

    private Long tableId;
    private TableStatus newStatus;
}
