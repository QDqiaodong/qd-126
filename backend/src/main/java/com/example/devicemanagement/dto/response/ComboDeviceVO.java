package com.example.devicemanagement.dto.response;

import lombok.Data;

/**
 * 套用前后接待室设备清单中的单台设备快照。
 */
@Data
public class ComboDeviceVO {

    private Long id;

    private String deviceCode;

    private String deviceName;

    private String deviceType;

    private String brand;

    private String model;

    private Integer status;

    private String statusText;
}
