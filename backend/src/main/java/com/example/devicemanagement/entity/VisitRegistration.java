package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visit_registration")
public class VisitRegistration {

    /** 待到访 */
    public static final int STATUS_PENDING = 0;
    /** 接待中（值班员已确认到场） */
    public static final int STATUS_RECEIVING = 1;
    /** 已结束（到达预计结束时间自动结束，接待标记移除） */
    public static final int STATUS_FINISHED = 2;
    /** 已取消（立即移除接待标记） */
    public static final int STATUS_CANCELLED = 3;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("visit_no")
    private String visitNo;

    @TableField("visitor_org")
    private String visitorOrg;

    @TableField("visitor_count")
    private Integer visitorCount;

    @TableField("room_id")
    private Long roomId;

    @TableField("floor_id")
    private Long floorId;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("status")
    private Integer status;

    @TableField("checked_in_at")
    private LocalDateTime checkedInAt;

    @TableField("cancelled_at")
    private LocalDateTime cancelledAt;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
