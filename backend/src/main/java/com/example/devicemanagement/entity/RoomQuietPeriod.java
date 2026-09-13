package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接待室静音时段：行政按接待室登记静音起止时间与原因。
 * 新建/修改活动占用时，时段与静音重叠即拦截并带出原因；
 * 已开始的活动只提示，不回改历史。
 */
@Data
@TableName("room_quiet_period")
public class RoomQuietPeriod {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("quiet_no")
    private String quietNo;

    @TableField("room_id")
    private Long roomId;

    /** 冗余接待室所在楼层，便于按楼层筛选 */
    @TableField("floor_id")
    private Long floorId;

    /** 静音开始时间 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 静音结束时间 */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 静音原因（拦截活动时弹出展示） */
    @TableField("reason")
    private String reason;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
