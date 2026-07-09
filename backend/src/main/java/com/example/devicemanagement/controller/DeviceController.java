package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.BatchTransferRequest;
import com.example.devicemanagement.dto.request.DeviceCreateRequest;
import com.example.devicemanagement.dto.request.DeviceTransferRequest;
import com.example.devicemanagement.dto.request.DeviceUpdateRequest;
import com.example.devicemanagement.dto.response.ApiResponse;
import com.example.devicemanagement.dto.response.DeviceVO;
import com.example.devicemanagement.dto.response.PageResponse;
import com.example.devicemanagement.dto.response.TransferRecordVO;
import com.example.devicemanagement.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/device")
@CrossOrigin(origins = "*")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @PostMapping
    public ApiResponse<DeviceVO> createDevice(@RequestBody DeviceCreateRequest request) {
        DeviceVO device = deviceService.createDevice(request);
        return ApiResponse.success(device);
    }

    @PutMapping("/{id}")
    public ApiResponse<DeviceVO> updateDevice(@PathVariable Long id, @RequestBody DeviceUpdateRequest request) {
        DeviceVO device = deviceService.updateDevice(id, request);
        return ApiResponse.success(device);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceVO> getDeviceById(@PathVariable Long id) {
        DeviceVO device = deviceService.getDeviceById(id);
        return ApiResponse.success(device);
    }

    @GetMapping("/code/{deviceCode}")
    public ApiResponse<DeviceVO> getDeviceByCode(@PathVariable String deviceCode) {
        DeviceVO device = deviceService.getDeviceByCode(deviceCode);
        return ApiResponse.success(device);
    }

    @GetMapping("/floor/{floorId}")
    public ApiResponse<List<DeviceVO>> getDevicesByFloor(@PathVariable Long floorId) {
        List<DeviceVO> devices = deviceService.getDevicesByFloor(floorId);
        return ApiResponse.success(devices);
    }

    @GetMapping("/room/{roomId}")
    public ApiResponse<List<DeviceVO>> getDevicesByRoom(@PathVariable Long roomId) {
        List<DeviceVO> devices = deviceService.getDevicesByRoom(roomId);
        return ApiResponse.success(devices);
    }

    @GetMapping
    public ApiResponse<List<DeviceVO>> getAllDevices() {
        List<DeviceVO> devices = deviceService.getAllDevices();
        return ApiResponse.success(devices);
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<DeviceVO>> getDevicesPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String deviceName) {
        IPage<DeviceVO> page = deviceService.getDevicesPage(pageNum, pageSize, deviceType, deviceName);
        PageResponse<DeviceVO> response = PageResponse.of(
                page.getRecords(),
                page.getTotal(),
                pageNum,
                pageSize
        );
        return ApiResponse.success(response);
    }

    @GetMapping("/grouped")
    public ApiResponse<Map<String, List<DeviceVO>>> getDevicesGroupedByFloor() {
        Map<String, List<DeviceVO>> devices = deviceService.getDevicesGroupedByFloor();
        return ApiResponse.success(devices);
    }

    @PostMapping("/transfer")
    public ApiResponse<DeviceVO> transferDevice(@RequestBody DeviceTransferRequest request) {
        DeviceVO device = deviceService.transferDevice(request);
        return ApiResponse.success("流转成功", device);
    }

    @PostMapping("/batch-transfer")
    public ApiResponse<List<DeviceVO>> batchTransferDevices(@RequestBody BatchTransferRequest request) {
        List<DeviceVO> devices = deviceService.batchTransferDevices(request);
        return ApiResponse.success("批量流转成功", devices);
    }

    @GetMapping("/transfer-history/{deviceId}")
    public ApiResponse<List<TransferRecordVO>> getTransferHistory(@PathVariable Long deviceId) {
        List<TransferRecordVO> records = deviceService.getTransferHistory(deviceId);
        return ApiResponse.success(records);
    }

    @GetMapping("/transfer-records")
    public ApiResponse<List<TransferRecordVO>> getAllTransferRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        List<TransferRecordVO> records = deviceService.getAllTransferRecords(pageNum, pageSize);
        return ApiResponse.success(records);
    }
}
