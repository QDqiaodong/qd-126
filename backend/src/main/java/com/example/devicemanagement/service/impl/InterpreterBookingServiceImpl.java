package com.example.devicemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.InterpreterBookingRequest;
import com.example.devicemanagement.dto.response.InterpreterBookingVO;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.InterpreterBooking;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.exception.InterpreterConflictException;
import com.example.devicemanagement.mapper.InterpreterBookingMapper;
import com.example.devicemanagement.service.FloorService;
import com.example.devicemanagement.service.InterpreterBookingService;
import com.example.devicemanagement.service.ReceptionRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class InterpreterBookingServiceImpl implements InterpreterBookingService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Autowired
    private InterpreterBookingMapper bookingMapper;

    @Autowired
    private FloorService floorService;

    @Autowired
    private ReceptionRoomService roomService;

    @Override
    public InterpreterBookingVO createBooking(InterpreterBookingRequest request) {
        LocalDateTime now = LocalDateTime.now();
        refreshExpired(now);

        ValidatedBooking valid = validateBooking(request, null);

        InterpreterBooking booking = new InterpreterBooking();
        booking.setBookingNo(generateBookingNo());
        booking.setRoomId(valid.room.getId());
        booking.setFloorId(valid.room.getFloorId());
        booking.setLanguage(valid.language);
        booking.setInterpreterName(valid.interpreterName);
        booking.setStartTime(valid.start);
        booking.setEndTime(valid.end);
        // 登记当前时刻已在时段内的预约直接为进行中（登记后即只读）
        booking.setStatus(now.isBefore(valid.start)
                ? InterpreterBooking.STATUS_PENDING : InterpreterBooking.STATUS_ONGOING);
        booking.setRemark(valid.remark);
        bookingMapper.insert(booking);

        return toVO(booking, loadFloorMap(), loadRoomMap());
    }

    @Override
    public InterpreterBookingVO updateBooking(Long bookingId, InterpreterBookingRequest request) {
        LocalDateTime now = LocalDateTime.now();
        refreshExpired(now);

        InterpreterBooking booking = requireBooking(bookingId);
        // 活动已开始后只允许查看预约，不能改译员（语种/时段等一并只读）
        if (!Integer.valueOf(InterpreterBooking.STATUS_PENDING).equals(booking.getStatus())) {
            throw new IllegalArgumentException("活动已开始，翻译预约只允许查看，不能修改译员");
        }

        ValidatedBooking valid = validateBooking(request, bookingId);

        booking.setRoomId(valid.room.getId());
        booking.setFloorId(valid.room.getFloorId());
        booking.setLanguage(valid.language);
        booking.setInterpreterName(valid.interpreterName);
        booking.setStartTime(valid.start);
        booking.setEndTime(valid.end);
        booking.setRemark(valid.remark);
        bookingMapper.updateById(booking);

        return toVO(booking, loadFloorMap(), loadRoomMap());
    }

    @Override
    public void deleteBooking(Long bookingId) {
        InterpreterBooking booking = requireBooking(bookingId);
        refreshOne(booking, LocalDateTime.now());
        if (!Integer.valueOf(InterpreterBooking.STATUS_PENDING).equals(booking.getStatus())) {
            throw new IllegalArgumentException("活动已开始，翻译预约只允许查看，不能删除");
        }
        bookingMapper.deleteById(booking.getId());
    }

    @Override
    public IPage<InterpreterBookingVO> getBookingsPage(int pageNum, int pageSize, Long floorId, Long roomId) {
        refreshExpired(LocalDateTime.now());

        Page<InterpreterBooking> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<InterpreterBooking> wrapper = new LambdaQueryWrapper<>();
        if (floorId != null) {
            wrapper.eq(InterpreterBooking::getFloorId, floorId);
        }
        if (roomId != null) {
            wrapper.eq(InterpreterBooking::getRoomId, roomId);
        }
        wrapper.orderByDesc(InterpreterBooking::getStartTime).orderByDesc(InterpreterBooking::getId);
        IPage<InterpreterBooking> bookingPage = bookingMapper.selectPage(page, wrapper);

        Map<Long, String> floorMap = loadFloorMap();
        Map<Long, String> roomMap = loadRoomMap();
        return bookingPage.convert(b -> toVO(b, floorMap, roomMap));
    }

    @Override
    public InterpreterBookingVO getBookingById(Long bookingId) {
        InterpreterBooking booking = requireBooking(bookingId);
        refreshOne(booking, LocalDateTime.now());
        return toVO(booking, loadFloorMap(), loadRoomMap());
    }

    @Override
    public List<InterpreterBookingVO> getRoomBookings(Long roomId) {
        LambdaQueryWrapper<InterpreterBooking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterpreterBooking::getRoomId, roomId);
        wrapper.orderByAsc(InterpreterBooking::getStartTime).orderByAsc(InterpreterBooking::getId);
        Map<Long, String> floorMap = loadFloorMap();
        Map<Long, String> roomMap = loadRoomMap();
        return bookingMapper.selectList(wrapper).stream()
                .map(b -> toVO(b, floorMap, roomMap))
                .collect(Collectors.toList());
    }

    // ---------------- 私有辅助方法 ----------------

    /**
     * 新建/修改共用校验：基础字段、同一接待室时段重叠、同一译员时段撞车。
     * excludeBookingId 用于修改时排除自身。
     */
    private ValidatedBooking validateBooking(InterpreterBookingRequest request, Long excludeBookingId) {
        if (request.getRoomId() == null) {
            throw new IllegalArgumentException("请选择接待室");
        }
        ReceptionRoom room = roomService.getRoomById(request.getRoomId());
        if (room == null) {
            throw new IllegalArgumentException("所选接待室不存在");
        }
        if (!Integer.valueOf(1).equals(room.getStatus())) {
            throw new IllegalArgumentException("所选接待室已停用，无法预约翻译");
        }

        String language = request.getLanguage() == null ? null : request.getLanguage().trim();
        if (language == null || language.isEmpty()) {
            throw new IllegalArgumentException("请选择翻译语种");
        }

        String interpreterName = request.getInterpreterName() == null
                ? null : request.getInterpreterName().trim();
        if (interpreterName == null || interpreterName.isEmpty()) {
            throw new IllegalArgumentException("请填写随行译员");
        }

        LocalDateTime start = request.getStartTime();
        LocalDateTime end = request.getEndTime();
        if (start == null || end == null) {
            throw new IllegalArgumentException("请选择预约开始与结束时间");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
        if (!end.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("结束时间已过，不能预约已结束的时段");
        }

        // 同一接待室时段重叠：该室已有翻译预约，不能重复预约
        List<InterpreterBooking> roomConflicts = findRoomConflicts(room.getId(), start, end, excludeBookingId);
        if (!roomConflicts.isEmpty()) {
            InterpreterBooking other = roomConflicts.get(0);
            throw new IllegalArgumentException("接待室「" + room.getRoomName()
                    + "」在该时段已有翻译预约（译员：" + other.getInterpreterName()
                    + "，" + other.getStartTime().format(TIME_FMT) + " ~ "
                    + other.getEndTime().format(TIME_FMT) + "）");
        }

        // 同一译员时段撞车必须拦住，并弹出已约接待室（边界相接不算重叠）
        assertInterpreterAvailable(interpreterName, start, end, excludeBookingId);

        ValidatedBooking valid = new ValidatedBooking();
        valid.room = room;
        valid.language = language;
        valid.interpreterName = interpreterName;
        valid.start = start;
        valid.end = end;
        valid.remark = request.getRemark();
        return valid;
    }

    /**
     * 同一译员时段撞车：译员在重叠时段已被其他接待室预约，
     * 抛出携带已约接待室与时段的业务异常，前端弹出提示并拦住提交。
     */
    private void assertInterpreterAvailable(String interpreterName, LocalDateTime start,
                                            LocalDateTime end, Long excludeBookingId) {
        List<InterpreterBooking> conflicts = findInterpreterConflicts(
                interpreterName, start, end, excludeBookingId);
        if (conflicts.isEmpty()) {
            return;
        }
        Map<Long, String> roomMap = loadRoomMap();
        StringBuilder sb = new StringBuilder("译员「").append(interpreterName)
                .append("」时段撞车，同一时段已被以下接待室预约：");
        for (int i = 0; i < conflicts.size(); i++) {
            InterpreterBooking other = conflicts.get(i);
            if (i > 0) {
                sb.append("；");
            }
            sb.append(roomMap.getOrDefault(other.getRoomId(), String.valueOf(other.getRoomId())))
                    .append("（").append(other.getStartTime().format(TIME_FMT))
                    .append(" ~ ").append(other.getEndTime().format(TIME_FMT))
                    .append("，语种：").append(other.getLanguage()).append("）");
        }
        sb.append("，请更换译员或调整预约时段");
        throw new InterpreterConflictException(sb.toString());
    }

    /** 校验通过的预约数据（新建/修改共用） */
    private static class ValidatedBooking {
        private ReceptionRoom room;
        private String language;
        private String interpreterName;
        private LocalDateTime start;
        private LocalDateTime end;
        private String remark;
    }

    private InterpreterBooking requireBooking(Long bookingId) {
        InterpreterBooking booking = bookingId == null ? null : bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("翻译预约不存在或已删除");
        }
        return booking;
    }

    /**
     * 按当前时间推进单个预约状态：到点开始、到期结束。
     */
    private InterpreterBooking refreshOne(InterpreterBooking booking, LocalDateTime now) {
        if (booking == null || Integer.valueOf(InterpreterBooking.STATUS_ENDED).equals(booking.getStatus())) {
            return booking;
        }
        boolean changed = false;
        if (!now.isBefore(booking.getEndTime())) {
            booking.setStatus(InterpreterBooking.STATUS_ENDED);
            changed = true;
        } else if (!now.isBefore(booking.getStartTime())
                && Integer.valueOf(InterpreterBooking.STATUS_PENDING).equals(booking.getStatus())) {
            booking.setStatus(InterpreterBooking.STATUS_ONGOING);
            changed = true;
        }
        if (changed) {
            bookingMapper.updateById(booking);
        }
        return booking;
    }

    /**
     * 批量推进所有未结束预约：刷新后列表状态与筛选保持一致。
     */
    private void refreshExpired(LocalDateTime now) {
        LambdaQueryWrapper<InterpreterBooking> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(InterpreterBooking::getStatus, InterpreterBooking.STATUS_ENDED);
        for (InterpreterBooking booking : bookingMapper.selectList(wrapper)) {
            refreshOne(booking, now);
        }
    }

    /**
     * 同一接待室时段重叠的其他预约（边界相接不算重叠：start &lt; other.end 且 other.start &lt; end）。
     */
    private List<InterpreterBooking> findRoomConflicts(Long roomId, LocalDateTime start,
                                                       LocalDateTime end, Long excludeId) {
        LambdaQueryWrapper<InterpreterBooking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterpreterBooking::getRoomId, roomId);
        wrapper.ne(InterpreterBooking::getStatus, InterpreterBooking.STATUS_ENDED);
        wrapper.lt(InterpreterBooking::getStartTime, end);
        wrapper.gt(InterpreterBooking::getEndTime, start);
        if (excludeId != null) {
            wrapper.ne(InterpreterBooking::getId, excludeId);
        }
        wrapper.orderByAsc(InterpreterBooking::getStartTime);
        return bookingMapper.selectList(wrapper);
    }

    /**
     * 同一译员（按姓名匹配）时段重叠的其他接待室预约；同名译员同一时段只能在一间接待室。
     */
    private List<InterpreterBooking> findInterpreterConflicts(String interpreterName,
                                                              LocalDateTime start, LocalDateTime end,
                                                              Long excludeId) {
        LambdaQueryWrapper<InterpreterBooking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterpreterBooking::getInterpreterName, interpreterName);
        wrapper.ne(InterpreterBooking::getStatus, InterpreterBooking.STATUS_ENDED);
        wrapper.lt(InterpreterBooking::getStartTime, end);
        wrapper.gt(InterpreterBooking::getEndTime, start);
        if (excludeId != null) {
            wrapper.ne(InterpreterBooking::getId, excludeId);
        }
        wrapper.orderByAsc(InterpreterBooking::getStartTime).orderByAsc(InterpreterBooking::getId);
        return bookingMapper.selectList(wrapper);
    }

    private Map<Long, String> loadFloorMap() {
        return floorService.getAllFloors().stream()
                .collect(Collectors.toMap(Floor::getId, Floor::getFloorName, (a, b) -> a));
    }

    private Map<Long, String> loadRoomMap() {
        return roomService.getAllRooms().stream()
                .collect(Collectors.toMap(ReceptionRoom::getId, ReceptionRoom::getRoomName, (a, b) -> a));
    }

    private InterpreterBookingVO toVO(InterpreterBooking booking, Map<Long, String> floorMap,
                                      Map<Long, String> roomMap) {
        InterpreterBookingVO vo = new InterpreterBookingVO();
        vo.setId(booking.getId());
        vo.setBookingNo(booking.getBookingNo());
        vo.setRoomId(booking.getRoomId());
        vo.setRoomName(roomMap.get(booking.getRoomId()));
        ReceptionRoom room = booking.getRoomId() != null ? roomService.getRoomById(booking.getRoomId()) : null;
        vo.setRoomCode(room != null ? room.getRoomCode() : null);
        vo.setFloorId(booking.getFloorId());
        vo.setFloorName(floorMap.get(booking.getFloorId()));
        vo.setLanguage(booking.getLanguage());
        vo.setInterpreterName(booking.getInterpreterName());
        vo.setStartTime(booking.getStartTime());
        vo.setEndTime(booking.getEndTime());
        vo.setStatus(booking.getStatus());
        vo.setStatusText(statusText(booking.getStatus()));
        vo.setRemark(booking.getRemark());
        vo.setCreatedAt(booking.getCreatedAt());
        vo.setUpdatedAt(booking.getUpdatedAt());
        return vo;
    }

    private String generateBookingNo() {
        return "FY" + LocalDateTime.now().format(NO_FMT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String statusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case InterpreterBooking.STATUS_PENDING: return "待开始";
            case InterpreterBooking.STATUS_ONGOING: return "进行中";
            case InterpreterBooking.STATUS_ENDED: return "已结束";
            default: return "未知";
        }
    }
}
