package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 接待室应急灯：行政登记编号、所在接待室、上次检查日和下次到期日。
 * 待检标记不落库，按 next_due_date 与当前日期实时计算，重启后自然一致。
 */
@Data
@TableName("emergency_light")
public class EmergencyLight {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("light_code")
    private String lightCode;

    @TableField("light_name")
    private String lightName;

    @TableField("room_id")
    private Long roomId;

    @TableField("floor_id")
    private Long floorId;

    @TableField("last_check_date")
    private LocalDate lastCheckDate;

    @TableField("next_due_date")
    private LocalDate nextDueDate;

    @TableField("interval_days")
    private Integer intervalDays;

    @TableField("status")
    private Integer status;

    @TableField("created_by")
    private String createdBy;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
