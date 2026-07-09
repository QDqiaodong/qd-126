package com.example.devicemanagement.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class DeviceUpdateRequest {

    private String deviceName;
    private String deviceType;
    private String brand;
    private String model;
    private Map<String, Object> specJson;
    private String imageUrl;
    private Long currentFloorId;
    private Long currentRoomId;
    private Integer status;
    private LocalDate purchaseDate;
    private LocalDate warrantyEndDate;
}
