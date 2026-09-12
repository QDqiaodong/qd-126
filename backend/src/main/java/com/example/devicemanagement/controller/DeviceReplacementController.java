package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.DeviceReplacementRequest;
import com.example.devicemanagement.dto.request.ReplacementResultRequest;
import com.example.devicemanagement.dto.response.ApiResponse;
import com.example.devicemanagement.dto.response.DeviceReplacementVO;
import com.example.devicemanagement.dto.response.PageResponse;
import com.example.devicemanagement.dto.response.SpareDeviceVO;
import com.example.devicemanagement.service.DeviceReplacementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/replacement")
@CrossOrigin(origins = "*")
public class DeviceReplacementController {

    @Autowired
    private DeviceReplacementService replacementService;

    /**
     * 接待中故障应急替换：同楼层备用机顶替，原设备转待维修。
     */
    @PostMapping
    public ApiResponse<DeviceReplacementVO> create(@RequestBody DeviceReplacementRequest request) {
        return ApiResponse.success("替换成功", replacementService.createReplacement(request));
    }

    /**
     * 可选备用机（故障设备同楼层、状态正常、未分配接待室）。
     */
    @GetMapping("/spares")
    public ApiResponse<List<SpareDeviceVO>> getSpares(@RequestParam Long faultyDeviceId) {
        return ApiResponse.success(replacementService.getSpares(faultyDeviceId));
    }

    /**
     * 替换记录分页，可按楼层、接待室、处理结果过滤。
     */
    @GetMapping
    public ApiResponse<PageResponse<DeviceReplacementVO>> getPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Integer processResult) {
        IPage<DeviceReplacementVO> page = replacementService.getReplacementsPage(
                pageNum, pageSize, floorId, roomId, processResult);
        return ApiResponse.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceReplacementVO> getById(@PathVariable Long id) {
        return ApiResponse.success(replacementService.getReplacementById(id));
    }

    /**
     * 登记故障设备维修/报废处理结果。
     */
    @PostMapping("/{id}/resolve")
    public ApiResponse<DeviceReplacementVO> resolve(@PathVariable Long id,
                                                    @RequestBody ReplacementResultRequest request) {
        return ApiResponse.success("处理结果已登记", replacementService.resolveResult(id, request));
    }
}
