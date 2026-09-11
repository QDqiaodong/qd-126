package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryBatchVO {

    private Long id;
    private String batchNo;
    private String batchName;
    /** 盘点范围类型 FLOOR / ROOM */
    private String scopeType;
    private String scopeTypeText;
    private Long floorId;
    private String floorName;
    private Long roomId;
    private String roomName;
    private String operator;
    /** 状态 0盘点中 1已提交 2已关闭 */
    private Integer status;
    private String statusText;
    private LocalDateTime snapshotTime;
    private LocalDateTime submittedAt;
    private LocalDateTime closedAt;
    private Integer totalCount;
    private Integer presentCount;
    private Integer missingCount;
    private Integer mismatchCount;
    private Integer repairCount;
    private Integer checkedCount;
    /** 盘点进度百分比 0-100 */
    private Integer progressPercent;
    /** 提交后尚未处理的差异数（缺失/位置不符，待处理） */
    private Integer pendingDiffCount;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
