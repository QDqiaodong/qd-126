package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class WarrantyDeviceVO {

    private Long id;
    private String deviceCode;
    private String deviceName;
    private String deviceType;
    private String brand;
    private String model;
    private Long currentFloorId;
    private String currentFloorName;
    private Long currentRoomId;
    private String currentRoomName;
    private LocalDate warrantyEndDate;
    /**
     * 距保修截止的天数，负数表示已过期；未设置截止日期时为 null
     */
    private Long daysRemaining;
    /**
     * EXPIRED 已过期 / EXPIRING 临期（不足15天） / NORMAL 正常 / NONE 未设置截止日期
     */
    private String warrantyStatus;
    private String warrantyStatusText;
}
