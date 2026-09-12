package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WelcomeBoardVO {

    private Long id;
    private String boardNo;
    private String boardText;

    private Long roomId;
    private String roomName;
    private String roomCode;
    private Long floorId;
    private String floorName;

    /** 计划上墙时间 */
    private LocalDateTime mountTime;
    /** 计划撤下时间 */
    private LocalDateTime plannedRemoveTime;
    private String registrar;

    /** 排期状态 0待上墙 1已上墙 2已撤下 */
    private Integer status;
    private String statusText;

    /**
     * 待撤标记：到期未撤（当前时间已过计划撤下时间但仍在墙上）。
     * 派生标记，不入库；重启后按当前时间重新计算，与排期状态保持一致。
     */
    private Boolean overdueRemove;

    /** 实际上墙时间 */
    private LocalDateTime mountedAt;
    /** 实际撤下时间 */
    private LocalDateTime removedAt;
    /** 撤下回执 */
    private String removeReceipt;
    private String removedBy;

    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
