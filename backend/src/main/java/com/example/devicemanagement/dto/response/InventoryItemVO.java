package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryItemVO {

    private Long id;
    private Long batchId;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String deviceType;

    /** 快照归属 */
    private Long snapshotFloorId;
    private String snapshotFloorName;
    private Long snapshotRoomId;
    private String snapshotRoomName;

    /** 盘点结果 1在场 2缺失 3位置不符 4待维修，null 未盘点 */
    private Integer checkResult;
    private String checkResultText;
    private String remark;

    /**
     * 提交时冻结的台账归属，作为差异基准；盘点中为空
     */
    private Long ledgerFloorId;
    private String ledgerFloorName;
    private Long ledgerRoomId;
    private String ledgerRoomName;

    /**
     * 盘点中：快照与实时台账的位置是否不一致（现场参考，刷新实时计算）；
     * 提交/关闭：以提交时冻结的差异基准为准，保持稳定。
     */
    private Boolean locationMismatch;

    /** 处理状态 1待处理 2已处理 */
    private Integer processStatus;
    private String processStatusText;
    private String processRemark;
    private LocalDateTime processedAt;

    private LocalDateTime updatedAt;
}
