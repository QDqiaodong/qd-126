package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.WelcomeBoardCreateRequest;
import com.example.devicemanagement.dto.request.WelcomeBoardRemoveRequest;
import com.example.devicemanagement.dto.response.BoardRoomAvailabilityVO;
import com.example.devicemanagement.dto.response.WelcomeBoardVO;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.entity.WelcomeBoard;
import com.example.devicemanagement.mapper.WelcomeBoardMapper;
import com.example.devicemanagement.service.impl.WelcomeBoardServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WelcomeBoardServiceImplTest {

    @Mock
    private WelcomeBoardMapper boardMapper;

    @Mock
    private FloorService floorService;

    @Mock
    private ReceptionRoomService roomService;

    @InjectMocks
    private WelcomeBoardServiceImpl boardService;

    private Floor floor;
    private ReceptionRoom room;
    private ReceptionRoom idleRoom;
    private ReceptionRoom disabledRoom;
    private List<WelcomeBoard> storedBoards;
    private WelcomeBoard captured;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, WelcomeBoard.class);
    }

    @BeforeEach
    void setUp() {
        floor = new Floor();
        floor.setId(1L);
        floor.setFloorName("3F");

        room = new ReceptionRoom();
        room.setId(10L);
        room.setRoomName("301接待室");
        room.setRoomCode("R301");
        room.setFloorId(1L);
        room.setStatus(1);

        idleRoom = new ReceptionRoom();
        idleRoom.setId(20L);
        idleRoom.setRoomName("302接待室");
        idleRoom.setRoomCode("R302");
        idleRoom.setFloorId(1L);
        idleRoom.setStatus(1);

        disabledRoom = new ReceptionRoom();
        disabledRoom.setId(30L);
        disabledRoom.setRoomName("303接待室");
        disabledRoom.setRoomCode("R303");
        disabledRoom.setFloorId(1L);
        disabledRoom.setStatus(0);

        storedBoards = new ArrayList<>();
        captured = null;

        when(floorService.getAllFloors()).thenReturn(List.of(floor));
        when(roomService.getAllRooms()).thenReturn(List.of(room, idleRoom, disabledRoom));
        when(roomService.getRoomById(10L)).thenReturn(room);
        when(roomService.getRoomById(20L)).thenReturn(idleRoom);
        when(roomService.getRoomById(30L)).thenReturn(disabledRoom);

        when(boardMapper.insert(any())).thenAnswer(inv -> {
            WelcomeBoard b = inv.getArgument(0);
            if (b.getId() == null) {
                b.setId(500L + storedBoards.size() + 1L);
            }
            captured = b;
            storedBoards.add(b);
            return 1;
        });
        doAnswer(inv -> 1).when(boardMapper).updateById(any());
        when(boardMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return storedBoards.stream().filter(b -> b.getId().equals(id)).findFirst().orElse(null);
        });
        mockSelectListBySqlSegment();

        when(boardMapper.selectPage(any(Page.class), any())).thenAnswer(inv -> {
            Page<WelcomeBoard> page = inv.getArgument(0);
            // 与SQL排序一致：上墙时间倒序、ID倒序（内存库中直接返回全部，筛选条件用专门用例覆盖）
            List<WelcomeBoard> all = storedBoards.stream()
                    .sorted((x, y) -> {
                        int c = y.getMountTime().compareTo(x.getMountTime());
                        return c != 0 ? c : y.getId().compareTo(x.getId());
                    })
                    .collect(Collectors.toList());
            page.setRecords(all);
            page.setTotal(all.size());
            return page;
        });
    }

    /**
     * 按 SQL 片段与绑定参数区分查询：
     * 待上墙刷新（status=0）、在墙占用（status=1）、待撤列表（status=1 + planned_remove_time）、
     * 分页排序（ORDER BY mount_time）、同接待室冲突（room_id + status&lt;&gt;2，内存判定时段重叠）。
     */
    private void mockSelectListBySqlSegment() {
        when(boardMapper.selectList(any())).thenAnswer(inv -> {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WelcomeBoard> w =
                    inv.getArgument(0, com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
            String sql = w.getSqlSegment();
            LocalDateTime now = LocalDateTime.now();
            if (sql.contains("room_id")) {
                // 同接待室未撤欢迎牌查询，内存判定时段重叠
                return storedBoards.stream()
                        .filter(b -> b.getRoomId() == 10L)
                        .filter(b -> !Integer.valueOf(WelcomeBoard.STATUS_REMOVED).equals(b.getStatus()))
                        .collect(Collectors.toList());
            }
            if (sql.contains("planned_remove_time")) {
                return storedBoards.stream()
                        .filter(b -> Integer.valueOf(WelcomeBoard.STATUS_MOUNTED).equals(b.getStatus()))
                        .filter(b -> !now.isBefore(b.getPlannedRemoveTime()))
                        .collect(Collectors.toList());
            }
            if (sql.contains("mount_time")) {
                // 分页排序查询（ORDER BY mount_time）
                return storedBoards.stream()
                        .sorted((x, y) -> y.getMountTime().compareTo(x.getMountTime()))
                        .collect(Collectors.toList());
            }
            if (sql.contains("status")) {
                // 刷新（status=0）与在墙占用（status=1）片段相同，按绑定参数值区分
                boolean scheduledRefresh = w.getParamNameValuePairs().values().stream()
                        .anyMatch(v -> Integer.valueOf(WelcomeBoard.STATUS_SCHEDULED).equals(v));
                if (scheduledRefresh) {
                    return storedBoards.stream()
                            .filter(b -> Integer.valueOf(WelcomeBoard.STATUS_SCHEDULED).equals(b.getStatus()))
                            .collect(Collectors.toList());
                }
                return storedBoards.stream()
                        .filter(b -> Integer.valueOf(WelcomeBoard.STATUS_MOUNTED).equals(b.getStatus()))
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        });
    }

    private WelcomeBoardCreateRequest request(LocalDateTime mount, LocalDateTime remove) {
        WelcomeBoardCreateRequest req = new WelcomeBoardCreateRequest();
        req.setBoardText("热烈欢迎XX考察团莅临指导");
        req.setRoomId(10L);
        req.setMountTime(mount);
        req.setPlannedRemoveTime(remove);
        req.setRegistrar("行政小王");
        return req;
    }

    private WelcomeBoard storedBoard(Long id, String text, Long roomId, int status,
                                     LocalDateTime mount, LocalDateTime plannedRemove) {
        WelcomeBoard b = new WelcomeBoard();
        b.setId(id);
        b.setBoardNo("YP" + id);
        b.setBoardText(text);
        b.setRoomId(roomId);
        b.setFloorId(1L);
        b.setMountTime(mount);
        b.setPlannedRemoveTime(plannedRemove);
        b.setRegistrar("行政小王");
        b.setStatus(status);
        storedBoards.add(b);
        return b;
    }

    // ---------- 登记 ----------

    @Test
    void createBoardPersistsScheduleWithFutureMountTime() {
        LocalDateTime now = LocalDateTime.now();
        WelcomeBoardVO vo = boardService.createBoard(request(now.plusDays(1), now.plusDays(1).plusHours(2)));

        assertNotNull(vo.getId());
        assertTrue(vo.getBoardNo().startsWith("YP"));
        assertEquals("热烈欢迎XX考察团莅临指导", vo.getBoardText());
        assertEquals(10L, vo.getRoomId());
        assertEquals("301接待室", vo.getRoomName());
        assertEquals("R301", vo.getRoomCode());
        assertEquals("3F", vo.getFloorName());
        assertEquals("行政小王", vo.getRegistrar());
        assertEquals(WelcomeBoard.STATUS_SCHEDULED, vo.getStatus());
        assertEquals("待上墙", vo.getStatusText());
        assertFalse(vo.getOverdueRemove());
        assertNull(vo.getMountedAt());
        assertEquals(1, storedBoards.size());
    }

    @Test
    void createBoardWithMountTimeReachedMarkedMounted() {
        LocalDateTime now = LocalDateTime.now();
        WelcomeBoardVO vo = boardService.createBoard(request(now.minusMinutes(5), now.plusHours(2)));
        assertEquals(WelcomeBoard.STATUS_MOUNTED, vo.getStatus());
        assertEquals("已上墙", vo.getStatusText());
        assertNotNull(captured.getMountedAt());
    }

    @Test
    void createRejectsInvalidFields() {
        LocalDateTime now = LocalDateTime.now();

        WelcomeBoardCreateRequest noText = request(now.plusDays(1), now.plusDays(1).plusHours(1));
        noText.setBoardText("   ");
        assertThrows(IllegalArgumentException.class, () -> boardService.createBoard(noText));

        WelcomeBoardCreateRequest noRoom = request(now.plusDays(1), now.plusDays(1).plusHours(1));
        noRoom.setRoomId(null);
        assertThrows(IllegalArgumentException.class, () -> boardService.createBoard(noRoom));

        WelcomeBoardCreateRequest unknownRoom = request(now.plusDays(1), now.plusDays(1).plusHours(1));
        unknownRoom.setRoomId(99L);
        when(roomService.getRoomById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> boardService.createBoard(unknownRoom));

        room.setStatus(0);
        assertThrows(IllegalArgumentException.class, () ->
                boardService.createBoard(request(now.plusDays(1), now.plusDays(1).plusHours(1))));
        room.setStatus(1);

        WelcomeBoardCreateRequest badOrder = request(now.plusDays(1).plusHours(2), now.plusDays(1));
        assertThrows(IllegalArgumentException.class, () -> boardService.createBoard(badOrder));

        WelcomeBoardCreateRequest pastRemove = request(now.minusHours(2), now.minusMinutes(1));
        assertThrows(IllegalArgumentException.class, () -> boardService.createBoard(pastRemove));

        WelcomeBoardCreateRequest noRegistrar = request(now.plusDays(1), now.plusDays(1).plusHours(1));
        noRegistrar.setRegistrar("  ");
        assertThrows(IllegalArgumentException.class, () -> boardService.createBoard(noRegistrar));
    }

    // ---------- 时段重叠拦截 ----------

    @Test
    void createBlocksWhenSameRoomScheduleOverlaps() {
        LocalDateTime now = LocalDateTime.now();
        storedBoard(900L, "原有欢迎牌", 10L, WelcomeBoard.STATUS_SCHEDULED,
                now.plusDays(1).plusHours(1), now.plusDays(1).plusHours(3));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                boardService.createBoard(request(now.plusDays(1).plusHours(2), now.plusDays(1).plusHours(4))));
        assertTrue(ex.getMessage().contains("同一接待室欢迎牌时段冲突"));
        assertTrue(ex.getMessage().contains("原有欢迎牌"));
        // 冲突时不得落库
        assertEquals(1, storedBoards.size());
    }

    @Test
    void createAllowsBackToBackScheduleWhenBoundariesTouch() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime boundary = now.plusDays(1).plusHours(3);
        storedBoard(901L, "原有欢迎牌", 10L, WelcomeBoard.STATUS_SCHEDULED,
                now.plusDays(1).plusHours(1), boundary);

        // 新排期从上一块撤下时刻开始：边界相接不算重叠
        assertDoesNotThrow(() ->
                boardService.createBoard(request(boundary, boundary.plusHours(2))));
        assertEquals(2, storedBoards.size());
    }

    @Test
    void createIgnoresRemovedBoardWhenCheckingOverlap() {
        LocalDateTime now = LocalDateTime.now();
        WelcomeBoard removed = storedBoard(902L, "已撤欢迎牌", 10L, WelcomeBoard.STATUS_REMOVED,
                now.plusDays(1).plusHours(1), now.plusDays(1).plusHours(3));
        removed.setRemovedAt(now.minusDays(1));
        removed.setRemoveReceipt("已撤下回执");

        assertDoesNotThrow(() ->
                boardService.createBoard(request(now.plusDays(1).plusHours(1), now.plusDays(1).plusHours(3))));
    }

    @Test
    void createBlocksWhenOverdueBoardStillMountedUntilReceipt() {
        LocalDateTime now = LocalDateTime.now();
        // 已上墙且到期待撤：物理上仍在墙上，占位视为无限远
        storedBoard(903L, "忘撤的欢迎牌", 10L, WelcomeBoard.STATUS_MOUNTED,
                now.minusDays(1), now.minusHours(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                boardService.createBoard(request(now.plusHours(1), now.plusHours(3))));
        assertTrue(ex.getMessage().contains("尚未撤下"));
        assertTrue(ex.getMessage().contains("撤下回执"));
    }

    // ---------- 状态懒推进与待撤标记 ----------

    @Test
    void queryingListAutoMountsDueSchedule() {
        LocalDateTime now = LocalDateTime.now();
        WelcomeBoard due = storedBoard(701L, "到点上墙", 10L, WelcomeBoard.STATUS_SCHEDULED,
                now.minusMinutes(10), now.plusHours(2));
        WelcomeBoard future = storedBoard(702L, "未到点", 20L, WelcomeBoard.STATUS_SCHEDULED,
                now.plusDays(1), now.plusDays(1).plusHours(1));

        boardService.getBoardsPage(1, 10, null, null, null, null);

        // 到点自动上墙并落实际上墙时间；未到点保持待上墙
        assertEquals(WelcomeBoard.STATUS_MOUNTED, due.getStatus());
        assertEquals(due.getMountTime(), due.getMountedAt());
        assertEquals(WelcomeBoard.STATUS_SCHEDULED, future.getStatus());
        assertNull(future.getMountedAt());
    }

    @Test
    void expiredMountedBoardFlaggedOverdueButStatusStaysMounted() {
        LocalDateTime now = LocalDateTime.now();
        WelcomeBoard overdue = storedBoard(703L, "到期未撤", 10L, WelcomeBoard.STATUS_MOUNTED,
                now.minusHours(3), now.minusMinutes(1));

        WelcomeBoardVO vo = boardService.getBoardById(703L);

        // 到期不会自动撤下：状态仍是已上墙，但待撤标记为 true
        assertEquals(WelcomeBoard.STATUS_MOUNTED, vo.getStatus());
        assertTrue(vo.getOverdueRemove());
        assertEquals("已上墙", vo.getStatusText());
        assertNull(vo.getRemovedAt());
    }

    @Test
    void overdueListReturnsOnlyMountedBoardsPastPlannedRemoveTime() {
        LocalDateTime now = LocalDateTime.now();
        storedBoard(801L, "待撤", 10L, WelcomeBoard.STATUS_MOUNTED,
                now.minusHours(3), now.minusMinutes(30));
        storedBoard(802L, "正常在墙", 20L, WelcomeBoard.STATUS_MOUNTED,
                now.minusMinutes(10), now.plusHours(2));
        storedBoard(803L, "待上墙", 10L, WelcomeBoard.STATUS_SCHEDULED,
                now.plusDays(1), now.plusDays(1).plusHours(1));

        List<WelcomeBoardVO> overdue = boardService.getOverdueBoards();

        assertEquals(1, overdue.size());
        assertEquals("待撤", overdue.get(0).getBoardText());
        assertTrue(overdue.get(0).getOverdueRemove());
    }

    // ---------- 撤下回执 ----------

    @Test
    void removeBoardRequiresReceipt() {
        LocalDateTime now = LocalDateTime.now();
        storedBoard(601L, "在墙欢迎牌", 10L, WelcomeBoard.STATUS_MOUNTED,
                now.minusHours(2), now.plusHours(1));

        WelcomeBoardRemoveRequest empty = new WelcomeBoardRemoveRequest();
        empty.setReceipt("   ");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> boardService.removeBoard(601L, empty));
        assertTrue(ex.getMessage().contains("撤下回执"));

        assertThrows(IllegalArgumentException.class,
                () -> boardService.removeBoard(601L, new WelcomeBoardRemoveRequest()));

        // 被拦截后标记不得拿掉
        assertEquals(WelcomeBoard.STATUS_MOUNTED, storedBoards.get(0).getStatus());
        assertNull(storedBoards.get(0).getRemovedAt());
    }

    @Test
    void removeBoardWithReceiptClearsOverdueFlagAndFreesRoom() {
        LocalDateTime now = LocalDateTime.now();
        WelcomeBoard board = storedBoard(602L, "待撤欢迎牌", 10L, WelcomeBoard.STATUS_MOUNTED,
                now.minusHours(3), now.minusMinutes(30));

        WelcomeBoardRemoveRequest req = new WelcomeBoardRemoveRequest();
        req.setReceipt("欢迎牌已取下，墙面恢复整洁，物料归库");
        req.setOperator("值班员李四");

        WelcomeBoardVO vo = boardService.removeBoard(602L, req);

        assertEquals(WelcomeBoard.STATUS_REMOVED, vo.getStatus());
        assertEquals("已撤下", vo.getStatusText());
        assertFalse(vo.getOverdueRemove());
        assertNotNull(vo.getRemovedAt());
        assertEquals("欢迎牌已取下，墙面恢复整洁，物料归库", vo.getRemoveReceipt());
        assertEquals("值班员李四", vo.getRemovedBy());

        // 撤下后该接待室立即可登记新欢迎牌（占位释放）
        assertDoesNotThrow(() ->
                boardService.createBoard(request(now.plusMinutes(5), now.plusHours(2))));
    }

    @Test
    void removeBoardRejectsScheduledAndAlreadyRemoved() {
        LocalDateTime now = LocalDateTime.now();
        storedBoard(603L, "未上墙", 10L, WelcomeBoard.STATUS_SCHEDULED,
                now.plusDays(1), now.plusDays(1).plusHours(1));
        WelcomeBoard removed = storedBoard(604L, "已撤", 20L, WelcomeBoard.STATUS_REMOVED,
                now.minusDays(2), now.minusDays(2).plusHours(2));
        removed.setRemovedAt(now.minusDays(2).plusHours(2));
        removed.setRemoveReceipt("旧回执");

        WelcomeBoardRemoveRequest req = new WelcomeBoardRemoveRequest();
        req.setReceipt("一条回执");

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> boardService.removeBoard(603L, req));
        assertTrue(ex1.getMessage().contains("尚未到上墙时间"));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> boardService.removeBoard(604L, req));
        assertTrue(ex2.getMessage().contains("已撤下"));
    }

    // ---------- 可接待情况 ----------

    @Test
    void roomAvailabilityReflectsMountedBoardAndOverdue() {
        LocalDateTime now = LocalDateTime.now();
        storedBoard(501L, "在墙欢迎牌", 10L, WelcomeBoard.STATUS_MOUNTED,
                now.minusHours(1), now.plusHours(1));
        storedBoard(502L, "待撤欢迎牌", 30L, WelcomeBoard.STATUS_MOUNTED,
                now.minusHours(2), now.minusMinutes(10));
        // 20号接待室无任何在墙欢迎牌

        List<BoardRoomAvailabilityVO> list = boardService.getRoomAvailability(null);

        BoardRoomAvailabilityVO r301 = list.stream().filter(v -> v.getRoomId() == 10L).findFirst().orElseThrow();
        BoardRoomAvailabilityVO r302 = list.stream().filter(v -> v.getRoomId() == 20L).findFirst().orElseThrow();
        BoardRoomAvailabilityVO r303 = list.stream().filter(v -> v.getRoomId() == 30L).findFirst().orElseThrow();

        assertFalse(r301.getAvailable());
        assertEquals("在墙欢迎牌", r301.getBoardText());
        assertFalse(r301.getOverdueRemove());
        assertTrue(r302.getAvailable());
        assertNull(r302.getBoardId());
        // 停用接待室即便有牌子也不可接待
        assertFalse(r303.getAvailable());
        assertFalse(r303.getRoomEnabled());
        assertTrue(r303.getOverdueRemove());
    }

    @Test
    void scheduledBoardDoesNotBlockAvailabilityBeforeMountTime() {
        LocalDateTime now = LocalDateTime.now();
        storedBoard(503L, "明天的欢迎牌", 10L, WelcomeBoard.STATUS_SCHEDULED,
                now.plusDays(1), now.plusDays(1).plusHours(2));

        BoardRoomAvailabilityVO r301 = boardService.getRoomAvailability(null).stream()
                .filter(v -> v.getRoomId() == 10L).findFirst().orElseThrow();
        assertTrue(r301.getAvailable());
        assertNull(r301.getBoardId());
    }

    // ---------- 重启一致性（状态全部来自持久层 + 时间派生） ----------

    @Test
    void stateIsDerivedFromPersistedScheduleAcrossRestart() {
        // 模拟"关掉再打开"：持久层里是一条状态停在待上墙、但两个时间点都已过的排期
        LocalDateTime now = LocalDateTime.now();
        WelcomeBoard stale = new WelcomeBoard();
        stale.setId(950L);
        stale.setBoardNo("YP950");
        stale.setBoardText("重启前忘推进的牌子");
        stale.setRoomId(10L);
        stale.setFloorId(1L);
        stale.setMountTime(now.minusHours(5));
        stale.setPlannedRemoveTime(now.minusHours(1));
        stale.setRegistrar("行政小王");
        stale.setStatus(WelcomeBoard.STATUS_SCHEDULED);
        storedBoards.add(stale);

        // 新实例（模拟重启后内存状态清空），依赖库中数据 + 当前时间重算
        WelcomeBoardServiceImpl restartedService = new WelcomeBoardServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(restartedService, "boardMapper", boardMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(restartedService, "floorService", floorService);
        org.springframework.test.util.ReflectionTestUtils.setField(restartedService, "roomService", roomService);

        WelcomeBoardVO vo = restartedService.getBoardById(950L);

        // 到点自动推进为已上墙；撤下时间已过但无回执 -> 待撤；接待室仍不可接待
        assertEquals(WelcomeBoard.STATUS_MOUNTED, vo.getStatus());
        assertTrue(vo.getOverdueRemove());
        BoardRoomAvailabilityVO availability = restartedService.getRoomAvailability(null).stream()
                .filter(v -> v.getRoomId() == 10L).findFirst().orElseThrow();
        assertFalse(availability.getAvailable());
        assertTrue(availability.getOverdueRemove());

        // 写回执撤下后，三处状态同步翻转
        WelcomeBoardRemoveRequest req = new WelcomeBoardRemoveRequest();
        req.setReceipt("重启后补登记：牌子已撤");
        WelcomeBoardVO removed = restartedService.removeBoard(950L, req);
        assertEquals(WelcomeBoard.STATUS_REMOVED, removed.getStatus());
        assertFalse(removed.getOverdueRemove());
        assertTrue(restartedService.getRoomAvailability(null).stream()
                .filter(v -> v.getRoomId() == 10L).findFirst().orElseThrow().getAvailable());

        // 落库更新包含状态推进与撤下两个动作
        verify(boardMapper, atLeast(2)).updateById(any());
    }

    @Test
    void pageQueryPassesFiltersToWrapper() {
        LocalDateTime now = LocalDateTime.now();
        storedBoard(701L, "A", 10L, WelcomeBoard.STATUS_MOUNTED, now, now.plusHours(2));

        boardService.getBoardsPage(2, 20, 1L, 10L, WelcomeBoard.STATUS_MOUNTED, true);

        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(boardMapper).selectPage(any(Page.class), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("floor_id"));
        assertTrue(sql.contains("room_id"));
        assertTrue(sql.contains("status"));
        assertTrue(sql.contains("planned_remove_time"));
    }
}
