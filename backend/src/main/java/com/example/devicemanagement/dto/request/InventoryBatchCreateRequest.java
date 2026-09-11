package com.example.devicemanagement.dto.request;

import lombok.Data;

@Data
public class InventoryBatchCreateRequest {

    /** 批次名称，可空：空时按范围名称自动生成 */
    private String batchName;

    /** 盘点范围类型 FLOOR / ROOM */
    private String scopeType;

    /** scopeType=FLOOR 时必填 */
    private Long floorId;

    /** scopeType=ROOM 时必填 */
    private Long roomId;

    /** 盘点负责人 */
    private String operator;

    private String remark;
}
