package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransferRecordVO {

    private Long id;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private Long fromFloorId;
    private String fromFloorName;
    private Long fromRoomId;
    private String fromRoomName;
    private Long toFloorId;
    private String toFloorName;
    private Long toRoomId;
    private String toRoomName;
    private String transferReason;
    private String operator;
    private LocalDateTime transferTime;
    private String remark;
}
