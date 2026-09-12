package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.DeviceReplacementRequest;
import com.example.devicemanagement.dto.request.ReplacementResultRequest;
import com.example.devicemanagement.dto.response.DeviceReplacementVO;
import com.example.devicemanagement.dto.response.SpareDeviceVO;

import java.util.List;

public interface DeviceReplacementService {

    /**
     * 接待中设备故障：同楼层备用机立刻顶上，原设备转待维修并卸下。
     */
    DeviceReplacementVO createReplacement(DeviceReplacementRequest request);

    /**
     * 查询故障设备所在楼层可立刻顶上的备用机（状态正常且未分配接待室）。
     */
    List<SpareDeviceVO> getSpares(Long faultyDeviceId);

    /**
     * 替换记录分页，可按楼层、接待室、处理结果过滤。
     */
    IPage<DeviceReplacementVO> getReplacementsPage(int pageNum, int pageSize,
                                                    Long floorId, Long roomId, Integer processResult);

    /**
     * 替换记录详情：对照换上/卸下设备、故障现象与处理结果。
     */
    DeviceReplacementVO getReplacementById(Long id);

    /**
     * 登记故障设备后续处理结果（已修复转楼层备用 / 已报废），同步设备状态。
     */
    DeviceReplacementVO resolveResult(Long id, ReplacementResultRequest request);
}
