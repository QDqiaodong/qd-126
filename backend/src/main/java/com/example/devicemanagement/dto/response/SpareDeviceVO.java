package com.example.devicemanagement.dto.response;

import lombok.Data;

/**
 * 可用于应急替换的同楼层备用设备（未分配接待室且状态正常）。
 */
@Data
public class SpareDeviceVO {

    private Long id;
    private String deviceCode;
    private String deviceName;
    private String deviceType;
    private String brand;
    private String model;
    private Long currentFloorId;
    private String currentFloorName;
    private Integer status;
    private String statusText;
    /**
     * 是否与故障设备同型号（前端可优先推荐）
     */
    private Boolean sameType;
    /**
     * 是否与故障设备同设备类型
     */
    private Boolean sameDeviceType;
}
