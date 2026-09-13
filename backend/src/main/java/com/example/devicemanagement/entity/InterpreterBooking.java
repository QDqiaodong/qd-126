package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接待室随行翻译预约：按接待室预约翻译语种、随行译员与服务时段。
 * 同一译员同一时段只能服务一间接待室，撞车时拦截并带出已约接待室；
 * 活动已开始（进行中/已结束）后只允许查看，不再修改译员与时段。
 * 没约翻译的接待室不影响其正常活动占用。
 */
@Data
@TableName("interpreter_booking")
public class InterpreterBooking {

    /** 待开始 */
    public static final int STATUS_PENDING = 0;
    /** 进行中（活动已开始，只允许查看） */
    public static final int STATUS_ONGOING = 1;
    /** 已结束 */
    public static final int STATUS_ENDED = 2;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("booking_no")
    private String bookingNo;

    @TableField("room_id")
    private Long roomId;

    /** 冗余接待室所在楼层，便于按楼层筛选 */
    @TableField("floor_id")
    private Long floorId;

    /** 翻译语种 */
    @TableField("language")
    private String language;

    /** 随行译员姓名（同名视为同一人，按姓名做时段撞车校验） */
    @TableField("interpreter_name")
    private String interpreterName;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("status")
    private Integer status;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
