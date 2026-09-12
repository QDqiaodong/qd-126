package com.example.devicemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.WelcomeBoardCreateRequest;
import com.example.devicemanagement.dto.request.WelcomeBoardRemoveRequest;
import com.example.devicemanagement.dto.response.BoardRoomAvailabilityVO;
import com.example.devicemanagement.dto.response.WelcomeBoardVO;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.entity.WelcomeBoard;
import com.example.devicemanagement.mapper.WelcomeBoardMapper;
import com.example.devicemanagement.service.FloorService;
import com.example.devicemanagement.service.ReceptionRoomService;
import com.example.devicemanagement.service.WelcomeBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WelcomeBoardServiceImpl implements WelcomeBoardService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Autowired
    private WelcomeBoardMapper boardMapper;

    @Autowired
    private FloorService floorService;

    @Autowired
    private ReceptionRoomService roomService;

    @Override
    @Transactional
    public WelcomeBoardVO createBoard(WelcomeBoardCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        // 先把到点的排期推进为已上墙，避免历史状态导致时段误判
        refreshExpired(now);

        String text = request.getBoardText() == null ? null : request.getBoardText().trim();
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("欢迎牌文案不能为空");
        }
        if (request.getRoomId() == null) {
            throw new IllegalArgumentException("请选择接待室");
        }
        ReceptionRoom room = roomService.getRoomById(request.getRoomId());
        if (room == null) {
            throw new IllegalArgumentException("所选接待室不存在");
        }
        if (!Integer.valueOf(1).equals(room.getStatus())) {
            throw new IllegalArgumentException("所选接待室已停用，无法登记欢迎牌");
        }
        LocalDateTime mountTime = request.getMountTime();
        LocalDateTime plannedRemoveTime = request.getPlannedRemoveTime();
        if (mountTime == null || plannedRemoveTime == null) {
            throw new IllegalArgumentException("请选择上墙时间与撤下时间");
        }
        if (!plannedRemoveTime.isAfter(mountTime)) {
            throw new IllegalArgumentException("撤下时间必须晚于上墙时间");
        }
        if (!plannedRemoveTime.isAfter(now)) {
            throw new IllegalArgumentException("撤下时间已过，不能登记已到期的欢迎牌");
        }
        String registrar = request.getRegistrar() == null ? null : request.getRegistrar().trim();
        if (registrar == null || registrar.isEmpty()) {
            throw new IllegalArgumentException("登记人不能为空");
        }

        // 同一接待室时段重叠必须拦住（已撤下的不再占位；边界相接不算重叠）
        List<WelcomeBoard> conflicts = findConflictingBoards(room.getId(), mountTime, plannedRemoveTime, now, null);
        if (!conflicts.isEmpty()) {
            WelcomeBoard other = conflicts.get(0);
            boolean stillUp = Integer.valueOf(WelcomeBoard.STATUS_MOUNTED).equals(other.getStatus())
                    && !now.isBefore(other.getPlannedRemoveTime());
            throw new IllegalArgumentException("同一接待室欢迎牌时段冲突：「" + other.getBoardText()
                    + "」排期 " + other.getMountTime().format(TIME_FMT) + " ~ "
                    + other.getPlannedRemoveTime().format(TIME_FMT)
                    + (stillUp ? "已到期但尚未撤下，请先登记撤下回执" : "与所选时段重叠"));
        }

        WelcomeBoard board = new WelcomeBoard();
        board.setBoardNo(generateBoardNo());
        board.setBoardText(text);
        board.setRoomId(room.getId());
        board.setFloorId(room.getFloorId());
        board.setMountTime(mountTime);
        board.setPlannedRemoveTime(plannedRemoveTime);
        board.setRegistrar(registrar);
        // 登记时已到上墙时间的直接按已上墙处理（上墙时间不允许早于当前？按活动惯例允许立即上墙）
        board.setStatus(!now.isBefore(mountTime)
                ? WelcomeBoard.STATUS_MOUNTED : WelcomeBoard.STATUS_SCHEDULED);
        if (Integer.valueOf(WelcomeBoard.STATUS_MOUNTED).equals(board.getStatus())) {
            board.setMountedAt(mountTime);
        }
        board.setRemark(request.getRemark());
        boardMapper.insert(board);

        return toVO(board, loadFloorMap(), loadRoomMap(), now);
    }

    @Override
    public IPage<WelcomeBoardVO> getBoardsPage(int pageNum, int pageSize,
                                               Long floorId, Long roomId,
                                               Integer status, Boolean overdueOnly) {
        LocalDateTime now = LocalDateTime.now();
        refreshExpired(now);

        Page<WelcomeBoard> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WelcomeBoard> wrapper = new LambdaQueryWrapper<>();
        if (floorId != null) {
            wrapper.eq(WelcomeBoard::getFloorId, floorId);
        }
        if (roomId != null) {
            wrapper.eq(WelcomeBoard::getRoomId, roomId);
        }
        if (status != null) {
            wrapper.eq(WelcomeBoard::getStatus, status);
        }
        if (Boolean.TRUE.equals(overdueOnly)) {
            // 待撤 = 已上墙且计划撤下时间已到（状态已在刷新时落库，可直接下推到SQL）
            wrapper.eq(WelcomeBoard::getStatus, WelcomeBoard.STATUS_MOUNTED);
            wrapper.le(WelcomeBoard::getPlannedRemoveTime, now);
        }
        wrapper.orderByDesc(WelcomeBoard::getMountTime).orderByDesc(WelcomeBoard::getId);
        IPage<WelcomeBoard> boardPage = boardMapper.selectPage(page, wrapper);

        Map<Long, String> floorMap = loadFloorMap();
        Map<Long, String> roomMap = loadRoomMap();
        return boardPage.convert(b -> toVO(b, floorMap, roomMap, now));
    }

    @Override
    public WelcomeBoardVO getBoardById(Long boardId) {
        WelcomeBoard board = requireBoard(boardId);
        LocalDateTime now = LocalDateTime.now();
        refreshOne(board, now);
        return toVO(board, loadFloorMap(), loadRoomMap(), now);
    }

    @Override
    public List<WelcomeBoardVO> getOverdueBoards() {
        LocalDateTime now = LocalDateTime.now();
        refreshExpired(now);

        LambdaQueryWrapper<WelcomeBoard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WelcomeBoard::getStatus, WelcomeBoard.STATUS_MOUNTED);
        wrapper.le(WelcomeBoard::getPlannedRemoveTime, now);
        wrapper.orderByAsc(WelcomeBoard::getPlannedRemoveTime).orderByAsc(WelcomeBoard::getId);
        List<WelcomeBoard> boards = boardMapper.selectList(wrapper);

        Map<Long, String> floorMap = loadFloorMap();
        Map<Long, String> roomMap = loadRoomMap();
        return boards.stream().map(b -> toVO(b, floorMap, roomMap, now)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WelcomeBoardVO removeBoard(Long boardId, WelcomeBoardRemoveRequest request) {
        LocalDateTime now = LocalDateTime.now();
        WelcomeBoard board = requireBoard(boardId);
        refreshOne(board, now);

        if (Integer.valueOf(WelcomeBoard.STATUS_REMOVED).equals(board.getStatus())) {
            throw new IllegalArgumentException("该欢迎牌已撤下，无需重复登记回执");
        }
        if (Integer.valueOf(WelcomeBoard.STATUS_SCHEDULED).equals(board.getStatus())) {
            throw new IllegalArgumentException("欢迎牌尚未到上墙时间，不能撤下");
        }
        String receipt = request == null || request.getReceipt() == null ? null : request.getReceipt().trim();
        if (receipt == null || receipt.isEmpty()) {
            // 撤下必须写回执才能拿掉标记
            throw new IllegalArgumentException("请填写撤下回执后再撤下欢迎牌");
        }

        board.setStatus(WelcomeBoard.STATUS_REMOVED);
        board.setRemovedAt(now);
        board.setRemoveReceipt(receipt);
        board.setRemovedBy(request.getOperator() == null ? null : request.getOperator().trim());
        boardMapper.updateById(board);

        return toVO(board, loadFloorMap(), loadRoomMap(), now);
    }

    @Override
    public List<BoardRoomAvailabilityVO> getRoomAvailability(Long floorId) {
        LocalDateTime now = LocalDateTime.now();
        refreshExpired(now);

        List<ReceptionRoom> rooms = roomService.getAllRooms().stream()
                .filter(r -> floorId == null || floorId.equals(r.getFloorId()))
                .collect(Collectors.toList());
        Map<Long, String> floorMap = loadFloorMap();

        // 当前在墙上（含待撤）的欢迎牌；已撤下/待上墙都不影响当前可接待
        LambdaQueryWrapper<WelcomeBoard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WelcomeBoard::getStatus, WelcomeBoard.STATUS_MOUNTED);
        Map<Long, WelcomeBoard> mountedMap = boardMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(WelcomeBoard::getRoomId, Function.identity(), (a, b) -> a));

        List<BoardRoomAvailabilityVO> result = new ArrayList<>();
        for (ReceptionRoom room : rooms) {
            BoardRoomAvailabilityVO vo = new BoardRoomAvailabilityVO();
            vo.setRoomId(room.getId());
            vo.setRoomName(room.getRoomName());
            vo.setRoomCode(room.getRoomCode());
            vo.setFloorId(room.getFloorId());
            vo.setFloorName(floorMap.get(room.getFloorId()));
            boolean enabled = Integer.valueOf(1).equals(room.getStatus());
            vo.setRoomEnabled(enabled);
            WelcomeBoard mounted = mountedMap.get(room.getId());
            vo.setAvailable(enabled && mounted == null);
            if (mounted != null) {
                vo.setBoardId(mounted.getId());
                vo.setBoardNo(mounted.getBoardNo());
                vo.setBoardText(mounted.getBoardText());
                vo.setOverdueRemove(isOverdue(mounted, now));
            }
            result.add(vo);
        }
        return result;
    }

    // ---------------- 私有辅助方法 ----------------

    private WelcomeBoard requireBoard(Long boardId) {
        WelcomeBoard board = boardMapper.selectById(boardId);
        if (board == null) {
            throw new IllegalArgumentException("欢迎牌排期不存在");
        }
        return board;
    }

    /**
     * 按当前时间推进单个排期：到上墙时间即置为已上墙。
     * 到期不会自动撤下——必须登记撤下回执，到期未撤由 overdueRemove 派生标记。
     */
    private WelcomeBoard refreshOne(WelcomeBoard board, LocalDateTime now) {
        if (board == null || !Integer.valueOf(WelcomeBoard.STATUS_SCHEDULED).equals(board.getStatus())) {
            return board;
        }
        if (!now.isBefore(board.getMountTime())) {
            board.setStatus(WelcomeBoard.STATUS_MOUNTED);
            board.setMountedAt(board.getMountTime());
            boardMapper.updateById(board);
        }
        return board;
    }

    /**
     * 批量推进所有待上墙排期，保证重启后状态、待撤标记、可接待情况一致。
     */
    private void refreshExpired(LocalDateTime now) {
        LambdaQueryWrapper<WelcomeBoard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WelcomeBoard::getStatus, WelcomeBoard.STATUS_SCHEDULED);
        for (WelcomeBoard board : boardMapper.selectList(wrapper)) {
            refreshOne(board, now);
        }
    }

    /**
     * 同一接待室时段重叠的其他未撤欢迎牌（边界相接不算重叠）。
     * 已上墙且到期待撤的牌子物理上仍在墙上，其占位结束时间视为无限远：
     * 必须先写回执撤下，才能在该接待室登记新欢迎牌。
     */
    private List<WelcomeBoard> findConflictingBoards(Long roomId, LocalDateTime start,
                                                     LocalDateTime end, LocalDateTime now,
                                                     Long excludeId) {
        LambdaQueryWrapper<WelcomeBoard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WelcomeBoard::getRoomId, roomId);
        wrapper.ne(WelcomeBoard::getStatus, WelcomeBoard.STATUS_REMOVED);
        if (excludeId != null) {
            wrapper.ne(WelcomeBoard::getId, excludeId);
        }
        List<WelcomeBoard> result = new ArrayList<>();
        for (WelcomeBoard other : boardMapper.selectList(wrapper)) {
            refreshOne(other, now);
            LocalDateTime effectiveEnd = other.getPlannedRemoveTime();
            if (Integer.valueOf(WelcomeBoard.STATUS_MOUNTED).equals(other.getStatus())
                    && !now.isBefore(other.getPlannedRemoveTime())) {
                effectiveEnd = LocalDateTime.MAX;
            }
            if (start.isBefore(effectiveEnd) && other.getMountTime().isBefore(end)) {
                result.add(other);
            }
        }
        return result;
    }

    private boolean isOverdue(WelcomeBoard board, LocalDateTime now) {
        return Integer.valueOf(WelcomeBoard.STATUS_MOUNTED).equals(board.getStatus())
                && !now.isBefore(board.getPlannedRemoveTime());
    }

    private Map<Long, String> loadFloorMap() {
        return floorService.getAllFloors().stream()
                .collect(Collectors.toMap(Floor::getId, Floor::getFloorName, (a, b) -> a));
    }

    private Map<Long, String> loadRoomMap() {
        return roomService.getAllRooms().stream()
                .collect(Collectors.toMap(ReceptionRoom::getId, ReceptionRoom::getRoomName, (a, b) -> a));
    }

    private WelcomeBoardVO toVO(WelcomeBoard board, Map<Long, String> floorMap,
                                Map<Long, String> roomMap, LocalDateTime now) {
        WelcomeBoardVO vo = new WelcomeBoardVO();
        vo.setId(board.getId());
        vo.setBoardNo(board.getBoardNo());
        vo.setBoardText(board.getBoardText());
        vo.setRoomId(board.getRoomId());
        vo.setRoomName(roomMap.get(board.getRoomId()));
        ReceptionRoom room = board.getRoomId() != null ? roomService.getRoomById(board.getRoomId()) : null;
        vo.setRoomCode(room != null ? room.getRoomCode() : null);
        vo.setFloorId(board.getFloorId());
        vo.setFloorName(floorMap.get(board.getFloorId()));
        vo.setMountTime(board.getMountTime());
        vo.setPlannedRemoveTime(board.getPlannedRemoveTime());
        vo.setRegistrar(board.getRegistrar());
        vo.setStatus(board.getStatus());
        vo.setStatusText(statusText(board.getStatus()));
        vo.setOverdueRemove(isOverdue(board, now));
        vo.setMountedAt(board.getMountedAt());
        vo.setRemovedAt(board.getRemovedAt());
        vo.setRemoveReceipt(board.getRemoveReceipt());
        vo.setRemovedBy(board.getRemovedBy());
        vo.setRemark(board.getRemark());
        vo.setCreatedAt(board.getCreatedAt());
        vo.setUpdatedAt(board.getUpdatedAt());
        return vo;
    }

    private String generateBoardNo() {
        return "YP" + LocalDateTime.now().format(NO_FMT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String statusText(int status) {
        switch (status) {
            case WelcomeBoard.STATUS_SCHEDULED: return "待上墙";
            case WelcomeBoard.STATUS_MOUNTED: return "已上墙";
            case WelcomeBoard.STATUS_REMOVED: return "已撤下";
            default: return "未知";
        }
    }
}
