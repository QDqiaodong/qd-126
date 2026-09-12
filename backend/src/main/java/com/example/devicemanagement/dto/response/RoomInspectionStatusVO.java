package com.example.devicemanagement.dto.response;

import lombok.Data;

/**
 * 接待室应急灯检查状态：
 * 接待室只要有一盏到期未检的应急灯就标待检（pending），同时不可接待。
 * 待检标记、检查单状态、可接待情况三处同源计算，关掉再打开后对得上。
 */
@Data
public class RoomInspectionStatusVO {

    private Long roomId;
    private String roomName;
    private String roomCode;
    private Long floorId;
    private String floorName;

    /** 接待室是否启用 */
    private Integer roomStatus;

    /** 应急灯总数（启用） */
    private Integer lightCount;

    /** 到期未检盏数 */
    private Integer pendingCount;

    /** 未关闭检查单数 */
    private Integer openInspectionCount;

    /** 是否待检：存在到期未检的应急灯 */
    private Boolean pending;

    /** 是否可接待：接待室启用且无待检应急灯 */
    private Boolean receivable;
}
