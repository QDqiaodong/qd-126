package com.example.devicemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.RoomQuietPeriodRequest;
import com.example.devicemanagement.dto.response.RoomQuietPeriodVO;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.entity.RoomQuietPeriod;
import com.example.devicemanagement.mapper.RoomQuietPeriodMapper;
import com.example.devicemanagement.service.FloorService;
import com.example.devicemanagement.service.ReceptionRoomService;
import com.example.devicemanagement.service.RoomQuietPeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class RoomQuietPeriodServiceImpl implements RoomQuietPeriodService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private RoomQuietPeriodMapper quietPeriodMapper;

    @Autowired
    private FloorService floorService;

    @Autowired
    private ReceptionRoomService roomService;

    @Override
    public RoomQuietPeriodVO createQuietPeriod(RoomQuietPeriodRequest request) {
        ReceptionRoom room = requireRoom(request.getRoomId());
        String reason = requireReason(request.getReason());
        validateRange(request.getStartTime(), request.getEndTime());

        RoomQuietPeriod period = new RoomQuietPeriod();
        period.setQuietNo(generateQuietNo());
        period.setRoomId(room.getId());
        period.setFloorId(room.getFloorId());
        period.setStartTime(request.getStartTime());
        period.setEndTime(request.getEndTime());
        period.setReason(reason);
        period.setRemark(request.getRemark());
        quietPeriodMapper.insert(period);

        return toVO(period, loadFloorMap(), loadRoomMap());
    }

    @Override
    public RoomQuietPeriodVO updateQuietPeriod(Long quietPeriodId, RoomQuietPeriodRequest request) {
        RoomQuietPeriod period = requireQuietPeriod(quietPeriodId);
        ReceptionRoom room = requireRoom(request.getRoomId());
        String reason = requireReason(request.getReason());
        validateRange(request.getStartTime(), request.getEndTime());

        period.setRoomId(room.getId());
        period.setFloorId(room.getFloorId());
        period.setStartTime(request.getStartTime());
        period.setEndTime(request.getEndTime());
        period.setReason(reason);
        period.setRemark(request.getRemark());
        quietPeriodMapper.updateById(period);

        return toVO(period, loadFloorMap(), loadRoomMap());
    }

    @Override
    public void deleteQuietPeriod(Long quietPeriodId) {
        RoomQuietPeriod period = requireQuietPeriod(quietPeriodId);
        quietPeriodMapper.deleteById(period.getId());
    }

    @Override
    public IPage<RoomQuietPeriodVO> getQuietPeriodsPage(int pageNum, int pageSize, Long floorId, Long roomId) {
        Page<RoomQuietPeriod> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RoomQuietPeriod> wrapper = new LambdaQueryWrapper<>();
        if (floorId != null) {
            wrapper.eq(RoomQuietPeriod::getFloorId, floorId);
        }
        if (roomId != null) {
            wrapper.eq(RoomQuietPeriod::getRoomId, roomId);
        }
        wrapper.orderByDesc(RoomQuietPeriod::getStartTime).orderByDesc(RoomQuietPeriod::getId);
        IPage<RoomQuietPeriod> periodPage = quietPeriodMapper.selectPage(page, wrapper);

        Map<Long, String> floorMap = loadFloorMap();
        Map<Long, String> roomMap = loadRoomMap();
        return periodPage.convert(p -> toVO(p, floorMap, roomMap));
    }

    @Override
    public List<RoomQuietPeriodVO> getRoomQuietPeriods(Long roomId) {
        LambdaQueryWrapper<RoomQuietPeriod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomQuietPeriod::getRoomId, roomId);
        wrapper.orderByAsc(RoomQuietPeriod::getStartTime).orderByAsc(RoomQuietPeriod::getId);
        Map<Long, String> floorMap = loadFloorMap();
        Map<Long, String> roomMap = loadRoomMap();
        return quietPeriodMapper.selectList(wrapper).stream()
                .map(p -> toVO(p, floorMap, roomMap))
                .collect(Collectors.toList());
    }

    @Override
    public List<RoomQuietPeriod> findOverlapping(Long roomId, LocalDateTime start, LocalDateTime end) {
        if (roomId == null || start == null || end == null) {
            return List.of();
        }
        // 边界相接不算重叠：start < quiet.end 且 quiet.start < end
        LambdaQueryWrapper<RoomQuietPeriod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomQuietPeriod::getRoomId, roomId);
        wrapper.lt(RoomQuietPeriod::getStartTime, end);
        wrapper.gt(RoomQuietPeriod::getEndTime, start);
        wrapper.orderByAsc(RoomQuietPeriod::getStartTime).orderByAsc(RoomQuietPeriod::getId);
        return quietPeriodMapper.selectList(wrapper);
    }

    // ---------------- 私有辅助方法 ----------------

    private RoomQuietPeriod requireQuietPeriod(Long quietPeriodId) {
        RoomQuietPeriod period = quietPeriodId == null ? null : quietPeriodMapper.selectById(quietPeriodId);
        if (period == null) {
            throw new IllegalArgumentException("静音时段不存在或已删除");
        }
        return period;
    }

    private ReceptionRoom requireRoom(Long roomId) {
        if (roomId == null) {
            throw new IllegalArgumentException("请选择接待室");
        }
        ReceptionRoom room = roomService.getRoomById(roomId);
        if (room == null) {
            throw new IllegalArgumentException("所选接待室不存在");
        }
        return room;
    }

    private String requireReason(String reason) {
        String trimmed = reason == null ? null : reason.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            throw new IllegalArgumentException("静音原因不能为空");
        }
        return trimmed;
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("请选择静音起止时间");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("静音结束时间必须晚于开始时间");
        }
    }

    private Map<Long, String> loadFloorMap() {
        return floorService.getAllFloors().stream()
                .collect(Collectors.toMap(Floor::getId, Floor::getFloorName, (a, b) -> a));
    }

    private Map<Long, String> loadRoomMap() {
        return roomService.getAllRooms().stream()
                .collect(Collectors.toMap(ReceptionRoom::getId, ReceptionRoom::getRoomName, (a, b) -> a));
    }

    private RoomQuietPeriodVO toVO(RoomQuietPeriod period, Map<Long, String> floorMap,
                                   Map<Long, String> roomMap) {
        RoomQuietPeriodVO vo = new RoomQuietPeriodVO();
        vo.setId(period.getId());
        vo.setQuietNo(period.getQuietNo());
        vo.setRoomId(period.getRoomId());
        vo.setRoomName(roomMap.get(period.getRoomId()));
        ReceptionRoom room = period.getRoomId() != null ? roomService.getRoomById(period.getRoomId()) : null;
        vo.setRoomCode(room != null ? room.getRoomCode() : null);
        vo.setFloorId(period.getFloorId());
        vo.setFloorName(floorMap.get(period.getFloorId()));
        vo.setStartTime(period.getStartTime());
        vo.setEndTime(period.getEndTime());
        vo.setReason(period.getReason());
        vo.setRemark(period.getRemark());
        vo.setCreatedAt(period.getCreatedAt());
        vo.setUpdatedAt(period.getUpdatedAt());
        return vo;
    }

    private String generateQuietNo() {
        return "JY" + LocalDateTime.now().format(NO_FMT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
