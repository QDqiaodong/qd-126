package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomQuietPeriodVO {

    private Long id;
    private String quietNo;

    private Long roomId;
    private String roomName;
    private String roomCode;
    private Long floorId;
    private String floorName;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
