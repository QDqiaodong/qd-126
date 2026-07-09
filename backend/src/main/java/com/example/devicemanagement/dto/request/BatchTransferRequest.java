package com.example.devicemanagement.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class BatchTransferRequest {

    private List<Long> deviceIds;
    private Long toFloorId;
    private Long toRoomId;
    private String transferReason;
    private String operator;
    private String remark;
}
