package com.example.devicemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.ComboApplyRequest;
import com.example.devicemanagement.dto.request.DeviceComboRequest;
import com.example.devicemanagement.dto.response.ComboApplyResultVO;
import com.example.devicemanagement.dto.response.ComboDeviceVO;
import com.example.devicemanagement.dto.response.ComboRecordItemVO;
import com.example.devicemanagement.dto.response.ComboRecordVO;
import com.example.devicemanagement.dto.response.DeviceComboVO;
import com.example.devicemanagement.entity.Device;
import com.example.devicemanagement.entity.DeviceCombo;
import com.example.devicemanagement.entity.DeviceComboItem;
import com.example.devicemanagement.entity.DeviceComboRecord;
import com.example.devicemanagement.entity.DeviceComboRecordItem;
import com.example.devicemanagement.entity.DeviceTransfer;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.mapper.DeviceComboItemMapper;
import com.example.devicemanagement.mapper.DeviceComboMapper;
import com.example.devicemanagement.mapper.DeviceComboRecordItemMapper;
import com.example.devicemanagement.mapper.DeviceComboRecordMapper;
import com.example.devicemanagement.mapper.DeviceMapper;
import com.example.devicemanagement.mapper.DeviceTransferMapper;
import com.example.devicemanagement.service.DeviceComboService;
import com.example.devicemanagement.service.FloorService;
import com.example.devicemanagement.service.ReceptionRoomService;
import com.example.devicemanagement.service.RedisCacheService;
import com.example.devicemanagement.service.RoomActivityService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeviceComboServiceImpl implements DeviceComboService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 设备状态：损坏 */
    private static final int DEVICE_STATUS_BROKEN = 0;
    /** 设备状态：正常 */
    private static final int DEVICE_STATUS_NORMAL = 1;
    /** 设备状态：待维修 */
    private static final int DEVICE_STATUS_WAIT_REPAIR = 2;

    @Autowired
    private DeviceComboMapper comboMapper;

    @Autowired
    private DeviceComboItemMapper comboItemMapper;

    @Autowired
    private DeviceComboRecordMapper recordMapper;

    @Autowired
    private DeviceComboRecordItemMapper recordItemMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceTransferMapper transferMapper;

    @Autowired
    private FloorService floorService;

    @Autowired
    private ReceptionRoomService roomService;

    @Autowired
    private RoomActivityService roomActivityService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------- 组合维护 ----------------

    @Override
    @Transactional
    public DeviceComboVO createCombo(DeviceComboRequest request) {
        String name = requireText(request.getComboName(), "组合名称不能为空");
        LambdaQueryWrapper<DeviceCombo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceCombo::getComboName, name);
        if (comboMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("组合名称「" + name + "」已存在");
        }
        List<Long> deviceIds = requireDeviceIds(request.getDeviceIds());

        DeviceCombo combo = new DeviceCombo();
        combo.setComboName(name);
        combo.setRemark(trimToNull(request.getRemark()));
        combo.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        combo.setCreatedBy(trimToNull(request.getCreatedBy()));
        comboMapper.insert(combo);

        saveComboItems(combo.getId(), deviceIds);
        return getComboById(combo.getId());
    }

    @Override
    @Transactional
    public DeviceComboVO updateCombo(Long id, DeviceComboRequest request) {
        DeviceCombo combo = requireCombo(id);
        String name = requireText(request.getComboName(), "组合名称不能为空");
        LambdaQueryWrapper<DeviceCombo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceCombo::getComboName, name);
        wrapper.ne(DeviceCombo::getId, id);
        if (comboMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("组合名称「" + name + "」已存在");
        }
        combo.setComboName(name);
        combo.setRemark(trimToNull(request.getRemark()));
        if (request.getStatus() != null) {
            combo.setStatus(request.getStatus());
        }
        comboMapper.updateById(combo);

        if (request.getDeviceIds() != null) {
            List<Long> deviceIds = requireDeviceIds(request.getDeviceIds());
            LambdaQueryWrapper<DeviceComboItem> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(DeviceComboItem::getComboId, id);
            comboItemMapper.delete(deleteWrapper);
            saveComboItems(id, deviceIds);
        }
        return getComboById(id);
    }

    @Override
    @Transactional
    public void deleteCombo(Long id) {
        requireCombo(id);
        LambdaQueryWrapper<DeviceComboItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(DeviceComboItem::getComboId, id);
        comboItemMapper.delete(itemWrapper);
        comboMapper.deleteById(id);
    }

    @Override
    public DeviceComboVO getComboById(Long id) {
        DeviceCombo combo = requireCombo(id);
        return toComboVO(combo, loadComboItems(id));
    }

    @Override
    public List<DeviceComboVO> listCombos(Integer status) {
        LambdaQueryWrapper<DeviceCombo> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(DeviceCombo::getStatus, status);
        }
        wrapper.orderByDesc(DeviceCombo::getCreatedAt).orderByDesc(DeviceCombo::getId);
        List<DeviceCombo> combos = comboMapper.selectList(wrapper);

        LambdaQueryWrapper<DeviceComboItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.orderByAsc(DeviceComboItem::getSortOrder).orderByAsc(DeviceComboItem::getId);
        Map<Long, List<DeviceComboItem>> itemMap = comboItemMapper.selectList(itemWrapper).stream()
                .collect(Collectors.groupingBy(DeviceComboItem::getComboId));
        return combos.stream()
                .map(combo -> toComboVO(combo, itemMap.getOrDefault(combo.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    // ---------------- 一键套用 ----------------

    @Override
    @Transactional
    public ComboApplyResultVO applyCombo(Long comboId, ComboApplyRequest request) {
        DeviceCombo combo = requireCombo(comboId);
        if (!Integer.valueOf(1).equals(combo.getStatus())) {
            throw new IllegalArgumentException("组合「" + combo.getComboName() + "」已停用，不能套用");
        }
        if (request.getRoomId() == null) {
            throw new IllegalArgumentException("请选择要调入的接待室");
        }
        ReceptionRoom room = roomService.getRoomById(request.getRoomId());
        if (room == null) {
            throw new IllegalArgumentException("所选接待室不存在");
        }
        if (!Integer.valueOf(1).equals(room.getStatus())) {
            throw new IllegalArgumentException("所选接待室已停用，不能调入设备");
        }
        String operator = requireText(request.getOperator(), "请填写套用人（值班员）");

        Long targetFloorId = room.getFloorId();
        LocalDateTime now = LocalDateTime.now();

        // 组合设备按保存顺序
        List<DeviceComboItem> comboItems = loadComboItems(comboId);
        if (comboItems.isEmpty()) {
            throw new IllegalArgumentException("组合「" + combo.getComboName() + "」内没有设备，无法套用");
        }
        Map<Long, Device> deviceMap = deviceMapper.selectBatchIds(
                comboItems.stream().map(DeviceComboItem::getDeviceId).collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(Device::getId, Function.identity(), (a, b) -> a));

        // 套用前接待室设备清单（对照基线）
        List<Device> beforeDevices = listRoomDevices(room.getId());

        List<DeviceComboRecordItem> resultItems = new ArrayList<>();
        int applied = 0;
        int present = 0;
        int skipped = 0;
        int sortOrder = 0;

        for (DeviceComboItem comboItem : comboItems) {
            DeviceComboRecordItem resultItem = new DeviceComboRecordItem();
            resultItem.setDeviceId(comboItem.getDeviceId());
            resultItem.setDeviceCode(comboItem.getDeviceCode());
            resultItem.setDeviceName(comboItem.getDeviceName());
            resultItem.setDeviceType(comboItem.getDeviceType());
            resultItem.setSortOrder(sortOrder++);

            Device device = deviceMap.get(comboItem.getDeviceId());
            if (device == null) {
                resultItem.setResult(DeviceComboRecord.RESULT_SKIPPED);
                resultItem.setSkipReason("设备已从台账删除");
                skipped++;
            } else if (Integer.valueOf(DEVICE_STATUS_WAIT_REPAIR).equals(device.getStatus())) {
                // 待修设备即使挂在目标房间也不能算已就位
                resultItem.setResult(DeviceComboRecord.RESULT_SKIPPED);
                resultItem.setSkipReason("设备待修，已送修不能调配");
                resultItem.setFromRoomId(device.getCurrentRoomId());
                resultItem.setFromRoomName(resolveRoomName(device.getCurrentRoomId()));
                skipped++;
            } else if (Integer.valueOf(DEVICE_STATUS_BROKEN).equals(device.getStatus())) {
                resultItem.setResult(DeviceComboRecord.RESULT_SKIPPED);
                resultItem.setSkipReason("设备损坏，不能调配");
                resultItem.setFromRoomId(device.getCurrentRoomId());
                resultItem.setFromRoomName(resolveRoomName(device.getCurrentRoomId()));
                skipped++;
            } else if (room.getId().equals(device.getCurrentRoomId())) {
                // 正常且已在目标接待室：无需重复调入
                resultItem.setResult(DeviceComboRecord.RESULT_ALREADY_PRESENT);
                present++;
            } else if (device.getCurrentRoomId() != null) {
                // 正常但已在别的接待室上墙，属于占用设备，跳过
                resultItem.setResult(DeviceComboRecord.RESULT_SKIPPED);
                resultItem.setSkipReason("设备正在接待室「" + resolveRoomName(device.getCurrentRoomId()) + "」使用");
                resultItem.setFromRoomId(device.getCurrentRoomId());
                resultItem.setFromRoomName(resolveRoomName(device.getCurrentRoomId()));
                skipped++;
            } else if (!Integer.valueOf(DEVICE_STATUS_NORMAL).equals(device.getStatus())) {
                resultItem.setResult(DeviceComboRecord.RESULT_SKIPPED);
                resultItem.setSkipReason("设备状态异常（" + deviceStatusText(device.getStatus()) + "），不能调配");
                skipped++;
            } else {
                // 未上墙空闲设备还需排除被进行中活动占用的情况
                String blockingActivity = roomActivityService.getBlockingActivityName(device.getId());
                if (blockingActivity != null) {
                    resultItem.setResult(DeviceComboRecord.RESULT_SKIPPED);
                    resultItem.setSkipReason("设备正被进行中的活动「" + blockingActivity + "」占用");
                    skipped++;
                } else {
                    Long oldFloorId = device.getCurrentFloorId();
                    Long oldRoomId = device.getCurrentRoomId();

                    // 调入目标接待室
                    device.setCurrentFloorId(targetFloorId);
                    device.setCurrentRoomId(room.getId());
                    deviceMapper.updateById(device);

                    // 写流转台账：组合一键套用调入
                    DeviceTransfer transfer = new DeviceTransfer();
                    transfer.setDeviceId(device.getId());
                    transfer.setDeviceCode(device.getDeviceCode());
                    transfer.setFromFloorId(oldFloorId);
                    transfer.setFromRoomId(oldRoomId);
                    transfer.setToFloorId(targetFloorId);
                    transfer.setToRoomId(room.getId());
                    transfer.setTransferReason("一键套用影音组合「" + combo.getComboName() + "」调入");
                    transfer.setOperator(operator);
                    transfer.setTransferTime(now);
                    transfer.setRemark(trimToNull(request.getRemark()));
                    transferMapper.insert(transfer);

                    resultItem.setResult(DeviceComboRecord.RESULT_APPLIED);
                    resultItem.setFromRoomId(oldRoomId);
                    resultItem.setFromRoomName(resolveRoomName(oldRoomId));
                    applied++;

                    if (oldFloorId != null) {
                        try {
                            redisCacheService.invalidateRoomCache(oldFloorId);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            resultItems.add(resultItem);
        }

        // 套用后接待室设备清单
        List<Device> afterDevices = listRoomDevices(room.getId());

        // 以实际在用设备数校准接待室设备数量
        recalibrateRoomCount(room, afterDevices.size());

        // 落库套用记录（即使全部跳过也保留记录，说明本次为什么没套上）
        DeviceComboRecord record = new DeviceComboRecord();
        record.setRecordNo(generateRecordNo());
        record.setComboId(combo.getId());
        record.setComboName(combo.getComboName());
        record.setRoomId(room.getId());
        record.setFloorId(targetFloorId);
        record.setOperator(operator);
        record.setApplyTime(now);
        record.setRequiredCount(comboItems.size());
        record.setAppliedCount(applied);
        record.setPresentCount(present);
        record.setSkippedCount(skipped);
        record.setBeforeSnapshot(writeSnapshot(beforeDevices));
        record.setAfterSnapshot(writeSnapshot(afterDevices));
        record.setRemark(trimToNull(request.getRemark()));
        recordMapper.insert(record);

        for (DeviceComboRecordItem resultItem : resultItems) {
            resultItem.setRecordId(record.getId());
            recordItemMapper.insert(resultItem);
        }

        try {
            redisCacheService.invalidateRoomCache(targetFloorId);
        } catch (Exception ignored) {
        }

        return toApplyResultVO(record, room, afterDevices, resultItems);
    }

    @Override
    public IPage<ComboRecordVO> getRecordsPage(int pageNum, int pageSize, Long floorId, Long roomId, Long comboId) {
        Page<DeviceComboRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DeviceComboRecord> wrapper = new LambdaQueryWrapper<>();
        if (floorId != null) {
            wrapper.eq(DeviceComboRecord::getFloorId, floorId);
        }
        if (roomId != null) {
            wrapper.eq(DeviceComboRecord::getRoomId, roomId);
        }
        if (comboId != null) {
            wrapper.eq(DeviceComboRecord::getComboId, comboId);
        }
        wrapper.orderByDesc(DeviceComboRecord::getApplyTime).orderByDesc(DeviceComboRecord::getId);
        IPage<DeviceComboRecord> recordPage = recordMapper.selectPage(page, wrapper);
        return recordPage.convert(r -> toRecordVO(r, null));
    }

    @Override
    public ComboRecordVO getRecordById(Long id) {
        DeviceComboRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("套用记录不存在");
        }
        LambdaQueryWrapper<DeviceComboRecordItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceComboRecordItem::getRecordId, id);
        wrapper.orderByAsc(DeviceComboRecordItem::getSortOrder).orderByAsc(DeviceComboRecordItem::getId);
        List<DeviceComboRecordItem> items = recordItemMapper.selectList(wrapper);
        return toRecordVO(record, items);
    }

    // ---------------- 私有辅助方法 ----------------

    private DeviceCombo requireCombo(Long id) {
        DeviceCombo combo = comboMapper.selectById(id);
        if (combo == null) {
            throw new IllegalArgumentException("影音组合不存在");
        }
        return combo;
    }

    private List<Long> requireDeviceIds(List<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一台组合设备");
        }
        List<Long> distinctIds = deviceIds.stream().distinct().collect(Collectors.toList());
        List<Device> devices = deviceMapper.selectBatchIds(distinctIds);
        if (devices.size() != distinctIds.size()) {
            throw new IllegalArgumentException("所选设备中有不存在或已删除的设备");
        }
        return distinctIds;
    }

    private void saveComboItems(Long comboId, List<Long> deviceIds) {
        Map<Long, Device> deviceMap = deviceMapper.selectBatchIds(deviceIds).stream()
                .collect(Collectors.toMap(Device::getId, Function.identity(), (a, b) -> a));
        int sortOrder = 0;
        for (Long deviceId : deviceIds) {
            Device device = deviceMap.get(deviceId);
            DeviceComboItem item = new DeviceComboItem();
            item.setComboId(comboId);
            item.setDeviceId(deviceId);
            item.setDeviceCode(device.getDeviceCode());
            item.setDeviceName(device.getDeviceName());
            item.setDeviceType(device.getDeviceType());
            item.setSortOrder(sortOrder++);
            comboItemMapper.insert(item);
        }
    }

    private List<DeviceComboItem> loadComboItems(Long comboId) {
        LambdaQueryWrapper<DeviceComboItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceComboItem::getComboId, comboId);
        wrapper.orderByAsc(DeviceComboItem::getSortOrder).orderByAsc(DeviceComboItem::getId);
        return comboItemMapper.selectList(wrapper);
    }

    private List<Device> listRoomDevices(Long roomId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getCurrentRoomId, roomId);
        wrapper.orderByAsc(Device::getDeviceType).orderByAsc(Device::getDeviceCode);
        return deviceMapper.selectList(wrapper);
    }

    private void recalibrateRoomCount(ReceptionRoom room, int actual) {
        int current = room.getEquipmentCount() == null ? 0 : room.getEquipmentCount();
        if (current == actual) {
            return;
        }
        com.example.devicemanagement.dto.request.ReceptionRoomRequest req =
                new com.example.devicemanagement.dto.request.ReceptionRoomRequest();
        req.setRoomName(room.getRoomName());
        req.setRoomCode(room.getRoomCode());
        req.setFloorId(room.getFloorId());
        req.setCapacity(room.getCapacity());
        req.setEquipmentCount(actual);
        req.setStatus(room.getStatus());
        roomService.updateRoom(room.getId(), req);
    }

    private DeviceComboVO toComboVO(DeviceCombo combo, List<DeviceComboItem> items) {
        DeviceComboVO vo = new DeviceComboVO();
        vo.setId(combo.getId());
        vo.setComboName(combo.getComboName());
        vo.setRemark(combo.getRemark());
        vo.setStatus(combo.getStatus());
        vo.setStatusText(Integer.valueOf(1).equals(combo.getStatus()) ? "启用" : "停用");
        vo.setCreatedBy(combo.getCreatedBy());
        vo.setCreatedAt(combo.getCreatedAt());
        vo.setUpdatedAt(combo.getUpdatedAt());
        vo.setDeviceCount(items.size());

        Map<Long, String> floorNames = floorService.getAllFloors().stream()
                .collect(Collectors.toMap(Floor::getId, Floor::getFloorName, (a, b) -> a));
        Map<Long, String> roomNames = roomService.getAllRooms().stream()
                .collect(Collectors.toMap(ReceptionRoom::getId, ReceptionRoom::getRoomName, (a, b) -> a));

        List<Long> deviceIds = items.stream().map(DeviceComboItem::getDeviceId).collect(Collectors.toList());
        Map<Long, Device> deviceMap = deviceIds.isEmpty()
                ? Collections.emptyMap()
                : deviceMapper.selectBatchIds(deviceIds).stream()
                .collect(Collectors.toMap(Device::getId, Function.identity(), (a, b) -> a));

        List<DeviceComboVO.ComboMemberVO> members = new ArrayList<>();
        for (DeviceComboItem item : items) {
            DeviceComboVO.ComboMemberVO member = new DeviceComboVO.ComboMemberVO();
            member.setDeviceId(item.getDeviceId());
            member.setDeviceCode(item.getDeviceCode());
            member.setDeviceName(item.getDeviceName());
            member.setDeviceType(item.getDeviceType());
            member.setSortOrder(item.getSortOrder());
            Device live = deviceMap.get(item.getDeviceId());
            member.setExists(live != null);
            if (live != null) {
                member.setBrand(live.getBrand());
                member.setModel(live.getModel());
                member.setLiveStatus(live.getStatus());
                member.setLiveStatusText(deviceStatusText(live.getStatus()));
                member.setCurrentFloorId(live.getCurrentFloorId());
                member.setCurrentFloorName(live.getCurrentFloorId() != null ? floorNames.get(live.getCurrentFloorId()) : null);
                member.setCurrentRoomId(live.getCurrentRoomId());
                member.setCurrentRoomName(live.getCurrentRoomId() != null ? roomNames.get(live.getCurrentRoomId()) : null);
            }
            members.add(member);
        }
        vo.setDevices(members);
        return vo;
    }

    private ComboApplyResultVO toApplyResultVO(DeviceComboRecord record, ReceptionRoom room,
                                               List<Device> afterDevices,
                                               List<DeviceComboRecordItem> resultItems) {
        ComboApplyResultVO vo = new ComboApplyResultVO();
        vo.setId(record.getId());
        vo.setRecordNo(record.getRecordNo());
        vo.setComboId(record.getComboId());
        vo.setComboName(record.getComboName());
        vo.setRoomId(record.getRoomId());
        vo.setRoomName(room.getRoomName());
        vo.setRoomCode(room.getRoomCode());
        vo.setFloorId(record.getFloorId());
        Floor floor = floorService.getFloorById(record.getFloorId());
        vo.setFloorName(floor != null ? floor.getFloorName() : null);
        vo.setOperator(record.getOperator());
        vo.setApplyTime(record.getApplyTime());
        vo.setRequiredCount(record.getRequiredCount());
        vo.setAppliedCount(record.getAppliedCount());
        vo.setPresentCount(record.getPresentCount());
        vo.setSkippedCount(record.getSkippedCount());
        vo.setRemark(record.getRemark());
        vo.setBeforeDevices(readSnapshot(record.getBeforeSnapshot()));
        vo.setAfterDevices(toComboDeviceList(afterDevices));
        vo.setItems(resultItems.stream().map(item -> toRecordItemVO(item, room.getId())).collect(Collectors.toList()));
        return vo;
    }

    private ComboRecordVO toRecordVO(DeviceComboRecord record, List<DeviceComboRecordItem> items) {
        ComboRecordVO vo = new ComboRecordVO();
        vo.setId(record.getId());
        vo.setRecordNo(record.getRecordNo());
        vo.setComboId(record.getComboId());
        vo.setComboName(record.getComboName());
        vo.setRoomId(record.getRoomId());
        vo.setFloorId(record.getFloorId());

        ReceptionRoom room = roomService.getRoomById(record.getRoomId());
        if (room != null) {
            vo.setRoomName(room.getRoomName());
            vo.setRoomCode(room.getRoomCode());
        }
        if (record.getFloorId() != null) {
            Floor floor = floorService.getFloorById(record.getFloorId());
            if (floor != null) {
                vo.setFloorName(floor.getFloorName());
            }
        }

        vo.setOperator(record.getOperator());
        vo.setApplyTime(record.getApplyTime());
        vo.setRequiredCount(record.getRequiredCount());
        vo.setAppliedCount(record.getAppliedCount());
        vo.setPresentCount(record.getPresentCount());
        vo.setSkippedCount(record.getSkippedCount());
        vo.setRemark(record.getRemark());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setBeforeDevices(readSnapshot(record.getBeforeSnapshot()));
        vo.setAfterDevices(readSnapshot(record.getAfterSnapshot()));

        if (items == null) {
            LambdaQueryWrapper<DeviceComboRecordItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DeviceComboRecordItem::getRecordId, record.getId());
            wrapper.orderByAsc(DeviceComboRecordItem::getSortOrder).orderByAsc(DeviceComboRecordItem::getId);
            items = recordItemMapper.selectList(wrapper);
        }
        vo.setItems(items.stream().map(item -> toRecordItemVO(item, record.getRoomId())).collect(Collectors.toList()));
        return vo;
    }

    private ComboRecordItemVO toRecordItemVO(DeviceComboRecordItem item, Long targetRoomId) {
        ComboRecordItemVO vo = new ComboRecordItemVO();
        vo.setId(item.getId());
        vo.setRecordId(item.getRecordId());
        vo.setDeviceId(item.getDeviceId());
        vo.setDeviceCode(item.getDeviceCode());
        vo.setDeviceName(item.getDeviceName());
        vo.setDeviceType(item.getDeviceType());
        vo.setResult(item.getResult());
        vo.setResultText(resultText(item.getResult()));
        vo.setSkipReason(item.getSkipReason());
        vo.setFromRoomId(item.getFromRoomId());
        vo.setFromRoomName(item.getFromRoomName());
        vo.setSortOrder(item.getSortOrder());

        // 实时台账：刷新后套用结果与设备实际归属、状态保持一致
        Device live = deviceMapper.selectById(item.getDeviceId());
        vo.setExists(live != null);
        if (live != null) {
            vo.setLiveStatus(live.getStatus());
            vo.setLiveStatusText(deviceStatusText(live.getStatus()));
            vo.setLiveRoomId(live.getCurrentRoomId());
            vo.setLiveRoomName(resolveRoomName(live.getCurrentRoomId()));
            vo.setLiveInTargetRoom(targetRoomId != null && targetRoomId.equals(live.getCurrentRoomId()));
        } else {
            vo.setLiveInTargetRoom(false);
        }
        return vo;
    }

    private List<ComboDeviceVO> toComboDeviceList(List<Device> devices) {
        return devices.stream().map(d -> {
            ComboDeviceVO vo = new ComboDeviceVO();
            vo.setId(d.getId());
            vo.setDeviceCode(d.getDeviceCode());
            vo.setDeviceName(d.getDeviceName());
            vo.setDeviceType(d.getDeviceType());
            vo.setBrand(d.getBrand());
            vo.setModel(d.getModel());
            vo.setStatus(d.getStatus());
            vo.setStatusText(deviceStatusText(d.getStatus()));
            return vo;
        }).collect(Collectors.toList());
    }

    private String writeSnapshot(List<Device> devices) {
        try {
            return objectMapper.writeValueAsString(toComboDeviceList(devices));
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<ComboDeviceVO> readSnapshot(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ComboDeviceVO>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String resolveRoomName(Long roomId) {
        if (roomId == null) {
            return null;
        }
        ReceptionRoom room = roomService.getRoomById(roomId);
        return room != null ? room.getRoomName() : null;
    }

    private String generateRecordNo() {
        return "TZ" + LocalDateTime.now().format(NO_FMT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String requireText(String value, String message) {
        String text = value == null ? null : value.trim();
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return text;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String resultText(Integer result) {
        if (result == null) {
            return "未知";
        }
        switch (result) {
            case DeviceComboRecord.RESULT_APPLIED: return "调入";
            case DeviceComboRecord.RESULT_ALREADY_PRESENT: return "已在房间";
            case DeviceComboRecord.RESULT_SKIPPED: return "跳过";
            default: return "未知";
        }
    }

    private String deviceStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case DEVICE_STATUS_NORMAL: return "正常";
            case DEVICE_STATUS_BROKEN: return "损坏";
            case DEVICE_STATUS_WAIT_REPAIR: return "待维修";
            default: return "未知";
        }
    }
}
