package com.example.devicemanagement.dto.request;

import lombok.Data;

@Data
public class ReceptionRoomRequest {

    private String roomName;
    private String roomCode;
    private Long floorId;
    private Integer capacity;
    private Integer equipmentCount;
    private Integer status;
}
