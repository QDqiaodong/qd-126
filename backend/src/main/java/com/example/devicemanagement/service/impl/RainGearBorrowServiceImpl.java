package com.example.devicemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.RainGearBorrowRequest;
import com.example.devicemanagement.dto.request.RainGearReturnRequest;
import com.example.devicemanagement.dto.response.RainGearBorrowStatsVO;
import com.example.devicemanagement.dto.response.RainGearBorrowVO;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.RainGearBorrow;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.mapper.RainGearBorrowMapper;
import com.example.devicemanagement.service.FloorService;
import com.example.devicemanagement.service.RainGearBorrowService;
import com.example.devicemanagement.service.ReceptionRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class RainGearBorrowServiceImpl implements RainGearBorrowService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private RainGearBorrowMapper borrowMapper;

    @Autowired
    private FloorService floorService;

    @Autowired
    private ReceptionRoomService roomService;

    @Override
    public RainGearBorrowVO createBorrow(RainGearBorrowRequest request) {
        LocalDateTime now = LocalDateTime.now();

        if (request.getRoomId() == null) {
            throw new IllegalArgumentException("请选择接待室");
        }
        ReceptionRoom room = roomService.getRoomById(request.getRoomId());
        if (room == null) {
            throw new IllegalArgumentException("所选接待室不存在");
        }
        if (!Integer.valueOf(1).equals(room.getStatus())) {
            throw new IllegalArgumentException("所选接待室已停用，无法登记雨具借用");
        }

        String gearType = normalizeGearType(request.getGearType());
        String borrower = request.getBorrower() == null ? null : request.getBorrower().trim();
        if (borrower == null || borrower.isEmpty()) {
            throw new IllegalArgumentException("请填写借出人");
        }
        LocalDateTime expectedReturn = request.getExpectedReturnTime();
        if (expectedReturn == null) {
            throw new IllegalArgumentException("请选择预计归还时间");
        }
        if (!expectedReturn.isAfter(now)) {
            throw new IllegalArgumentException("预计归还时间必须晚于当前时间");
        }

        RainGearBorrow borrow = new RainGearBorrow();
        borrow.setBorrowNo(generateBorrowNo());
        borrow.setRoomId(room.getId());
        borrow.setFloorId(room.getFloorId());
        borrow.setGearType(gearType);
        borrow.setBorrower(borrower);
        borrow.setExpectedReturnTime(expectedReturn);
        borrow.setStatus(RainGearBorrow.STATUS_BORROWED);
        borrow.setRemark(request.getRemark());
        borrowMapper.insert(borrow);

        return toVO(borrow, loadFloorMap(), loadRoomMap(), now);
    }

    @Override
    public RainGearBorrowVO returnBorrow(Long borrowId, RainGearReturnRequest request) {
        LocalDateTime now = LocalDateTime.now();
        RainGearBorrow borrow = requireBorrow(borrowId);
        if (Integer.valueOf(RainGearBorrow.STATUS_RETURNED).equals(borrow.getStatus())) {
            throw new IllegalArgumentException("该雨具已归还核销，无需重复核销");
        }

        borrow.setStatus(RainGearBorrow.STATUS_RETURNED);
        borrow.setReturnedAt(now);
        borrow.setReturnedBy(request != null && request.getReturnedBy() != null
                && !request.getReturnedBy().trim().isEmpty()
                ? request.getReturnedBy().trim() : null);
        borrowMapper.updateById(borrow);

        return toVO(borrow, loadFloorMap(), loadRoomMap(), now);
    }

    @Override
    public IPage<RainGearBorrowVO> getBorrowsPage(int pageNum, int pageSize, Long floorId, Long roomId,
                                                  String gearType, Integer status, Boolean overdue) {
        LocalDateTime now = LocalDateTime.now();

        Page<RainGearBorrow> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RainGearBorrow> wrapper = new LambdaQueryWrapper<>();
        if (floorId != null) {
            wrapper.eq(RainGearBorrow::getFloorId, floorId);
        }
        if (roomId != null) {
            wrapper.eq(RainGearBorrow::getRoomId, roomId);
        }
        String normalizedGearType = normalizeGearType(gearType);
        if (normalizedGearType != null) {
            wrapper.eq(RainGearBorrow::getGearType, normalizedGearType);
        }
        if (status != null) {
            wrapper.eq(RainGearBorrow::getStatus, status);
        }
        if (Boolean.TRUE.equals(overdue)) {
            // 逾期 = 在借且预计归还时间已到（逾期标记按当前时间派生，直接下推到SQL）
            wrapper.eq(RainGearBorrow::getStatus, RainGearBorrow.STATUS_BORROWED);
            wrapper.le(RainGearBorrow::getExpectedReturnTime, now);
        } else if (Boolean.FALSE.equals(overdue)) {
            // 未逾期：已归还，或在借但尚未到预计归还时间
            wrapper.and(w -> w.eq(RainGearBorrow::getStatus, RainGearBorrow.STATUS_RETURNED)
                    .or(o -> o.eq(RainGearBorrow::getStatus, RainGearBorrow.STATUS_BORROWED)
                            .gt(RainGearBorrow::getExpectedReturnTime, now)));
        }
        wrapper.orderByDesc(RainGearBorrow::getCreatedAt).orderByDesc(RainGearBorrow::getId);
        IPage<RainGearBorrow> borrowPage = borrowMapper.selectPage(page, wrapper);

        Map<Long, String> floorMap = loadFloorMap();
        Map<Long, String> roomMap = loadRoomMap();
        return borrowPage.convert(b -> toVO(b, floorMap, roomMap, now));
    }

    @Override
    public RainGearBorrowVO getBorrowById(Long borrowId) {
        RainGearBorrow borrow = requireBorrow(borrowId);
        return toVO(borrow, loadFloorMap(), loadRoomMap(), LocalDateTime.now());
    }

    @Override
    public RainGearBorrowStatsVO getStats(Long floorId, Long roomId) {
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<RainGearBorrow> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(RainGearBorrow::getStatus, RainGearBorrow.STATUS_BORROWED);
        if (floorId != null) {
            activeWrapper.eq(RainGearBorrow::getFloorId, floorId);
        }
        if (roomId != null) {
            activeWrapper.eq(RainGearBorrow::getRoomId, roomId);
        }
        long activeCount = borrowMapper.selectCount(activeWrapper);

        LambdaQueryWrapper<RainGearBorrow> overdueWrapper = new LambdaQueryWrapper<>();
        overdueWrapper.eq(RainGearBorrow::getStatus, RainGearBorrow.STATUS_BORROWED);
        overdueWrapper.le(RainGearBorrow::getExpectedReturnTime, now);
        if (floorId != null) {
            overdueWrapper.eq(RainGearBorrow::getFloorId, floorId);
        }
        if (roomId != null) {
            overdueWrapper.eq(RainGearBorrow::getRoomId, roomId);
        }
        long overdueCount = borrowMapper.selectCount(overdueWrapper);

        RainGearBorrowStatsVO stats = new RainGearBorrowStatsVO();
        stats.setFloorId(floorId);
        stats.setRoomId(roomId);
        stats.setActiveCount(activeCount);
        stats.setOverdueCount(overdueCount);
        if (floorId != null) {
            stats.setFloorName(loadFloorMap().get(floorId));
        }
        if (roomId != null) {
            stats.setRoomName(loadRoomMap().get(roomId));
        }
        return stats;
    }

    // ---------------- 私有辅助方法 ----------------

    private RainGearBorrow requireBorrow(Long borrowId) {
        RainGearBorrow borrow = borrowId == null ? null : borrowMapper.selectById(borrowId);
        if (borrow == null) {
            throw new IllegalArgumentException("雨具借用记录不存在或已删除");
        }
        return borrow;
    }

    /** 校验收紧雨具类型，只允许雨伞/雨衣；空值返回 null（不参与筛选）。 */
    private String normalizeGearType(String gearType) {
        if (gearType == null) {
            return null;
        }
        String value = gearType.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (RainGearBorrow.GEAR_UMBRELLA.equals(value) || RainGearBorrow.GEAR_RAINCOAT.equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("雨具类型只支持雨伞或雨衣");
    }

    private Map<Long, String> loadFloorMap() {
        return floorService.getAllFloors().stream()
                .collect(Collectors.toMap(Floor::getId, Floor::getFloorName, (a, b) -> a));
    }

    private Map<Long, String> loadRoomMap() {
        return roomService.getAllRooms().stream()
                .collect(Collectors.toMap(ReceptionRoom::getId, ReceptionRoom::getRoomName, (a, b) -> a));
    }

    private RainGearBorrowVO toVO(RainGearBorrow borrow, Map<Long, String> floorMap,
                                  Map<Long, String> roomMap, LocalDateTime now) {
        RainGearBorrowVO vo = new RainGearBorrowVO();
        vo.setId(borrow.getId());
        vo.setBorrowNo(borrow.getBorrowNo());
        vo.setRoomId(borrow.getRoomId());
        vo.setRoomName(roomMap.get(borrow.getRoomId()));
        ReceptionRoom room = borrow.getRoomId() != null ? roomService.getRoomById(borrow.getRoomId()) : null;
        vo.setRoomCode(room != null ? room.getRoomCode() : null);
        vo.setFloorId(borrow.getFloorId());
        vo.setFloorName(floorMap.get(borrow.getFloorId()));
        vo.setGearType(borrow.getGearType());
        vo.setGearTypeText(gearTypeText(borrow.getGearType()));
        vo.setBorrower(borrow.getBorrower());
        vo.setExpectedReturnTime(borrow.getExpectedReturnTime());
        vo.setStatus(borrow.getStatus());
        vo.setStatusText(statusText(borrow.getStatus()));
        // 逾期只按当前时间实时派生：在借且已过预计归还时间，刷新后自动保持一致
        boolean overdue = Integer.valueOf(RainGearBorrow.STATUS_BORROWED).equals(borrow.getStatus())
                && !now.isBefore(borrow.getExpectedReturnTime());
        vo.setOverdue(overdue);
        vo.setReturnedAt(borrow.getReturnedAt());
        vo.setReturnedBy(borrow.getReturnedBy());
        vo.setRemark(borrow.getRemark());
        vo.setCreatedAt(borrow.getCreatedAt());
        vo.setUpdatedAt(borrow.getUpdatedAt());
        return vo;
    }

    private String generateBorrowNo() {
        return "YJ" + LocalDateTime.now().format(NO_FMT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String gearTypeText(String gearType) {
        if (RainGearBorrow.GEAR_UMBRELLA.equals(gearType)) {
            return "雨伞";
        }
        if (RainGearBorrow.GEAR_RAINCOAT.equals(gearType)) {
            return "雨衣";
        }
        return "未知";
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case RainGearBorrow.STATUS_BORROWED:
                return "在借";
            case RainGearBorrow.STATUS_RETURNED:
                return "已归还";
            default:
                return "未知";
        }
    }
}
