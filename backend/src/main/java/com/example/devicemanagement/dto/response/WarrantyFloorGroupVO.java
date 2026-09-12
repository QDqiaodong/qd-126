package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class WarrantyFloorGroupVO {

    /**
     * 楼层ID，未分配楼层的分组为 null
     */
    private Long floorId;
    private String floorName;
    private List<WarrantyDeviceVO> devices;
}
