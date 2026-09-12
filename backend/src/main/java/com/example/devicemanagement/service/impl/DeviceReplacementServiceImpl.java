package com.example.devicemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.DeviceReplacementRequest;
import com.example.devicemanagement.dto.request.ReceptionRoomRequest;
import com.example.devicemanagement.dto.request.ReplacementResultRequest;
import com.example.devicemanagement.dto.response.DeviceReplacementVO;
import com.example.devicemanagement.dto.response.SpareDeviceVO;
import com.example.devicemanagement.entity.Device;
import com.example.devicemanagement.entity.DeviceReplacement;
import com.example.devicemanagement.entity.DeviceTransfer;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.entity.RoomActivity;
import com.example.devicemanagement.entity.RoomActivityDevice;
import com.example.devicemanagement.mapper.DeviceMapper;
import com.example.devicemanagement.mapper.DeviceReplacementMapper;
import com.example.devicemanagement.mapper.DeviceTransferMapper;
import com.example.devicemanagement.mapper.RoomActivityDeviceMapper;
import com.example.devicemanagement.mapper.RoomActivityMapper;
import com.example.devicemanagement.service.DeviceReplacementService;
import com.example.devicemanagement.service.FloorService;
import com.example.devicemanagement.service.ReceptionRoomService;
import com.example.devicemanagement.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeviceReplacementServiceImpl implements DeviceReplacementService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 设备状态：损坏 */
    private static final int DEVICE_STATUS_BROKEN = 0;
    /** 设备状态：正常 */
    private static final int DEVICE_STATUS_NORMAL = 1;
    /** 设备状态：待维修 */
    private static final int DEVICE_STATUS_WAIT_REPAIR = 2;

    @Autowired
    private DeviceReplacementMapper replacementMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceTransferMapper transferMapper;

    @Autowired
    private RoomActivityMapper activityMapper;

    @Autowired
    private RoomActivityDeviceMapper activityDeviceMapper;

    @Autowired
    private FloorService floorService;

    @Autowired
    private ReceptionRoomService roomService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    @Transactional
    public DeviceReplacementVO createReplacement(DeviceReplacementRequest request) {
        if (request.getFaultyDeviceId() == null) {
            throw new IllegalArgumentException("请选择发生故障的设备");
        }
        if (request.getSpareDeviceId() == null) {
            throw new IllegalArgumentException("请选择同楼层的备用设备");
        }
        String phenomenon = request.getFaultPhenomenon() == null ? null : request.getFaultPhenomenon().trim();
        if (phenomenon == null || phenomenon.isEmpty()) {
            throw new IllegalArgumentException("请填写故障现象");
        }
        String operator = request.getOperator() == null ? null : request.getOperator().trim();
        if (operator == null || operator.isEmpty()) {
            throw new IllegalArgumentException("请填写替换操作人");
        }

        Device faulty = deviceMapper.selectById(request.getFaultyDeviceId());
        if (faulty == null) {
            throw new IllegalArgumentException("故障设备不存在或已删除");
        }
        if (faulty.getCurrentRoomId() == null) {
            throw new IllegalArgumentException("故障设备当前不在任何接待室，无法执行接待中替换");
        }
        ReceptionRoom room = roomService.getRoomById(faulty.getCurrentRoomId());
        if (room == null) {
            throw new IllegalArgumentException("故障设备所属接待室不存在");
        }
        if (!room.getFloorId().equals(faulty.getCurrentFloorId())) {
            throw new IllegalArgumentException("故障设备台账楼层与接待室楼层不一致，请先核对设备归属");
        }

        Device spare = deviceMapper.selectById(request.getSpareDeviceId());
        if (spare == null) {
            throw new IllegalArgumentException("备用设备不存在或已删除");
        }
        if (spare.getId().equals(faulty.getId())) {
            throw new IllegalArgumentException("备用设备不能与故障设备相同");
        }
        if (!Integer.valueOf(DEVICE_STATUS_NORMAL).equals(spare.getStatus())) {
            throw new IllegalArgumentException("备用设备「" + spare.getDeviceName() + "」状态不是正常，不能顶上");
        }
        if (spare.getCurrentRoomId() != null) {
            throw new IllegalArgumentException("备用设备「" + spare.getDeviceName()
                    + "」已分配接待室，请选择未上墙的楼层备用机");
        }
        if (!room.getFloorId().equals(spare.getCurrentFloorId())) {
            throw new IllegalArgumentException("备用设备「" + spare.getDeviceName()
                    + "」与接待室不在同一楼层，应急替换只能调用同楼层备用机");
        }

        LocalDateTime now = LocalDateTime.now();
        Long floorId = room.getFloorId();
        Long oldFaultyFloor = faulty.getCurrentFloorId();
        String remark = request.getRemark();

        // 1. 原设备改为待维修、卸下接待室，后续不能再调去别的接待室。
        // 注意：updateById 默认 NOT_NULL 策略会跳过 null 字段，current_room_id 无法清空，
        // 必须用 UpdateWrapper#set 显式置 NULL，否则故障机仍挂在原接待室、修复后又能被占用。
        faulty.setStatus(DEVICE_STATUS_WAIT_REPAIR);
        faulty.setCurrentRoomId(null);
        deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getId, faulty.getId())
                .set(Device::getStatus, DEVICE_STATUS_WAIT_REPAIR)
                .set(Device::getCurrentRoomId, null));

        // 2. 备用机转入该接待室
        spare.setCurrentFloorId(floorId);
        spare.setCurrentRoomId(room.getId());
        deviceMapper.updateById(spare);

        // 接待室在用设备总数保持不变（一减一增）
        adjustRoomEquipmentCount(room.getId());

        // 3. 同步该接待室未结束活动中的占用设备：故障机 -> 备用机，保证活动设备清单一致
        swapActivityDevice(faulty, spare, now);

        // 4. 台账流转记录（故障机卸下、备用机调入），保持替换记录与设备归属一致
        insertTransfer(faulty.getId(), faulty.getDeviceCode(), oldFaultyFloor, room.getId(),
                floorId, null, "接待中故障应急替换-设备送修", operator, now, remark);
        insertTransfer(spare.getId(), spare.getDeviceCode(), floorId, null,
                floorId, room.getId(), "接待中故障应急替换-备用机顶替「" + faulty.getDeviceName() + "」",
                operator, now, remark);

        // 5. 替换记录
        DeviceReplacement replacement = new DeviceReplacement();
        replacement.setReplacementNo(generateReplacementNo());
        replacement.setRoomId(room.getId());
        replacement.setFloorId(floorId);
        replacement.setFaultyDeviceId(faulty.getId());
        replacement.setFaultyDeviceCode(faulty.getDeviceCode());
        replacement.setFaultyDeviceName(faulty.getDeviceName());
        replacement.setFaultyDeviceType(faulty.getDeviceType());
        replacement.setSpareDeviceId(spare.getId());
        replacement.setSpareDeviceCode(spare.getDeviceCode());
        replacement.setSpareDeviceName(spare.getDeviceName());
        replacement.setSpareDeviceType(spare.getDeviceType());
        replacement.setFaultPhenomenon(phenomenon);
        replacement.setOperator(operator);
        replacement.setReplacementTime(now);
        replacement.setProcessResult(DeviceReplacement.RESULT_PENDING_REPAIR);
        replacement.setRemark(remark);
        replacementMapper.insert(replacement);

        try {
            redisCacheService.invalidateRoomCache(floorId);
        } catch (Exception e) {
        }

        return toVO(replacement);
    }

    @Override
    public List<SpareDeviceVO> getSpares(Long faultyDeviceId) {
        if (faultyDeviceId == null) {
            return Collections.emptyList();
        }
        Device faulty = deviceMapper.selectById(faultyDeviceId);
        if (faulty == null) {
            throw new IllegalArgumentException("故障设备不存在或已删除");
        }
        Long floorId = faulty.getCurrentFloorId();
        if (floorId == null) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getCurrentFloorId, floorId);
        wrapper.isNull(Device::getCurrentRoomId);
        wrapper.eq(Device::getStatus, DEVICE_STATUS_NORMAL);
        wrapper.ne(Device::getId, faulty.getId());
        wrapper.orderByAsc(Device::getDeviceType).orderByAsc(Device::getDeviceCode);
        List<Device> devices = deviceMapper.selectList(wrapper);

        Floor floor = floorService.getFloorById(floorId);
        String floorName = floor != null ? floor.getFloorName() : null;

        List<SpareDeviceVO> result = new ArrayList<>();
        for (Device device : devices) {
            SpareDeviceVO vo = new SpareDeviceVO();
            vo.setId(device.getId());
            vo.setDeviceCode(device.getDeviceCode());
            vo.setDeviceName(device.getDeviceName());
            vo.setDeviceType(device.getDeviceType());
            vo.setBrand(device.getBrand());
            vo.setModel(device.getModel());
            vo.setCurrentFloorId(device.getCurrentFloorId());
            vo.setCurrentFloorName(floorName);
            vo.setStatus(device.getStatus());
            vo.setStatusText(deviceStatusText(device.getStatus()));
            vo.setSameDeviceType(device.getDeviceType() != null
                    && device.getDeviceType().equals(faulty.getDeviceType()));
            vo.setSameType(vo.getSameDeviceType()
                    && device.getBrand() != null && device.getBrand().equals(faulty.getBrand())
                    && device.getModel() != null && device.getModel().equals(faulty.getModel()));
            result.add(vo);
        }
        // 同设备类型优先、同型号更优先
        result.sort((a, b) -> {
            int bySameType = Boolean.compare(b.getSameType(), a.getSameType());
            if (bySameType != 0) return bySameType;
            int bySameDeviceType = Boolean.compare(b.getSameDeviceType(), a.getSameDeviceType());
            if (bySameDeviceType != 0) return bySameDeviceType;
            return a.getDeviceCode().compareTo(b.getDeviceCode());
        });
        return result;
    }

    @Override
    public IPage<DeviceReplacementVO> getReplacementsPage(int pageNum, int pageSize,
                                                           Long floorId, Long roomId, Integer processResult) {
        Page<DeviceReplacement> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DeviceReplacement> wrapper = new LambdaQueryWrapper<>();
        if (floorId != null) {
            wrapper.eq(DeviceReplacement::getFloorId, floorId);
        }
        if (roomId != null) {
            wrapper.eq(DeviceReplacement::getRoomId, roomId);
        }
        if (processResult != null) {
            wrapper.eq(DeviceReplacement::getProcessResult, processResult);
        }
        wrapper.orderByDesc(DeviceReplacement::getReplacementTime).orderByDesc(DeviceReplacement::getId);
        IPage<DeviceReplacement> resultPage = replacementMapper.selectPage(page, wrapper);
        return resultPage.convert(this::toVO);
    }

    @Override
    public DeviceReplacementVO getReplacementById(Long id) {
        DeviceReplacement replacement = replacementMapper.selectById(id);
        if (replacement == null) {
            throw new IllegalArgumentException("替换记录不存在");
        }
        return toVO(replacement);
    }

    @Override
    @Transactional
    public DeviceReplacementVO resolveResult(Long id, ReplacementResultRequest request) {
        DeviceReplacement replacement = replacementMapper.selectById(id);
        if (replacement == null) {
            throw new IllegalArgumentException("替换记录不存在");
        }
        if (!Integer.valueOf(DeviceReplacement.RESULT_PENDING_REPAIR).equals(replacement.getProcessResult())) {
            throw new IllegalArgumentException("该记录已登记处理结果，不能重复处理");
        }
        Integer result = request.getProcessResult();
        if (result == null
                || (!Integer.valueOf(DeviceReplacement.RESULT_REPAIRED).equals(result)
                && !Integer.valueOf(DeviceReplacement.RESULT_SCRAPPED).equals(result))) {
            throw new IllegalArgumentException("处理结果只能为已修复或已报废");
        }
        String processedBy = request.getProcessedBy() == null ? null : request.getProcessedBy().trim();
        if (processedBy == null || processedBy.isEmpty()) {
            throw new IllegalArgumentException("请填写处理登记人");
        }
        String processRemark = request.getProcessRemark() == null ? null : request.getProcessRemark().trim();
        if (processRemark == null || processRemark.isEmpty()) {
            throw new IllegalArgumentException("请填写处理备注");
        }

        LocalDateTime now = LocalDateTime.now();
        replacement.setProcessResult(result);
        replacement.setProcessRemark(processRemark);
        replacement.setProcessedBy(processedBy);
        replacement.setProcessedAt(now);
        replacementMapper.updateById(replacement);

        // 同步原故障设备状态，刷新后替换记录与设备状态保持一致
        Device faulty = deviceMapper.selectById(replacement.getFaultyDeviceId());
        if (faulty != null) {
            int newStatus = Integer.valueOf(DeviceReplacement.RESULT_REPAIRED).equals(result)
                    ? DEVICE_STATUS_NORMAL : DEVICE_STATUS_BROKEN;
            faulty.setStatus(newStatus);
            faulty.setCurrentRoomId(null);
            // 卸下的故障机不得再留在任何接待室：修复后回楼层备用、报废后不可使用。
            // 历史替换单若因置空未生效仍挂在原接待室，登记结果时一并卸下，避免“已修复”后又能被占用。
            deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                    .eq(Device::getId, faulty.getId())
                    .set(Device::getStatus, newStatus)
                    .set(Device::getCurrentRoomId, null));
        }

        return toVO(replacement);
    }

    // ---------------- 私有辅助方法 ----------------

    /**
     * 将该接待室未结束活动中对故障设备的占用替换为备用设备；
     * 活动进行中同样允许，保持「接待室在用设备」与活动清单一致。
     */
    private void swapActivityDevice(Device faulty, Device spare, LocalDateTime now) {
        LambdaQueryWrapper<RoomActivityDevice> rowWrapper = new LambdaQueryWrapper<>();
        rowWrapper.eq(RoomActivityDevice::getDeviceId, faulty.getId());
        List<RoomActivityDevice> rows = activityDeviceMapper.selectList(rowWrapper);
        if (rows.isEmpty()) {
            return;
        }
        Set<Long> activityIds = rows.stream()
                .map(RoomActivityDevice::getActivityId)
                .collect(Collectors.toSet());
        Map<Long, RoomActivity> activityMap = activityMapper.selectBatchIds(activityIds).stream()
                .collect(Collectors.toMap(RoomActivity::getId, Function.identity(), (a, b) -> a));

        for (RoomActivityDevice row : rows) {
            RoomActivity activity = activityMap.get(row.getActivityId());
            // 仅处理同接待室、未结束活动的占用行
            if (activity == null
                    || Integer.valueOf(RoomActivity.STATUS_ENDED).equals(activity.getStatus())
                    || !spare.getCurrentRoomId().equals(activity.getRoomId())) {
                continue;
            }
            // 备用机已在同一活动清单中则删除旧行，避免唯一键冲突与重复占用
            LambdaQueryWrapper<RoomActivityDevice> existsWrapper = new LambdaQueryWrapper<>();
            existsWrapper.eq(RoomActivityDevice::getActivityId, activity.getId());
            existsWrapper.eq(RoomActivityDevice::getDeviceId, spare.getId());
            if (activityDeviceMapper.selectCount(existsWrapper) > 0) {
                activityDeviceMapper.deleteById(row.getId());
            } else {
                row.setDeviceId(spare.getId());
                row.setDeviceCode(spare.getDeviceCode());
                row.setDeviceName(spare.getDeviceName());
                row.setDeviceType(spare.getDeviceType());
                activityDeviceMapper.updateById(row);
            }
        }
    }

    private void insertTransfer(Long deviceId, String deviceCode, Long fromFloorId, Long fromRoomId,
                                Long toFloorId, Long toRoomId, String reason,
                                String operator, LocalDateTime time, String remark) {
        DeviceTransfer transfer = new DeviceTransfer();
        transfer.setDeviceId(deviceId);
        transfer.setDeviceCode(deviceCode);
        transfer.setFromFloorId(fromFloorId);
        transfer.setFromRoomId(fromRoomId);
        transfer.setToFloorId(toFloorId);
        transfer.setToRoomId(toRoomId);
        transfer.setTransferReason(reason);
        transfer.setOperator(operator);
        transfer.setTransferTime(time);
        transfer.setRemark(remark);
        transferMapper.insert(transfer);
    }

    private DeviceReplacementVO toVO(DeviceReplacement r) {
        DeviceReplacementVO vo = new DeviceReplacementVO();
        vo.setId(r.getId());
        vo.setReplacementNo(r.getReplacementNo());
        vo.setRoomId(r.getRoomId());
        vo.setFloorId(r.getFloorId());

        ReceptionRoom room = r.getRoomId() != null ? roomService.getRoomById(r.getRoomId()) : null;
        if (room != null) {
            vo.setRoomName(room.getRoomName());
            vo.setRoomCode(room.getRoomCode());
        }
        if (r.getFloorId() != null) {
            Floor floor = floorService.getFloorById(r.getFloorId());
            if (floor != null) {
                vo.setFloorName(floor.getFloorName());
            }
        }

        vo.setFaultyDeviceId(r.getFaultyDeviceId());
        vo.setFaultyDeviceCode(r.getFaultyDeviceCode());
        vo.setFaultyDeviceName(r.getFaultyDeviceName());
        vo.setFaultyDeviceType(r.getFaultyDeviceType());
        vo.setSpareDeviceId(r.getSpareDeviceId());
        vo.setSpareDeviceCode(r.getSpareDeviceCode());
        vo.setSpareDeviceName(r.getSpareDeviceName());
        vo.setSpareDeviceType(r.getSpareDeviceType());

        // 实时设备信息：刷新后在用设备与设备状态保持一致
        Device faulty = r.getFaultyDeviceId() != null ? deviceMapper.selectById(r.getFaultyDeviceId()) : null;
        if (faulty != null) {
            vo.setFaultyDeviceStatus(faulty.getStatus());
            vo.setFaultyDeviceStatusText(deviceStatusText(faulty.getStatus()));
            vo.setFaultyDeviceRoomId(faulty.getCurrentRoomId());
            vo.setFaultyDeviceRoomName(resolveRoomName(faulty.getCurrentRoomId()));
        }
        Device spare = r.getSpareDeviceId() != null ? deviceMapper.selectById(r.getSpareDeviceId()) : null;
        if (spare != null) {
            vo.setSpareDeviceStatus(spare.getStatus());
            vo.setSpareDeviceStatusText(deviceStatusText(spare.getStatus()));
            vo.setSpareDeviceRoomId(spare.getCurrentRoomId());
            vo.setSpareDeviceRoomName(resolveRoomName(spare.getCurrentRoomId()));
        }

        vo.setFaultPhenomenon(r.getFaultPhenomenon());
        vo.setOperator(r.getOperator());
        vo.setReplacementTime(r.getReplacementTime());
        vo.setProcessResult(r.getProcessResult());
        vo.setProcessResultText(resultText(r.getProcessResult()));
        vo.setProcessRemark(r.getProcessRemark());
        vo.setProcessedBy(r.getProcessedBy());
        vo.setProcessedAt(r.getProcessedAt());
        vo.setRemark(r.getRemark());
        vo.setCreatedAt(r.getCreatedAt());
        vo.setUpdatedAt(r.getUpdatedAt());
        return vo;
    }

    private String resolveRoomName(Long roomId) {
        if (roomId == null) {
            return null;
        }
        ReceptionRoom room = roomService.getRoomById(roomId);
        return room != null ? room.getRoomName() : null;
    }

    /**
     * 一减一增后设备总数不变：以当前在用设备数为准校准，避免重复计数。
     */
    private void adjustRoomEquipmentCount(Long roomId) {
        ReceptionRoom room = roomService.getRoomById(roomId);
        if (room == null) {
            return;
        }
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getCurrentRoomId, roomId);
        long actual = deviceMapper.selectCount(wrapper);
        if (actual == (room.getEquipmentCount() == null ? 0 : room.getEquipmentCount())) {
            return;
        }
        ReceptionRoomRequest req = new ReceptionRoomRequest();
        req.setRoomName(room.getRoomName());
        req.setRoomCode(room.getRoomCode());
        req.setFloorId(room.getFloorId());
        req.setCapacity(room.getCapacity());
        req.setEquipmentCount((int) actual);
        req.setStatus(room.getStatus());
        roomService.updateRoom(roomId, req);
    }

    private String generateReplacementNo() {
        return "TH" + LocalDateTime.now().format(NO_FMT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String resultText(Integer result) {
        if (result == null) return "未知";
        switch (result) {
            case DeviceReplacement.RESULT_PENDING_REPAIR: return "待维修";
            case DeviceReplacement.RESULT_REPAIRED: return "已修复";
            case DeviceReplacement.RESULT_SCRAPPED: return "已报废";
            default: return "未知";
        }
    }

    private String deviceStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case DEVICE_STATUS_NORMAL: return "正常";
            case DEVICE_STATUS_BROKEN: return "损坏";
            case DEVICE_STATUS_WAIT_REPAIR: return "待维修";
            default: return "未知";
        }
    }
}
