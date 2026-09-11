package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inventory_item")
public class InventoryItem {

    /** 在场 */
    public static final int RESULT_PRESENT = 1;
    /** 缺失 */
    public static final int RESULT_MISSING = 2;
    /** 位置不符 */
    public static final int RESULT_MISMATCH = 3;
    /** 待维修 */
    public static final int RESULT_REPAIR = 4;

    /** 待处理 */
    public static final int PROCESS_PENDING = 1;
    /** 已处理 */
    public static final int PROCESS_DONE = 2;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("batch_id")
    private Long batchId;

    @TableField("device_id")
    private Long deviceId;

    @TableField("device_code")
    private String deviceCode;

    @TableField("device_name")
    private String deviceName;

    @TableField("device_type")
    private String deviceType;

    @TableField("snapshot_floor_id")
    private Long snapshotFloorId;

    @TableField("snapshot_room_id")
    private Long snapshotRoomId;

    @TableField("check_result")
    private Integer checkResult;

    @TableField("remark")
    private String remark;

    @TableField("ledger_floor_id")
    private Long ledgerFloorId;

    @TableField("ledger_room_id")
    private Long ledgerRoomId;

    @TableField("location_mismatch")
    private Integer locationMismatch;

    @TableField("process_status")
    private Integer processStatus;

    @TableField("process_remark")
    private String processRemark;

    @TableField("processed_at")
    private LocalDateTime processedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
