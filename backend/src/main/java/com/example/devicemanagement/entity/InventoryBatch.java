package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inventory_batch")
public class InventoryBatch {

    /** 盘点中 */
    public static final int STATUS_COUNTING = 0;
    /** 已提交 */
    public static final int STATUS_SUBMITTED = 1;
    /** 已关闭 */
    public static final int STATUS_CLOSED = 2;

    /** 按楼层盘点 */
    public static final String SCOPE_FLOOR = "FLOOR";
    /** 按接待室盘点 */
    public static final String SCOPE_ROOM = "ROOM";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("batch_no")
    private String batchNo;

    @TableField("batch_name")
    private String batchName;

    @TableField("scope_type")
    private String scopeType;

    @TableField("floor_id")
    private Long floorId;

    @TableField("room_id")
    private Long roomId;

    @TableField("operator")
    private String operator;

    @TableField("status")
    private Integer status;

    @TableField("snapshot_time")
    private LocalDateTime snapshotTime;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @TableField("closed_at")
    private LocalDateTime closedAt;

    @TableField("total_count")
    private Integer totalCount;

    @TableField("present_count")
    private Integer presentCount;

    @TableField("missing_count")
    private Integer missingCount;

    @TableField("mismatch_count")
    private Integer mismatchCount;

    @TableField("repair_count")
    private Integer repairCount;

    @TableField("checked_count")
    private Integer checkedCount;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
