package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class WarrantyOverviewVO {

    /**
     * 临期阈值天数：距到期不足该天数标黄
     */
    private Integer expiringSoonDays;
    /**
     * 按楼层分组的设备（仅含已设置保修截止日期的设备）
     */
    private List<WarrantyFloorGroupVO> groups;
    /**
     * 未设置保修截止日期的设备，单独列出
     */
    private List<WarrantyDeviceVO> noWarrantyDevices;
}
