package com.restaurant.api.dto.table;

import lombok.Getter;
import lombok.Setter;

/**
 * MergeTableRequest – Request gộp 2 bàn lại với nhau.
 * sourceTableId: bàn bị gộp (sẽ chuyển sang trạng thái MERGED)
 * targetTableId: bàn gốc (giữ order chính)
 */
@Getter
@Setter
public class MergeTableRequest {

    private Long sourceTableId;
    private Long targetTableId;
}
