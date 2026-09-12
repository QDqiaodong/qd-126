package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitVO {

    private Long id;
    private String visitNo;

    private String visitorOrg;
    private Integer visitorCount;

    private Long roomId;
    private String roomName;
    private String roomCode;
    private Long floorId;
    private String floorName;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 状态 0待到访 1接待中 2已结束 3已取消 */
    private Integer status;
    private String statusText;

    private LocalDateTime checkedInAt;
    private LocalDateTime cancelledAt;
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
