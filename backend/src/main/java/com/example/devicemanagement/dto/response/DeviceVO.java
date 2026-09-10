package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    /**
     * 当前设备类型对应的规格字段定义（含停用模板，用于详情页按模板渲染）
     */
    private List<SpecFieldVO> specFields;
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
