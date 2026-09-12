package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_combo_record")
public class DeviceComboRecord {

    /** 套用结果：调入 */
    public static final int RESULT_APPLIED = 1;
    /** 套用结果：套用前已在目标接待室 */
    public static final int RESULT_ALREADY_PRESENT = 2;
    /** 套用结果：跳过 */
    public static final int RESULT_SKIPPED = 3;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("record_no")
    private String recordNo;

    @TableField("combo_id")
    private Long comboId;

    @TableField("combo_name")
    private String comboName;

    @TableField("room_id")
    private Long roomId;

    @TableField("floor_id")
    private Long floorId;

    @TableField("operator")
    private String operator;

    @TableField("apply_time")
    private LocalDateTime applyTime;

    @TableField("required_count")
    private Integer requiredCount;

    @TableField("applied_count")
    private Integer appliedCount;

    @TableField("present_count")
    private Integer presentCount;

    @TableField("skipped_count")
    private Integer skippedCount;

    @TableField("before_snapshot")
    private String beforeSnapshot;

    @TableField("after_snapshot")
    private String afterSnapshot;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
