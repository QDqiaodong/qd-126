package com.example.devicemanagement.dto.request;

import lombok.Data;

@Data
public class DeviceTransferRequest {

    private Long deviceId;
    private Long toFloorId;
    private Long toRoomId;
    private String transferReason;
    private String operator;
    private String remark;
}
