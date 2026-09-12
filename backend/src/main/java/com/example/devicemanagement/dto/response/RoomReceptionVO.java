package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接待室来访接待状态：只有值班员确认到场（接待中）的来访才把接待室标为正在接待。
 */
@Data
public class RoomReceptionVO {

    private Long roomId;
    private String roomName;
    private Long floorId;
    private String floorName;

    /** 接待室是否启用 */
    private Integer roomStatus;

    /** 是否正在接待（存在接待中的来访） */
    private Boolean receiving;

    /** 当前是否可接待：启用且无接待中来访 */
    private Boolean available;

    /** 正在接待的来访单（接待中时返回） */
    private Long visitId;
    private String visitNo;
    private String visitorOrg;
    private Integer visitorCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 最近一条未到访的预约（可接待提示用），无则为空 */
    private Long upcomingVisitId;
    private String upcomingVisitorOrg;
    private LocalDateTime upcomingStartTime;
}
