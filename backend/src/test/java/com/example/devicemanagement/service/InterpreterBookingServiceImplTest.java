package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.InterpreterBookingRequest;
import com.example.devicemanagement.dto.response.InterpreterBookingVO;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.InterpreterBooking;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.exception.InterpreterConflictException;
import com.example.devicemanagement.mapper.InterpreterBookingMapper;
import com.example.devicemanagement.service.impl.InterpreterBookingServiceImpl;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterpreterBookingServiceImplTest {

    @Mock
    private InterpreterBookingMapper bookingMapper;

    @Mock
    private FloorService floorService;

    @Mock
    private ReceptionRoomService roomService;

    @InjectMocks
    private InterpreterBookingServiceImpl bookingService;

    private Floor floor;
    private ReceptionRoom room;
    private ReceptionRoom room2;
    private List<InterpreterBooking> stored;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, InterpreterBooking.class);
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

        room2 = new ReceptionRoom();
        room2.setId(11L);
        room2.setRoomName("302接待室");
        room2.setRoomCode("R302");
        room2.setFloorId(1L);
        room2.setStatus(1);

        stored = new ArrayList<>();

        when(floorService.getAllFloors()).thenReturn(List.of(floor));
        when(roomService.getAllRooms()).thenReturn(List.of(room, room2));
        when(roomService.getRoomById(10L)).thenReturn(room);
        when(roomService.getRoomById(11L)).thenReturn(room2);

        when(bookingMapper.insert(any())).thenAnswer(inv -> {
            InterpreterBooking b = inv.getArgument(0);
            if (b.getId() == null) {
                b.setId(100L + stored.size() + 1L);
            }
            stored.add(b);
            return 1;
        });
        doAnswer(inv -> 1).when(bookingMapper).updateById(any());
        doAnswer(inv -> {
            Long id = inv.getArgument(0);
            stored.removeIf(b -> b.getId().equals(id));
            return 1;
        }).when(bookingMapper).deleteById(anyLong());
        when(bookingMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return stored.stream().filter(b -> b.getId().equals(id)).findFirst().orElse(null);
        });

        // 按参数类型/数量区分：状态推进 / 同接待室冲突 / 同译员撞车 / 接待室预约列表
        // wrapper 参数按生成顺序命名、不可假定索引，统一按值类型提取
        when(bookingMapper.selectList(any())).thenAnswer(inv -> {
            LambdaQueryWrapper<InterpreterBooking> wrapper = inv.getArgument(0);
            String sql = wrapper.getSqlSegment();
            Map<String, Object> params = wrapper.getParamNameValuePairs();
            Collection<Object> values = params.values();

            String name = values.stream().filter(v -> v instanceof String).map(v -> (String) v)
                    .findFirst().orElse(null);
            List<LocalDateTime> times = values.stream()
                    .filter(v -> v instanceof LocalDateTime).map(v -> (LocalDateTime) v)
                    .collect(java.util.stream.Collectors.toList());
            List<Long> ids = values.stream().filter(v -> v instanceof Long).map(v -> (Long) v)
                    .collect(java.util.stream.Collectors.toList());
            Long excludeId = ids.size() >= 2 ? ids.get(1) : null;

            if (name != null && times.size() == 2) {
                // 译员冲突：姓名 + 起止两个时间（excludeId 为第二个 Long）
                return filterConflicts(name, true, times.get(0), times.get(1), excludeId);
            }
            if (!ids.isEmpty() && times.size() == 2) {
                // 同接待室时段冲突：roomId + 起止两个时间
                return filterConflicts(ids.get(0), false, times.get(0), times.get(1), excludeId);
            }
            if (!ids.isEmpty() && sql.contains("room_id")) {
                // 接待室预约列表：仅 roomId 一个等值参数
                return stored.stream().filter(b -> ids.get(0).equals(b.getRoomId()))
                        .collect(java.util.stream.Collectors.toList());
            }
            // 状态推进：未结束预约
            return stored.stream()
                    .filter(b -> !Integer.valueOf(InterpreterBooking.STATUS_ENDED).equals(b.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
        });
        when(bookingMapper.selectPage(any(Page.class), any())).thenAnswer(inv -> {
            Page<InterpreterBooking> page = inv.getArgument(0);
            Wrapper<InterpreterBooking> wrapper = inv.getArgument(1);
            String sql = wrapper.getSqlSegment();
            List<InterpreterBooking> all;
            if (sql.contains("floor_id")) {
                Map<String, Object> p = ((LambdaQueryWrapper<InterpreterBooking>) wrapper).getParamNameValuePairs();
                Long floorId = (Long) p.values().iterator().next();
                all = stored.stream().filter(b -> floorId.equals(b.getFloorId()))
                        .collect(java.util.stream.Collectors.toList());
            } else {
                all = new ArrayList<>(stored);
            }
            page.setRecords(all);
            page.setTotal(all.size());
            return page;
        });
    }

    /**
     * 内存模拟 SQL 重叠判定：未结束、不排除自身、start &lt; other.end 且 other.start &lt; end。
     */
    private List<InterpreterBooking> filterConflicts(Object key, boolean byInterpreter,
                                                     LocalDateTime start, LocalDateTime end,
                                                     Long excludeId) {
        List<InterpreterBooking> result = new ArrayList<>();
        for (InterpreterBooking b : stored) {
            if (excludeId != null && excludeId.equals(b.getId())) {
                continue;
            }
            if (Integer.valueOf(InterpreterBooking.STATUS_ENDED).equals(b.getStatus())) {
                continue;
            }
            boolean keyMatch = byInterpreter
                    ? key.equals(b.getInterpreterName())
                    : key.equals(b.getRoomId());
            if (!keyMatch) {
                continue;
            }
            if (start.isBefore(b.getEndTime()) && b.getStartTime().isBefore(end)) {
                result.add(b);
            }
        }
        return result;
    }

    private InterpreterBookingRequest request(Long roomId, String interpreter,
                                              LocalDateTime start, LocalDateTime end) {
        InterpreterBookingRequest req = new InterpreterBookingRequest();
        req.setRoomId(roomId);
        req.setLanguage("英语");
        req.setInterpreterName(interpreter);
        req.setStartTime(start);
        req.setEndTime(end);
        return req;
    }

    @Test
    void createPersistsBookingWithLanguageInterpreterAndFloor() {
        LocalDateTime now = LocalDateTime.now();
        InterpreterBookingVO vo = bookingService.createBooking(
                request(10L, "王芳", now.plusDays(1), now.plusDays(1).plusHours(2)));

        assertNotNull(vo.getId());
        assertTrue(vo.getBookingNo().startsWith("FY"));
        assertEquals(10L, vo.getRoomId());
        assertEquals("301接待室", vo.getRoomName());
        assertEquals("R301", vo.getRoomCode());
        assertEquals(1L, vo.getFloorId());
        assertEquals("3F", vo.getFloorName());
        assertEquals("英语", vo.getLanguage());
        assertEquals("王芳", vo.getInterpreterName());
        assertEquals(0, vo.getStatus());
        assertEquals("待开始", vo.getStatusText());
        assertEquals(1, stored.size());
        // 楼层冗余落库，便于按楼层筛选
        assertEquals(1L, stored.get(0).getFloorId());
    }

    @Test
    void createRejectsInvalidInput() {
        LocalDateTime now = LocalDateTime.now();

        InterpreterBookingRequest noRoom = request(10L, "王芳", now.plusDays(1), now.plusDays(1).plusHours(1));
        noRoom.setRoomId(null);
        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(noRoom));

        InterpreterBookingRequest unknownRoom = request(99L, "王芳", now.plusDays(1), now.plusDays(1).plusHours(1));
        when(roomService.getRoomById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(unknownRoom));

        InterpreterBookingRequest noLanguage = request(10L, "王芳", now.plusDays(1), now.plusDays(1).plusHours(1));
        noLanguage.setLanguage("  ");
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(noLanguage));
        assertTrue(ex1.getMessage().contains("语种"));

        InterpreterBookingRequest noInterpreter = request(10L, "  ", now.plusDays(1), now.plusDays(1).plusHours(1));
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(noInterpreter));
        assertTrue(ex2.getMessage().contains("译员"));

        InterpreterBookingRequest badRange = request(10L, "王芳", now.plusDays(1).plusHours(1), now.plusDays(1));
        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(badRange));

        InterpreterBookingRequest passedEnd = request(10L, "王芳", now.minusHours(2), now.minusHours(1));
        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(passedEnd));

        assertEquals(0, stored.size());
    }

    @Test
    void sameInterpreterOverlapBlocksAndCarriesBookedRoom() {
        LocalDateTime now = LocalDateTime.now();
        bookingService.createBooking(request(10L, "王芳", now.plusDays(1), now.plusDays(1).plusHours(2)));

        // 另一接待室、重叠时段、同一译员 → 撞车拦截并弹出已约接待室
        InterpreterBookingRequest clash = request(11L, "王芳",
                now.plusDays(1).plusHours(1), now.plusDays(1).plusHours(3));
        InterpreterConflictException ex = assertThrows(InterpreterConflictException.class,
                () -> bookingService.createBooking(clash));
        assertTrue(ex.getMessage().contains("王芳"));
        assertTrue(ex.getMessage().contains("301接待室"));
        assertTrue(ex.getMessage().contains("撞车"));
        assertEquals(1, stored.size());
    }

    @Test
    void sameInterpreterWithTouchingBoundaryIsAllowed() {
        LocalDateTime now = LocalDateTime.now();
        bookingService.createBooking(request(10L, "王芳", now.plusDays(1), now.plusDays(1).plusHours(2)));

        // 边界相接（16:00 接 16:00）不算重叠，换一间接待室可约
        InterpreterBookingVO vo = bookingService.createBooking(
                request(11L, "王芳", now.plusDays(1).plusHours(2), now.plusDays(1).plusHours(3)));
        assertNotNull(vo.getId());
        assertEquals(2, stored.size());
    }

    @Test
    void differentInterpretersSameRoomOverlapBlocked() {
        LocalDateTime now = LocalDateTime.now();
        bookingService.createBooking(request(10L, "王芳", now.plusDays(1), now.plusDays(1).plusHours(2)));

        // 同一接待室时段重叠，即使译员不同也不能重复预约
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(
                        request(10L, "李娜", now.plusDays(1).plusHours(1), now.plusDays(1).plusHours(3))));
        assertTrue(ex.getMessage().contains("已有翻译预约"));
        assertEquals(1, stored.size());
    }

    @Test
    void updatePendingBookingChangesInterpreterAndRange() {
        LocalDateTime now = LocalDateTime.now();
        InterpreterBookingVO created = bookingService.createBooking(
                request(10L, "王芳", now.plusDays(1), now.plusDays(1).plusHours(2)));

        InterpreterBookingRequest update = request(10L, "李娜", now.plusDays(2), now.plusDays(2).plusHours(2));
        update.setLanguage("日语");
        InterpreterBookingVO vo = bookingService.updateBooking(created.getId(), update);

        assertEquals("李娜", vo.getInterpreterName());
        assertEquals("日语", vo.getLanguage());
        assertEquals(now.plusDays(2), vo.getStartTime());
        assertEquals(1, stored.size());

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateBooking(999L, update));
    }

    @Test
    void updateToClashingInterpreterIsBlockedExcludingSelf() {
        LocalDateTime now = LocalDateTime.now();
        bookingService.createBooking(
                request(10L, "王芳", now.plusDays(1), now.plusDays(1).plusHours(2)));
        bookingService.createBooking(
                request(11L, "李娜", now.plusDays(1).plusHours(3), now.plusDays(1).plusHours(4)));

        // 把 302 的预约改成与王芳重叠的时段 → 撞车（自身 302 不参与判定）
        Long bookingBId = stored.get(1).getId();
        InterpreterBookingRequest clash = request(11L, "王芳",
                now.plusDays(1).plusHours(1), now.plusDays(1).plusHours(2));
        assertThrows(InterpreterConflictException.class,
                () -> bookingService.updateBooking(bookingBId, clash));

        // 自身排除：把 301 的预约改成同名不重叠时段，不应误报撞车
        InterpreterBookingRequest self = request(10L, "王芳",
                now.plusDays(2), now.plusDays(2).plusHours(1));
        assertDoesNotThrow(() -> bookingService.updateBooking(stored.get(0).getId(), self));
    }

    @Test
    void ongoingBookingIsReadOnlyAndCannotChangeInterpreter() {
        LocalDateTime now = LocalDateTime.now();
        // 登记时已在时段内 → 直接进行中
        InterpreterBookingVO created = bookingService.createBooking(
                request(10L, "王芳", now.minusHours(1), now.plusHours(1)));
        assertEquals(InterpreterBooking.STATUS_ONGOING, created.getStatus());

        InterpreterBookingRequest update = request(10L, "李娜", now.plusDays(1), now.plusDays(1).plusHours(1));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateBooking(created.getId(), update));
        assertTrue(ex.getMessage().contains("活动已开始"));
        assertTrue(ex.getMessage().contains("只允许查看"));

        IllegalArgumentException deleteEx = assertThrows(IllegalArgumentException.class,
                () -> bookingService.deleteBooking(created.getId()));
        assertTrue(deleteEx.getMessage().contains("只允许查看"));
        assertEquals(1, stored.size());
    }

    @Test
    void endedBookingIsReadOnlyAndLazyAdvanced() {
        LocalDateTime now = LocalDateTime.now();
        // 直接落一条已过期的待开始预约，详情查询时懒推进为已结束
        InterpreterBooking stale = new InterpreterBooking();
        stale.setBookingNo("FY-OLD");
        stale.setRoomId(10L);
        stale.setFloorId(1L);
        stale.setLanguage("英语");
        stale.setInterpreterName("王芳");
        stale.setStartTime(now.minusHours(3));
        stale.setEndTime(now.minusHours(2));
        stale.setStatus(InterpreterBooking.STATUS_PENDING);
        bookingMapper.insert(stale);

        InterpreterBookingVO vo = bookingService.getBookingById(stale.getId());
        assertEquals(InterpreterBooking.STATUS_ENDED, vo.getStatus());
        assertEquals("已结束", vo.getStatusText());

        InterpreterBookingRequest update = request(10L, "李娜", now.plusDays(1), now.plusDays(1).plusHours(1));
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateBooking(stale.getId(), update));
    }

    @Test
    void deletePendingBookingRemovesIt() {
        LocalDateTime now = LocalDateTime.now();
        InterpreterBookingVO created = bookingService.createBooking(
                request(10L, "王芳", now.plusDays(1), now.plusDays(1).plusHours(1)));
        assertEquals(1, stored.size());

        bookingService.deleteBooking(created.getId());
        assertEquals(0, stored.size());

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.deleteBooking(created.getId()));
    }

    @Test
    void pagePassesFloorFilterToWrapper() {
        LocalDateTime now = LocalDateTime.now();
        bookingService.createBooking(request(10L, "王芳", now.plusDays(1), now.plusDays(1).plusHours(1)));

        var page = bookingService.getBookingsPage(1, 10, 1L, null);
        assertEquals(1, page.getRecords().size());

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(bookingMapper).selectPage(any(Page.class), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("floor_id"));
    }

    @Test
    void roomBookingsReturnAllOfRoom() {
        LocalDateTime now = LocalDateTime.now();
        bookingService.createBooking(request(10L, "王芳", now.plusDays(1), now.plusDays(1).plusHours(1)));
        bookingService.createBooking(request(10L, "李娜", now.plusDays(2), now.plusDays(2).plusHours(1)));
        bookingService.createBooking(request(11L, "赵磊", now.plusDays(3), now.plusDays(3).plusHours(1)));

        List<InterpreterBookingVO> list = bookingService.getRoomBookings(10L);
        assertEquals(2, list.size());
        assertTrue(list.stream().allMatch(v -> "301接待室".equals(v.getRoomName())));
    }
}
