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
import com.example.devicemanagement.dto.response.WarrantyDeviceVO;
import com.example.devicemanagement.dto.response.WarrantyFloorGroupVO;
import com.example.devicemanagement.dto.response.WarrantyOverviewVO;
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
import com.example.devicemanagement.service.RoomActivityService;
import com.example.devicemanagement.service.SpecTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeviceServiceImpl implements DeviceService {

    /**
     * 保修临期阈值：距截止不足该天数标黄
     */
    private static final int WARRANTY_EXPIRING_SOON_DAYS = 15;

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

    @Autowired
    private RoomActivityService roomActivityService;

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

        // 活动进行中的设备不得再被调配到其他房间
        if (request.getCurrentRoomId() == null || !request.getCurrentRoomId().equals(oldRoomId)) {
            roomActivityService.assertDeviceTransferable(device.getId());
        }
        // 待维修设备已卸下送修，不能再被分配到任何接待室
        if (request.getCurrentRoomId() != null
                && Integer.valueOf(2).equals(request.getStatus() == null ? device.getStatus() : request.getStatus())
                && !request.getCurrentRoomId().equals(oldRoomId)) {
            throw new IllegalArgumentException("设备「" + device.getDeviceName()
                    + "」处于待维修状态，修复前不能再调配到其他接待室");
        }

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
    public WarrantyOverviewVO getWarrantyOverview(Long floorId, Long roomId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (floorId != null) {
            wrapper.eq(Device::getCurrentFloorId, floorId);
        }
        if (roomId != null) {
            wrapper.eq(Device::getCurrentRoomId, roomId);
        }
        List<Device> devices = deviceMapper.selectList(wrapper);

        List<Floor> floors = floorService.getAllFloors();
        Map<Long, String> floorNames = floors.stream()
                .collect(Collectors.toMap(Floor::getId, Floor::getFloorName));
        Map<Long, String> roomNames = roomService.getAllRooms().stream()
                .collect(Collectors.toMap(ReceptionRoom::getId, ReceptionRoom::getRoomName));

        LocalDate today = LocalDate.now();
        Map<Long, List<WarrantyDeviceVO>> datedByFloor = new LinkedHashMap<>();
        List<WarrantyDeviceVO> datedUnassigned = new ArrayList<>();
        List<WarrantyDeviceVO> undated = new ArrayList<>();

        for (Device device : devices) {
            WarrantyDeviceVO vo = convertToWarrantyVO(device, today, floorNames, roomNames);
            if (device.getWarrantyEndDate() == null) {
                undated.add(vo);
            } else if (vo.getCurrentFloorId() != null && floorNames.containsKey(vo.getCurrentFloorId())) {
                datedByFloor.computeIfAbsent(vo.getCurrentFloorId(), k -> new ArrayList<>()).add(vo);
            } else {
                datedUnassigned.add(vo);
            }
        }

        // 已过期（负数）排最前，其次临期，同组内按剩余天数升序
        Comparator<WarrantyDeviceVO> byDaysRemaining = Comparator.comparing(WarrantyDeviceVO::getDaysRemaining);
        datedByFloor.values().forEach(list -> list.sort(byDaysRemaining));
        datedUnassigned.sort(byDaysRemaining);
        undated.sort(Comparator.comparing(WarrantyDeviceVO::getDeviceCode,
                Comparator.nullsLast(Comparator.naturalOrder())));

        List<WarrantyFloorGroupVO> groups = new ArrayList<>();
        for (Floor floor : floors) {
            List<WarrantyDeviceVO> floorDevices = datedByFloor.get(floor.getId());
            if (floorDevices != null && !floorDevices.isEmpty()) {
                WarrantyFloorGroupVO group = new WarrantyFloorGroupVO();
                group.setFloorId(floor.getId());
                group.setFloorName(floor.getFloorName());
                group.setDevices(floorDevices);
                groups.add(group);
            }
        }
        if (!datedUnassigned.isEmpty()) {
            WarrantyFloorGroupVO group = new WarrantyFloorGroupVO();
            group.setFloorId(null);
            group.setFloorName("未分配楼层");
            group.setDevices(datedUnassigned);
            groups.add(group);
        }

        WarrantyOverviewVO overview = new WarrantyOverviewVO();
        overview.setExpiringSoonDays(WARRANTY_EXPIRING_SOON_DAYS);
        overview.setGroups(groups);
        overview.setNoWarrantyDevices(undated);
        return overview;
    }

    private WarrantyDeviceVO convertToWarrantyVO(Device device, LocalDate today,
                                                 Map<Long, String> floorNames, Map<Long, String> roomNames) {
        WarrantyDeviceVO vo = new WarrantyDeviceVO();
        vo.setId(device.getId());
        vo.setDeviceCode(device.getDeviceCode());
        vo.setDeviceName(device.getDeviceName());
        vo.setDeviceType(device.getDeviceType());
        vo.setBrand(device.getBrand());
        vo.setModel(device.getModel());
        vo.setCurrentFloorId(device.getCurrentFloorId());
        vo.setCurrentFloorName(device.getCurrentFloorId() != null ? floorNames.get(device.getCurrentFloorId()) : null);
        vo.setCurrentRoomId(device.getCurrentRoomId());
        vo.setCurrentRoomName(device.getCurrentRoomId() != null ? roomNames.get(device.getCurrentRoomId()) : null);
        vo.setWarrantyEndDate(device.getWarrantyEndDate());

        if (device.getWarrantyEndDate() == null) {
            vo.setWarrantyStatus("NONE");
            vo.setWarrantyStatusText("未设置");
            return vo;
        }
        long daysRemaining = ChronoUnit.DAYS.between(today, device.getWarrantyEndDate());
        vo.setDaysRemaining(daysRemaining);
        if (daysRemaining < 0) {
            vo.setWarrantyStatus("EXPIRED");
            vo.setWarrantyStatusText("已过期");
        } else if (daysRemaining < WARRANTY_EXPIRING_SOON_DAYS) {
            vo.setWarrantyStatus("EXPIRING");
            vo.setWarrantyStatusText("临期");
        } else {
            vo.setWarrantyStatus("NORMAL");
            vo.setWarrantyStatusText("正常");
        }
        return vo;
    }

    @Override
    @Transactional
    public DeviceVO transferDevice(DeviceTransferRequest request) {
        Device device = deviceMapper.selectById(request.getDeviceId());
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }

        // 活动进行中的设备不得再被调配到其他房间
        if (request.getToRoomId() == null || !request.getToRoomId().equals(device.getCurrentRoomId())) {
            roomActivityService.assertDeviceTransferable(device.getId());
        }
        // 待维修设备已卸下送修，不能再调去别的接待室
        if (request.getToRoomId() != null && Integer.valueOf(2).equals(device.getStatus())) {
            throw new IllegalArgumentException("设备「" + device.getDeviceName()
                    + "」处于待维修状态，修复前不能再调配到其他接待室");
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
    public IPage<TransferRecordVO> getAllTransferRecords(int pageNum, int pageSize, String deviceName, String operator) {
        Page<DeviceTransfer> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DeviceTransfer> wrapper = new LambdaQueryWrapper<>();

        // 设备名称不在流转表上，先按名称匹配设备ID再过滤流转记录
        if (deviceName != null && !deviceName.trim().isEmpty()) {
            LambdaQueryWrapper<Device> deviceWrapper = new LambdaQueryWrapper<>();
            deviceWrapper.like(Device::getDeviceName, deviceName.trim());
            List<Long> deviceIds = deviceMapper.selectList(deviceWrapper).stream()
                    .map(Device::getId)
                    .collect(Collectors.toList());
            if (deviceIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            wrapper.in(DeviceTransfer::getDeviceId, deviceIds);
        }
        if (operator != null && !operator.trim().isEmpty()) {
            wrapper.like(DeviceTransfer::getOperator, operator.trim());
        }
        wrapper.orderByDesc(DeviceTransfer::getTransferTime);

        IPage<DeviceTransfer> transferPage = transferMapper.selectPage(page, wrapper);
        return transferPage.convert(this::convertTransferToVO);
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
