package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room_activity")
public class RoomActivity {

    /** 待开始 */
    public static final int STATUS_PENDING = 0;
    /** 进行中 */
    public static final int STATUS_ONGOING = 1;
    /** 已结束（占用已释放） */
    public static final int STATUS_ENDED = 2;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("activity_no")
    private String activityNo;

    @TableField("activity_name")
    private String activityName;

    @TableField("room_id")
    private Long roomId;

    @TableField("floor_id")
    private Long floorId;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("manager")
    private String manager;

    @TableField("status")
    private Integer status;

    @TableField("released_at")
    private LocalDateTime releasedAt;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
