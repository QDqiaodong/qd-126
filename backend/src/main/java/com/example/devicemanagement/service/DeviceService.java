package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.BatchTransferRequest;
import com.example.devicemanagement.dto.request.DeviceCreateRequest;
import com.example.devicemanagement.dto.request.DeviceTransferRequest;
import com.example.devicemanagement.dto.request.DeviceUpdateRequest;
import com.example.devicemanagement.dto.response.DeviceVO;
import com.example.devicemanagement.dto.response.TransferRecordVO;
import com.example.devicemanagement.dto.response.WarrantyOverviewVO;
import com.example.devicemanagement.entity.Device;

import java.util.List;
import java.util.Map;

public interface DeviceService {

    DeviceVO createDevice(DeviceCreateRequest request);

    DeviceVO updateDevice(Long id, DeviceUpdateRequest request);

    void deleteDevice(Long id);

    DeviceVO getDeviceById(Long id);

    DeviceVO getDeviceByCode(String deviceCode);

    List<DeviceVO> getDevicesByFloor(Long floorId);

    List<DeviceVO> getDevicesByRoom(Long roomId);

    List<DeviceVO> getAllDevices();

    IPage<DeviceVO> getDevicesPage(int pageNum, int pageSize, String deviceType, String deviceName);

    Map<String, List<DeviceVO>> getDevicesGroupedByFloor();

    WarrantyOverviewVO getWarrantyOverview(Long floorId, Long roomId);

    List<DeviceVO> getDevicesInRoom(Long roomId);

    DeviceVO transferDevice(DeviceTransferRequest request);

    List<DeviceVO> batchTransferDevices(BatchTransferRequest request);

    List<TransferRecordVO> getTransferHistory(Long deviceId);

    IPage<TransferRecordVO> getAllTransferRecords(int pageNum, int pageSize, String deviceName, String operator);
}
