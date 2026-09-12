package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应急灯检查单（到期补检）。
 * check_result 为 NULL 表示未关闭；同一盏灯同时只允许一张未关闭检查单，
 * 业务层先拦截，数据库 uk_open_light 生成列唯一索引兜底。
 */
@Data
@TableName("emergency_light_inspection")
public class EmergencyLightInspection {

    /** 检查结果：合格 */
    public static final int RESULT_PASS = 1;
    /** 检查结果：不合格 */
    public static final int RESULT_FAIL = 2;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("inspection_no")
    private String inspectionNo;

    @TableField("light_id")
    private Long lightId;

    @TableField("light_code")
    private String lightCode;

    @TableField("light_name")
    private String lightName;

    @TableField("room_id")
    private Long roomId;

    @TableField("floor_id")
    private Long floorId;

    @TableField("inspector")
    private String inspector;

    @TableField("inspect_date")
    private LocalDate inspectDate;

    /** 1合格 2不合格，NULL=检查单未关闭 */
    @TableField("check_result")
    private Integer checkResult;

    @TableField("result_remark")
    private String resultRemark;

    @TableField("closed_by")
    private String closedBy;

    @TableField("closed_at")
    private LocalDateTime closedAt;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
