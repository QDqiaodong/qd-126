package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接待室欢迎牌排期：行政登记文案、上墙时间、撤下时间。
 * 状态只记录排期生命周期（待上墙/已上墙/已撤下），
 * 「待撤」是到期未撤（now &gt; plannedRemoveTime 且仍在墙上）的派生标记，不入库，
 * 因此关掉服务再打开后待撤标记、排期状态与可接待情况仍能对得上。
 */
@Data
@TableName("welcome_board")
public class WelcomeBoard {

    /** 待上墙（已排期，未到上墙时间） */
    public static final int STATUS_SCHEDULED = 0;
    /** 已上墙（到上墙时间后懒推进，撤下回执前一直占着接待室） */
    public static final int STATUS_MOUNTED = 1;
    /** 已撤下（必须登记撤下回执才能进入此状态） */
    public static final int STATUS_REMOVED = 2;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("board_no")
    private String boardNo;

    /** 欢迎牌文案（如“热烈欢迎XX考察团莅临指导”） */
    @TableField("board_text")
    private String boardText;

    @TableField("room_id")
    private Long roomId;

    @TableField("floor_id")
    private Long floorId;

    /** 计划上墙时间 */
    @TableField("mount_time")
    private LocalDateTime mountTime;

    /** 计划撤下时间（到期未撤即标待撤） */
    @TableField("planned_remove_time")
    private LocalDateTime plannedRemoveTime;

    /** 登记人（行政） */
    @TableField("registrar")
    private String registrar;

    @TableField("status")
    private Integer status;

    /** 实际上墙时间（到点懒推进时按计划上墙时间写入） */
    @TableField("mounted_at")
    private LocalDateTime mountedAt;

    /** 实际撤下时间（写撤下回执时写入） */
    @TableField("removed_at")
    private LocalDateTime removedAt;

    /** 撤下回执内容（撤下必填） */
    @TableField("remove_receipt")
    private String removeReceipt;

    /** 撤下回执登记人 */
    @TableField("removed_by")
    private String removedBy;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
