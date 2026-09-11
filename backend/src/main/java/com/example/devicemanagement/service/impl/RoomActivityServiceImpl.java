package com.example.devicemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.RoomActivityCreateRequest;
import com.example.devicemanagement.dto.response.RoomActivityDeviceVO;
import com.example.devicemanagement.dto.response.RoomActivityVO;
import com.example.devicemanagement.dto.response.RoomOccupancyVO;
import com.example.devicemanagement.entity.Device;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.entity.RoomActivity;
import com.example.devicemanagement.entity.RoomActivityDevice;
import com.example.devicemanagement.mapper.DeviceMapper;
import com.example.devicemanagement.mapper.RoomActivityDeviceMapper;
import com.example.devicemanagement.mapper.RoomActivityMapper;
import com.example.devicemanagement.service.FloorService;
import com.example.devicemanagement.service.ReceptionRoomService;
import com.example.devicemanagement.service.RoomActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoomActivityServiceImpl implements RoomActivityService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Autowired
    private RoomActivityMapper activityMapper;

    @Autowired
    private RoomActivityDeviceMapper activityDeviceMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private FloorService floorService;

    @Autowired
    private ReceptionRoomService roomService;

    @Override
    @Transactional
    public RoomActivityVO createActivity(RoomActivityCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        // 先把到期/到时的活动状态推进，避免历史状态导致误拦
        refreshExpired(now);

        String name = request.getActivityName() == null ? null : request.getActivityName().trim();
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("活动名称不能为空");
        }
        if (request.getRoomId() == null) {
            throw new IllegalArgumentException("请选择接待室");
        }
        ReceptionRoom room = roomService.getRoomById(request.getRoomId());
        if (room == null) {
            throw new IllegalArgumentException("所选接待室不存在");
        }
        if (!Integer.valueOf(1).equals(room.getStatus())) {
            throw new IllegalArgumentException("所选接待室已停用，无法登记活动");
        }
        LocalDateTime start = request.getStartTime();
        LocalDateTime end = request.getEndTime();
        if (start == null || end == null) {
            throw new IllegalArgumentException("请选择活动开始与结束时间");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        if (!end.isAfter(now)) {
            throw new IllegalArgumentException("结束时间已过，不能登记已结束的活动");
        }
        String manager = request.getManager() == null ? null : request.getManager().trim();
        if (manager == null || manager.isEmpty()) {
            throw new IllegalArgumentException("负责人不能为空");
        }
        if (request.getDeviceIds() == null || request.getDeviceIds().isEmpty()) {
            throw new IllegalArgumentException("请至少选择一台预计使用的影音设备");
        }

        // 同一接待室时段重叠必须拦住（边界相接不算重叠）
        List<RoomActivity> roomConflicts = findOverlappingActivities(
                room.getId(), start, end, null);
        if (!roomConflicts.isEmpty()) {
            RoomActivity other = roomConflicts.get(0);
            throw new IllegalArgumentException("同一接待室时段冲突：活动「" + other.getActivityName()
                    + "」占用 " + other.getStartTime().format(TIME_FMT) + " ~ "
                    + other.getEndTime().format(TIME_FMT) + "，与所选时段重叠");
        }

        // 设备校验：存在、属于该接待室、状态正常、登记内不重复
        List<Long> deviceIds = request.getDeviceIds().stream().distinct().collect(Collectors.toList());
        List<Device> devices = deviceMapper.selectBatchIds(deviceIds);
        Map<Long, Device> deviceMap = devices.stream()
                .collect(Collectors.toMap(Device::getId, Function.identity(), (a, b) -> a));
        for (Long deviceId : deviceIds) {
            Device device = deviceMap.get(deviceId);
            if (device == null) {
                throw new IllegalArgumentException("所选设备不存在或已删除");
            }
            if (!room.getId().equals(device.getCurrentRoomId())) {
                throw new IllegalArgumentException("设备「" + device.getDeviceName()
                        + "」当前不在该接待室，不能登记使用");
            }
            if (!Integer.valueOf(1).equals(device.getStatus())) {
                throw new IllegalArgumentException("设备「" + device.getDeviceName()
                        + "」当前状态为" + deviceStatusText(device.getStatus()) + "，不能投入活动使用");
            }
        }

        // 活动进行中的设备不得再被调配到其他房间：设备时段重叠同样拦截
        Map<Long, RoomActivity> deviceConflict = findDeviceConflicts(deviceIds, start, end, null);
        if (!deviceConflict.isEmpty()) {
            Map<Long, String> roomNameMap = loadRoomMap();
            Long firstDeviceId = deviceConflict.keySet().iterator().next();
            RoomActivity other = deviceConflict.get(firstDeviceId);
            Device device = deviceMap.get(firstDeviceId);
            throw new IllegalArgumentException("设备「" + (device != null ? device.getDeviceName() : firstDeviceId)
                    + "」在该时段已被活动「" + other.getActivityName() + "」占用（接待室 "
                    + roomNameMap.getOrDefault(other.getRoomId(), String.valueOf(other.getRoomId())) + "）");
        }

        RoomActivity activity = new RoomActivity();
        activity.setActivityNo(generateActivityNo());
        activity.setActivityName(name);
        activity.setRoomId(room.getId());
        activity.setFloorId(room.getFloorId());
        activity.setStartTime(start);
        activity.setEndTime(end);
        activity.setManager(manager);
        // 登记当前时刻已在时段内的活动直接为进行中
        int initStatus = now.isBefore(start)
                ? RoomActivity.STATUS_PENDING : RoomActivity.STATUS_ONGOING;
        activity.setStatus(initStatus);
        activity.setRemark(request.getRemark());
        activityMapper.insert(activity);

        for (Long deviceId : deviceIds) {
            Device device = deviceMap.get(deviceId);
            RoomActivityDevice row = new RoomActivityDevice();
            row.setActivityId(activity.getId());
            row.setDeviceId(device.getId());
            row.setDeviceCode(device.getDeviceCode());
            row.setDeviceName(device.getDeviceName());
            row.setDeviceType(device.getDeviceType());
            activityDeviceMapper.insert(row);
        }

        return toVO(activity, loadFloorMap(), loadRoomMap(), deviceIds.size(), null, null);
    }

    @Override
    public IPage<RoomActivityVO> getActivitiesPage(int pageNum, int pageSize,
                                                   LocalDate date, Long floorId, Integer status) {
        refreshExpired(LocalDateTime.now());

        Page<RoomActivity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RoomActivity> wrapper = new LambdaQueryWrapper<>();
        if (date != null) {
            // 与所选自然日有交集即命中
            wrapper.lt(RoomActivity::getStartTime, date.plusDays(1).atStartOfDay());
            wrapper.gt(RoomActivity::getEndTime, date.atStartOfDay());
        }
        if (floorId != null) {
            wrapper.eq(RoomActivity::getFloorId, floorId);
        }
        if (status != null) {
            wrapper.eq(RoomActivity::getStatus, status);
        }
        wrapper.orderByDesc(RoomActivity::getStartTime).orderByDesc(RoomActivity::getId());
        IPage<RoomActivity> activityPage = activityMapper.selectPage(page, wrapper);

        Map<Long, String> floorMap = loadFloorMap();
        Map<Long, String> roomMap = loadRoomMap();
        Map<Long, Integer> countMap = loadDeviceCounts(activityPage.getRecords());
        return activityPage.convert(a -> toVO(
                a, floorMap, roomMap, countMap.getOrDefault(a.getId(), 0), null, null));
    }

    @Override
    public RoomActivityVO getActivityById(Long activityId) {
        RoomActivity activity = requireActivity(activityId);
        refreshOne(activity, LocalDateTime.now());

        LambdaQueryWrapper<RoomActivityDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomActivityDevice::getActivityId, activityId);
        wrapper.orderByAsc(RoomActivityDevice::getDeviceCode);
        List<RoomActivityDevice> rows = activityDeviceMapper.selectList(wrapper);

        Map<Long, String> floorMap = loadFloorMap();
        Map<Long, String> roomMap = loadRoomMap();
        List<Long> deviceIds = rows.stream().map(RoomActivityDevice::getDeviceId).collect(Collectors.toList());
        Map<Long, Device> deviceMap = deviceIds.isEmpty()
                ? Collections.emptyMap()
                : deviceMapper.selectBatchIds(deviceIds).stream()
                .collect(Collectors.toMap(Device::getId, Function.identity(), (a, b) -> a));

        boolean ended = Integer.valueOf(RoomActivity.STATUS_ENDED).equals(activity.getStatus());
        // 其他活动对同一批设备的时段占用（进行中/待开始）
        Map<Long, RoomActivity> deviceConflicts = ended
                ? Collections.emptyMap()
                : findDeviceConflicts(deviceIds, activity.getStartTime(), activity.getEndTime(), activity.getId());

        List<RoomActivityDeviceVO> deviceVOs = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        for (RoomActivityDevice row : rows) {
            RoomActivityDeviceVO vo = new RoomActivityDeviceVO();
            vo.setId(row.getId());
            vo.setActivityId(row.getActivityId());
            vo.setDeviceId(row.getDeviceId());
            vo.setDeviceCode(row.getDeviceCode());
            vo.setDeviceName(row.getDeviceName());
            vo.setDeviceType(row.getDeviceType());

            Device live = deviceMap.get(row.getDeviceId());
            if (live != null) {
                vo.setCurrentFloorId(live.getCurrentFloorId());
                vo.setCurrentFloorName(live.getCurrentFloorId() != null ? floorMap.get(live.getCurrentFloorId()) : null);
                vo.setCurrentRoomId(live.getCurrentRoomId());
                vo.setCurrentRoomName(live.getCurrentRoomId() != null ? roomMap.get(live.getCurrentRoomId()) : null);
                vo.setDeviceStatus(live.getStatus());
                vo.setDeviceStatusText(deviceStatusText(live.getStatus()));
            }

            String deviceConflict = null;
            if (!ended) {
                RoomActivity other = deviceConflicts.get(row.getDeviceId());
                if (other != null) {
                    deviceConflict = "该设备同时被活动「" + other.getActivityName()
                            + "」（" + roomMap.getOrDefault(other.getRoomId(), "其他接待室") + "）占用";
                } else if (live == null) {
                    deviceConflict = "设备已被删除";
                } else {
                    if (!activity.getRoomId().equals(live.getCurrentRoomId())) {
                        deviceConflict = "设备已调配至 "
                                + (vo.getCurrentRoomName() != null ? vo.getCurrentRoomName() : "其他接待室")
                                + "，不在本接待室";
                    } else if (!Integer.valueOf(1).equals(live.getStatus())) {
                        deviceConflict = "设备当前状态为" + deviceStatusText(live.getStatus());
                    }
                }
            }
            vo.setConflict(deviceConflict);
            if (deviceConflict != null) {
                conflicts.add("设备「" + row.getDeviceName() + "」：" + deviceConflict);
            }
            deviceVOs.add(vo);
        }

        // 同一接待室时段重叠提示
        if (!ended) {
            List<RoomActivity> roomConflicts = findOverlappingActivities(
                    activity.getRoomId(), activity.getStartTime(), activity.getEndTime(), activity.getId());
            for (RoomActivity other : roomConflicts) {
                conflicts.add("与活动「" + other.getActivityName() + "」时段重叠（"
                        + other.getStartTime().format(TIME_FMT) + " ~ " + other.getEndTime().format(TIME_FMT) + "）");
            }
        }

        return toVO(activity, floorMap, roomMap, rows.size(), deviceVOs, conflicts);
    }

    @Override
    @Transactional
    public RoomActivityVO finishActivity(Long activityId) {
        RoomActivity activity = requireActivity(activityId);
        refreshOne(activity, LocalDateTime.now());
        if (Integer.valueOf(RoomActivity.STATUS_ENDED).equals(activity.getStatus())) {
            throw new IllegalArgumentException("活动已结束，占用设备已释放");
        }
        // 手动结束：保留原计划结束时间，立即释放占用
        activity.setStatus(RoomActivity.STATUS_ENDED);
        activity.setReleasedAt(LocalDateTime.now());
        activityMapper.updateById(activity);
        return getActivityById(activityId);
    }

    @Override
    public List<RoomOccupancyVO> getRoomOccupancies(Long floorId) {
        refreshExpired(LocalDateTime.now());

        List<ReceptionRoom> rooms = roomService.getAllRooms().stream()
                .filter(r -> floorId == null || floorId.equals(r.getFloorId()))
                .collect(Collectors.toList());
        Map<Long, String> floorMap = loadFloorMap();

        LambdaQueryWrapper<RoomActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomActivity::getStatus, RoomActivity.STATUS_ONGOING);
        Map<Long, RoomActivity> ongoingMap = activityMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(RoomActivity::getRoomId, Function.identity(), (a, b) -> a));

        List<RoomOccupancyVO> result = new ArrayList<>();
        for (ReceptionRoom room : rooms) {
            RoomOccupancyVO vo = new RoomOccupancyVO();
            vo.setRoomId(room.getId());
            vo.setRoomName(room.getRoomName());
            vo.setFloorId(room.getFloorId());
            vo.setFloorName(floorMap.get(room.getFloorId()));
            RoomActivity ongoing = ongoingMap.get(room.getId());
            vo.setOccupied(ongoing != null);
            if (ongoing != null) {
                vo.setActivityId(ongoing.getId());
                vo.setActivityName(ongoing.getActivityName());
                vo.setManager(ongoing.getManager());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public void assertDeviceTransferable(Long deviceId) {
        if (deviceId == null) {
            return;
        }
        LambdaQueryWrapper<RoomActivityDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomActivityDevice::getDeviceId, deviceId);
        List<RoomActivityDevice> rows = activityDeviceMapper.selectList(wrapper);
        if (rows.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Long> activityIds = rows.stream().map(RoomActivityDevice::getActivityId).distinct().collect(Collectors.toList());
        List<RoomActivity> activities = activityMapper.selectBatchIds(activityIds);
        for (RoomActivity activity : activities) {
            refreshOne(activity, now);
            if (Integer.valueOf(RoomActivity.STATUS_ONGOING).equals(activity.getStatus())) {
                throw new IllegalArgumentException("设备正被进行中的活动「" + activity.getActivityName()
                        + "」使用，活动结束前不可调配");
            }
        }
    }

    // ---------------- 私有辅助方法 ----------------

    private RoomActivity requireActivity(Long activityId) {
        RoomActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        return activity;
    }

    /**
     * 按当前时间推进单个活动状态：到点开始、到期结束并释放占用。
     */
    private RoomActivity refreshOne(RoomActivity activity, LocalDateTime now) {
        if (activity == null || Integer.valueOf(RoomActivity.STATUS_ENDED).equals(activity.getStatus())) {
            return activity;
        }
        boolean changed = false;
        if (!now.isBefore(activity.getEndTime())) {
            activity.setStatus(RoomActivity.STATUS_ENDED);
            activity.setReleasedAt(activity.getEndTime());
            changed = true;
        } else if (!now.isBefore(activity.getStartTime())
                && Integer.valueOf(RoomActivity.STATUS_PENDING).equals(activity.getStatus())) {
            activity.setStatus(RoomActivity.STATUS_ONGOING);
            changed = true;
        }
        if (changed) {
            activityMapper.updateById(activity);
        }
        return activity;
    }

    /**
     * 批量推进所有未结束活动：刷新后接待室状态、设备归属、活动列表保持一致。
     */
    private void refreshExpired(LocalDateTime now) {
        LambdaQueryWrapper<RoomActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(RoomActivity::getStatus, RoomActivity.STATUS_ENDED);
        List<RoomActivity> actives = activityMapper.selectList(wrapper);
        for (RoomActivity activity : actives) {
            refreshOne(activity, now);
        }
    }

    /**
     * 同一接待室时段重叠的其他活动（边界相接不算重叠：start &lt; other.end 且 other.start &lt; end）。
     */
    private List<RoomActivity> findOverlappingActivities(Long roomId, LocalDateTime start,
                                                         LocalDateTime end, Long excludeId) {
        LambdaQueryWrapper<RoomActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomActivity::getRoomId, roomId);
        wrapper.ne(RoomActivity::getStatus, RoomActivity.STATUS_ENDED);
        wrapper.lt(RoomActivity::getStartTime, end);
        wrapper.gt(RoomActivity::getEndTime, start);
        if (excludeId != null) {
            wrapper.ne(RoomActivity::getId, excludeId);
        }
        return activityMapper.selectList(wrapper);
    }

    /**
     * 设备维度的时段冲突：设备已被其他未结束活动在重叠时段占用。
     */
    private Map<Long, RoomActivity> findDeviceConflicts(List<Long> deviceIds,
                                                        LocalDateTime start, LocalDateTime end,
                                                        Long excludeActivityId) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<RoomActivityDevice> deviceWrapper = new LambdaQueryWrapper<>();
        deviceWrapper.in(RoomActivityDevice::getDeviceId, deviceIds);
        List<RoomActivityDevice> rows = activityDeviceMapper.selectList(deviceWrapper);
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> activityIds = rows.stream()
                .map(RoomActivityDevice::getActivityId)
                .filter(id -> !Objects.equals(id, excludeActivityId))
                .collect(Collectors.toSet());
        if (activityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, RoomActivity> activityMap = activityMapper.selectBatchIds(activityIds).stream()
                .collect(Collectors.toMap(RoomActivity::getId, Function.identity(), (a, b) -> a));

        Map<Long, RoomActivity> result = new HashMap<>();
        for (RoomActivityDevice row : rows) {
            RoomActivity other = activityMap.get(row.getActivityId());
            if (other == null || Integer.valueOf(RoomActivity.STATUS_ENDED).equals(other.getStatus())) {
                continue;
            }
            if (!start.isBefore(other.getEndTime()) || !other.getStartTime().isBefore(end)) {
                continue;
            }
            // 同一设备只保留一条冲突提示
            result.putIfAbsent(row.getDeviceId(), other);
        }
        return result;
    }

    private Map<Long, Integer> loadDeviceCounts(List<RoomActivity> activities) {
        if (activities.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> activityIds = activities.stream().map(RoomActivity::getId).collect(Collectors.toList());
        LambdaQueryWrapper<RoomActivityDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RoomActivityDevice::getActivityId, activityIds);
        Map<Long, Integer> result = new HashMap<>();
        for (RoomActivityDevice row : activityDeviceMapper.selectList(wrapper)) {
            result.merge(row.getActivityId(), 1, Integer::sum);
        }
        return result;
    }

    private Map<Long, String> loadFloorMap() {
        return floorService.getAllFloors().stream()
                .collect(Collectors.toMap(Floor::getId, Floor::getFloorName, (a, b) -> a));
    }

    private Map<Long, String> loadRoomMap() {
        return roomService.getAllRooms().stream()
                .collect(Collectors.toMap(ReceptionRoom::getId, ReceptionRoom::getRoomName, (a, b) -> a));
    }

    private RoomActivityVO toVO(RoomActivity activity, Map<Long, String> floorMap,
                                Map<Long, String> roomMap, int deviceCount,
                                List<RoomActivityDeviceVO> devices, List<String> conflicts) {
        RoomActivityVO vo = new RoomActivityVO();
        vo.setId(activity.getId());
        vo.setActivityNo(activity.getActivityNo());
        vo.setActivityName(activity.getActivityName());
        vo.setRoomId(activity.getRoomId());
        vo.setRoomName(roomMap.get(activity.getRoomId()));
        vo.setFloorId(activity.getFloorId());
        vo.setFloorName(floorMap.get(activity.getFloorId()));
        ReceptionRoom room = activity.getRoomId() != null ? roomService.getRoomById(activity.getRoomId()) : null;
        vo.setRoomCode(room != null ? room.getRoomCode() : null);
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setManager(activity.getManager());
        vo.setStatus(activity.getStatus());
        vo.setStatusText(statusText(activity.getStatus()));
        vo.setDeviceCount(deviceCount);
        vo.setReleasedAt(activity.getReleasedAt());
        vo.setRemark(activity.getRemark());
        vo.setCreatedAt(activity.getCreatedAt());
        vo.setUpdatedAt(activity.getUpdatedAt());
        vo.setDevices(devices);
        vo.setConflicts(conflicts != null ? conflicts : new ArrayList<>());
        return vo;
    }

    private String generateActivityNo() {
        return "HD" + LocalDateTime.now().format(NO_FMT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String statusText(int status) {
        switch (status) {
            case RoomActivity.STATUS_PENDING: return "待开始";
            case RoomActivity.STATUS_ONGOING: return "进行中";
            case RoomActivity.STATUS_ENDED: return "已结束";
            default: return "未知";
        }
    }

    private String deviceStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "正常";
            case 0: return "损坏";
            case 2: return "待维修";
            default: return "未知";
        }
    }
}
