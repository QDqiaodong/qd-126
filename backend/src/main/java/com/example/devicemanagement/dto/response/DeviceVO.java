package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class DeviceVO {

    private Long id;
    private String deviceCode;
    private String deviceName;
    private String deviceType;
    private String brand;
    private String model;
    private Map<String, Object> specJson;
    private String imageUrl;
    private Long currentFloorId;
    private String currentFloorName;
    private Long currentRoomId;
    private String currentRoomName;
    private Integer status;
    private String statusText;
    private LocalDate purchaseDate;
    private LocalDate warrantyEndDate;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
