package com.example.devicemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.BatchTransferRequest;
import com.example.devicemanagement.dto.request.DeviceCreateRequest;
import com.example.devicemanagement.dto.request.DeviceTransferRequest;
import com.example.devicemanagement.dto.request.DeviceUpdateRequest;
import com.example.devicemanagement.dto.response.DeviceVO;
import com.example.devicemanagement.dto.response.TransferRecordVO;
import com.example.devicemanagement.entity.Device;
import com.example.devicemanagement.entity.DeviceTransfer;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.mapper.DeviceMapper;
import com.example.devicemanagement.mapper.DeviceTransferMapper;
import com.example.devicemanagement.service.DeviceService;
import com.example.devicemanagement.service.DeviceSpecValidator;
import com.example.devicemanagement.service.FloorService;
import com.example.devicemanagement.service.ReceptionRoomService;
import com.example.devicemanagement.service.RedisCacheService;
import com.example.devicemanagement.service.SpecTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeviceServiceImpl implements DeviceService {

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceTransferMapper transferMapper;

    @Autowired
    private FloorService floorService;

    @Autowired
    private ReceptionRoomService roomService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeviceSpecValidator deviceSpecValidator;

    @Autowired
    private SpecTemplateService specTemplateService;

    @Override
    @Transactional
    public DeviceVO createDevice(DeviceCreateRequest request) {
        Device device = new Device();
        device.setDeviceCode(request.getDeviceCode());
        device.setDeviceName(request.getDeviceName());
        device.setDeviceType(request.getDeviceType());
        device.setBrand(request.getBrand());
        device.setModel(request.getModel());
        Map<String, Object> validatedSpec = deviceSpecValidator.validate(
                request.getDeviceType(), request.getSpecJson());
        try {
            device.setSpecJson(objectMapper.writeValueAsString(validatedSpec));
        } catch (JsonProcessingException e) {
            device.setSpecJson("{}");
        }
        device.setImageUrl(request.getImageUrl());
        device.setCurrentFloorId(request.getCurrentFloorId());
        device.setCurrentRoomId(request.getCurrentRoomId());
        device.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        device.setPurchaseDate(request.getPurchaseDate());
        device.setWarrantyEndDate(request.getWarrantyEndDate());
        device.setCreatedBy(request.getCreatedBy());
        deviceMapper.insert(device);

        if (request.getCurrentRoomId() != null) {
            updateRoomEquipmentCount(request.getCurrentRoomId(), 1);
        }

        return convertToVO(device);
    }

    @Override
    @Transactional
    public DeviceVO updateDevice(Long id, DeviceUpdateRequest request) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }

        Long oldRoomId = device.getCurrentRoomId();

        device.setDeviceName(request.getDeviceName());
        device.setDeviceType(request.getDeviceType());
        device.setBrand(request.getBrand());
        device.setModel(request.getModel());
        if (request.getSpecJson() != null) {
            Map<String, Object> validatedSpec = deviceSpecValidator.validate(
                    request.getDeviceType(), request.getSpecJson());
            try {
                device.setSpecJson(objectMapper.writeValueAsString(validatedSpec));
            } catch (JsonProcessingException e) {
                device.setSpecJson("{}");
            }
        }
        device.setImageUrl(request.getImageUrl());
        device.setCurrentFloorId(request.getCurrentFloorId());
        device.setCurrentRoomId(request.getCurrentRoomId());
        device.setStatus(request.getStatus());
        device.setPurchaseDate(request.getPurchaseDate());
        device.setWarrantyEndDate(request.getWarrantyEndDate());
        deviceMapper.updateById(device);

        if (oldRoomId != null && !oldRoomId.equals(request.getCurrentRoomId())) {
            updateRoomEquipmentCount(oldRoomId, -1);
        }
        if (request.getCurrentRoomId() != null && !request.getCurrentRoomId().equals(oldRoomId)) {
            updateRoomEquipmentCount(request.getCurrentRoomId(), 1);
        }

        return convertToVO(device);
    }

    @Override
    @Transactional
    public void deleteDevice(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }
        if (device.getCurrentRoomId() != null) {
            updateRoomEquipmentCount(device.getCurrentRoomId(), -1);
        }
        deviceMapper.deleteById(id);
    }

    @Override
    public DeviceVO getDeviceById(Long id) {
        Device device = deviceMapper.selectById(id);
        return device != null ? convertToVO(device) : null;
    }

    @Override
    public DeviceVO getDeviceByCode(String deviceCode) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getDeviceCode, deviceCode);
        Device device = deviceMapper.selectOne(wrapper);
        return device != null ? convertToVO(device) : null;
    }

    @Override
    public List<DeviceVO> getDevicesByFloor(Long floorId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getCurrentFloorId, floorId);
        List<Device> devices = deviceMapper.selectList(wrapper);
        return devices.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<DeviceVO> getDevicesByRoom(Long roomId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getCurrentRoomId, roomId);
        List<Device> devices = deviceMapper.selectList(wrapper);
        return devices.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<DeviceVO> getAllDevices() {
        List<Device> devices = deviceMapper.selectList(null);
        return devices.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public IPage<DeviceVO> getDevicesPage(int pageNum, int pageSize, String deviceType, String deviceName) {
        Page<Device> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (deviceType != null && !deviceType.isEmpty()) {
            wrapper.eq(Device::getDeviceType, deviceType);
        }
        if (deviceName != null && !deviceName.isEmpty()) {
            wrapper.like(Device::getDeviceName, deviceName);
        }
        wrapper.orderByDesc(Device::getCreatedAt);
        IPage<Device> devicePage = deviceMapper.selectPage(page, wrapper);
        return devicePage.convert(this::convertToVO);
    }

    @Override
    public Map<String, List<DeviceVO>> getDevicesGroupedByFloor() {
        List<Device> devices = deviceMapper.selectList(null);
        Map<Long, List<Device>> grouped = devices.stream()
                .filter(d -> d.getCurrentFloorId() != null)
                .collect(Collectors.groupingBy(Device::getCurrentFloorId));

        Map<String, List<DeviceVO>> result = new LinkedHashMap<>();
        List<Floor> floors = floorService.getAllFloors();

        for (Floor floor : floors) {
            List<Device> floorDevices = grouped.getOrDefault(floor.getId(), Collections.emptyList());
            List<DeviceVO> voList = floorDevices.stream().map(this::convertToVO).collect(Collectors.toList());
            result.put(floor.getFloorName(), voList);
        }
        return result;
    }

    @Override
    public List<DeviceVO> getDevicesInRoom(Long roomId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getCurrentRoomId, roomId);
        List<Device> devices = deviceMapper.selectList(wrapper);
        return devices.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DeviceVO transferDevice(DeviceTransferRequest request) {
        Device device = deviceMapper.selectById(request.getDeviceId());
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }

        Long oldFloorId = device.getCurrentFloorId();
        Long oldRoomId = device.getCurrentRoomId();

        DeviceTransfer transfer = new DeviceTransfer();
        transfer.setDeviceId(device.getId());
        transfer.setDeviceCode(device.getDeviceCode());
        transfer.setFromFloorId(oldFloorId);
        transfer.setFromRoomId(oldRoomId);
        transfer.setToFloorId(request.getToFloorId());
        transfer.setToRoomId(request.getToRoomId());
        transfer.setTransferReason(request.getTransferReason());
        transfer.setOperator(request.getOperator());
        transfer.setTransferTime(LocalDateTime.now());
        transfer.setRemark(request.getRemark());
        transferMapper.insert(transfer);

        device.setCurrentFloorId(request.getToFloorId());
        device.setCurrentRoomId(request.getToRoomId());
        deviceMapper.updateById(device);

        if (oldRoomId != null) {
            updateRoomEquipmentCount(oldRoomId, -1);
        }
        if (request.getToRoomId() != null) {
            updateRoomEquipmentCount(request.getToRoomId(), 1);
        }

        try {
            redisCacheService.invalidateRoomCache(oldFloorId);
            redisCacheService.invalidateRoomCache(request.getToFloorId());
        } catch (Exception e) {
        }

        return convertToVO(device);
    }

    @Override
    @Transactional
    public List<DeviceVO> batchTransferDevices(BatchTransferRequest request) {
        List<DeviceVO> result = new ArrayList<>();
        for (Long deviceId : request.getDeviceIds()) {
            DeviceTransferRequest transferRequest = new DeviceTransferRequest();
            transferRequest.setDeviceId(deviceId);
            transferRequest.setToFloorId(request.getToFloorId());
            transferRequest.setToRoomId(request.getToRoomId());
            transferRequest.setTransferReason(request.getTransferReason());
            transferRequest.setOperator(request.getOperator());
            transferRequest.setRemark(request.getRemark());
            DeviceVO vo = transferDevice(transferRequest);
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<TransferRecordVO> getTransferHistory(Long deviceId) {
        LambdaQueryWrapper<DeviceTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceTransfer::getDeviceId, deviceId);
        wrapper.orderByDesc(DeviceTransfer::getTransferTime);
        List<DeviceTransfer> records = transferMapper.selectList(wrapper);
        return records.stream().map(this::convertTransferToVO).collect(Collectors.toList());
    }

    @Override
    public List<TransferRecordVO> getAllTransferRecords(int pageNum, int pageSize) {
        Page<DeviceTransfer> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DeviceTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(DeviceTransfer::getTransferTime);
        IPage<DeviceTransfer> transferPage = transferMapper.selectPage(page, wrapper);
        return transferPage.getRecords().stream()
                .map(this::convertTransferToVO)
                .collect(Collectors.toList());
    }

    private DeviceVO convertToVO(Device device) {
        DeviceVO vo = new DeviceVO();
        vo.setId(device.getId());
        vo.setDeviceCode(device.getDeviceCode());
        vo.setDeviceName(device.getDeviceName());
        vo.setDeviceType(device.getDeviceType());
        vo.setBrand(device.getBrand());
        vo.setModel(device.getModel());

        if (device.getSpecJson() != null && !device.getSpecJson().isEmpty()) {
            try {
                vo.setSpecJson(objectMapper.readValue(device.getSpecJson(), new TypeReference<Map<String, Object>>() {}));
            } catch (JsonProcessingException e) {
                vo.setSpecJson(new HashMap<>());
            }
        } else {
            vo.setSpecJson(new HashMap<>());
        }

        // 含停用模板的字段定义，支撑历史设备详情按模板渲染
        vo.setSpecFields(specTemplateService.getAnyStatusFields(device.getDeviceType()));

        vo.setImageUrl(device.getImageUrl());
        vo.setCurrentFloorId(device.getCurrentFloorId());
        vo.setCurrentRoomId(device.getCurrentRoomId());
        vo.setStatus(device.getStatus());
        vo.setStatusText(getStatusText(device.getStatus()));
        vo.setPurchaseDate(device.getPurchaseDate());
        vo.setWarrantyEndDate(device.getWarrantyEndDate());
        vo.setCreatedBy(device.getCreatedBy());
        vo.setCreatedAt(device.getCreatedAt());
        vo.setUpdatedAt(device.getUpdatedAt());

        if (device.getCurrentFloorId() != null) {
            Floor floor = floorService.getFloorById(device.getCurrentFloorId());
            if (floor != null) {
                vo.setCurrentFloorName(floor.getFloorName());
            }
        }
        if (device.getCurrentRoomId() != null) {
            ReceptionRoom room = roomService.getRoomById(device.getCurrentRoomId());
            if (room != null) {
                vo.setCurrentRoomName(room.getRoomName());
            }
        }
        return vo;
    }

    private TransferRecordVO convertTransferToVO(DeviceTransfer transfer) {
        TransferRecordVO vo = new TransferRecordVO();
        vo.setId(transfer.getId());
        vo.setDeviceId(transfer.getDeviceId());
        vo.setDeviceCode(transfer.getDeviceCode());

        Device device = deviceMapper.selectById(transfer.getDeviceId());
        if (device != null) {
            vo.setDeviceName(device.getDeviceName());
        }

        vo.setFromFloorId(transfer.getFromFloorId());
        vo.setFromRoomId(transfer.getFromRoomId());
        vo.setToFloorId(transfer.getToFloorId());
        vo.setToRoomId(transfer.getToRoomId());
        vo.setTransferReason(transfer.getTransferReason());
        vo.setOperator(transfer.getOperator());
        vo.setTransferTime(transfer.getTransferTime());
        vo.setRemark(transfer.getRemark());

        if (transfer.getFromFloorId() != null) {
            Floor floor = floorService.getFloorById(transfer.getFromFloorId());
            if (floor != null) {
                vo.setFromFloorName(floor.getFloorName());
            }
        }
        if (transfer.getFromRoomId() != null) {
            ReceptionRoom room = roomService.getRoomById(transfer.getFromRoomId());
            if (room != null) {
                vo.setFromRoomName(room.getRoomName());
            }
        }
        if (transfer.getToFloorId() != null) {
            Floor floor = floorService.getFloorById(transfer.getToFloorId());
            if (floor != null) {
                vo.setToFloorName(floor.getFloorName());
            }
        }
        if (transfer.getToRoomId() != null) {
            ReceptionRoom room = roomService.getRoomById(transfer.getToRoomId());
            if (room != null) {
                vo.setToRoomName(room.getRoomName());
            }
        }
        return vo;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "正常";
            case 0: return "损坏";
            case 2: return "待维修";
            default: return "未知";
        }
    }

    private void updateRoomEquipmentCount(Long roomId, int delta) {
        ReceptionRoom room = roomService.getRoomById(roomId);
        if (room != null) {
            int newCount = (room.getEquipmentCount() != null ? room.getEquipmentCount() : 0) + delta;
            room.setEquipmentCount(Math.max(0, newCount));
            roomService.updateRoom(roomId, convertRoomToRequest(room));
        }
    }

    private com.example.devicemanagement.dto.request.ReceptionRoomRequest convertRoomToRequest(ReceptionRoom room) {
        com.example.devicemanagement.dto.request.ReceptionRoomRequest request = new com.example.devicemanagement.dto.request.ReceptionRoomRequest();
        request.setRoomName(room.getRoomName());
        request.setRoomCode(room.getRoomCode());
        request.setFloorId(room.getFloorId());
        request.setCapacity(room.getCapacity());
        request.setEquipmentCount(room.getEquipmentCount());
        request.setStatus(room.getStatus());
        return request;
    }
}
