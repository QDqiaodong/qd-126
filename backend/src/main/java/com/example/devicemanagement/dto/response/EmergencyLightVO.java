package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应急灯台账行：待检标记（pending）与是否存在未关闭检查单（openInspection）
 * 全部按库内日期和检查单实时算出，不存内存状态，重启后保持一致。
 */
@Data
public class EmergencyLightVO {

    private Long id;
    private String lightCode;
    private String lightName;

    private Long roomId;
    private String roomName;
    private String roomCode;
    private Long floorId;
    private String floorName;

    private LocalDate lastCheckDate;
    private LocalDate nextDueDate;
    private Integer intervalDays;
    private Integer status;

    /** 是否到期未检（下次到期日 <= 今天且未被合格补检覆盖） */
    private Boolean pending;

    /** 是否挂着未关闭检查单 */
    private Boolean openInspection;

    /** 未关闭检查单ID（有则返回） */
    private Long openInspectionId;
    private String openInspectionNo;

    private String createdBy;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
