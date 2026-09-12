package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceReplacementVO {

    private Long id;
    private String replacementNo;

    private Long roomId;
    private String roomName;
    private String roomCode;
    private Long floorId;
    private String floorName;

    // 卸下的故障设备（快照字段 + 实时状态，便于对照）
    private Long faultyDeviceId;
    private String faultyDeviceCode;
    private String faultyDeviceName;
    private String faultyDeviceType;
    private Integer faultyDeviceStatus;
    private String faultyDeviceStatusText;
    private Long faultyDeviceRoomId;
    private String faultyDeviceRoomName;

    // 换上的备用设备
    private Long spareDeviceId;
    private String spareDeviceCode;
    private String spareDeviceName;
    private String spareDeviceType;
    private Integer spareDeviceStatus;
    private String spareDeviceStatusText;
    private Long spareDeviceRoomId;
    private String spareDeviceRoomName;

    private String faultPhenomenon;
    private String operator;
    private LocalDateTime replacementTime;

    private Integer processResult;
    private String processResultText;
    private String processRemark;
    private String processedBy;
    private LocalDateTime processedAt;
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
