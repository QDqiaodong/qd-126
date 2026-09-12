package com.example.devicemanagement.dto.request;

import lombok.Data;

@Data
public class DeviceReplacementRequest {

    /**
     * 故障设备ID（接待中发生故障、被卸下的设备）
     */
    private Long faultyDeviceId;

    /**
     * 同楼层备用设备ID（立刻顶上的设备）
     */
    private Long spareDeviceId;

    /**
     * 故障现象
     */
    private String faultPhenomenon;

    /**
     * 替换操作人（值班员）
     */
    private String operator;

    /**
     * 备注
     */
    private String remark;
}
