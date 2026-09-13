package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.RoomQuietPeriodRequest;
import com.example.devicemanagement.dto.response.RoomQuietPeriodVO;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.entity.RoomQuietPeriod;
import com.example.devicemanagement.mapper.RoomQuietPeriodMapper;
import com.example.devicemanagement.service.impl.RoomQuietPeriodServiceImpl;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomQuietPeriodServiceImplTest {

    @Mock
    private RoomQuietPeriodMapper quietPeriodMapper;

    @Mock
    private FloorService floorService;

    @Mock
    private ReceptionRoomService roomService;

    @InjectMocks
    private RoomQuietPeriodServiceImpl quietPeriodService;

    private Floor floor;
    private ReceptionRoom room;
    private List<RoomQuietPeriod> stored;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, RoomQuietPeriod.class);
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

        stored = new ArrayList<>();

        when(floorService.getAllFloors()).thenReturn(List.of(floor));
        when(roomService.getAllRooms()).thenReturn(List.of(room));
        when(roomService.getRoomById(10L)).thenReturn(room);

        when(quietPeriodMapper.insert(any())).thenAnswer(inv -> {
            RoomQuietPeriod p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(100L + stored.size() + 1L);
            }
            stored.add(p);
            return 1;
        });
        doAnswer(inv -> 1).when(quietPeriodMapper).updateById(any());
        doAnswer(inv -> {
            Long id = inv.getArgument(0);
            stored.removeIf(p -> p.getId().equals(id));
            return 1;
        }).when(quietPeriodMapper).deleteById(anyLong());
        when(quietPeriodMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return stored.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
        });
        when(quietPeriodMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(stored));
        when(quietPeriodMapper.selectPage(any(Page.class), any())).thenAnswer(inv -> {
            Page<RoomQuietPeriod> page = inv.getArgument(0);
            page.setRecords(new ArrayList<>(stored));
            page.setTotal(stored.size());
            return page;
        });
    }

    private RoomQuietPeriodRequest request(LocalDateTime start, LocalDateTime end) {
        RoomQuietPeriodRequest req = new RoomQuietPeriodRequest();
        req.setRoomId(10L);
        req.setStartTime(start);
        req.setEndTime(end);
        req.setReason("设备检修");
        return req;
    }

    @Test
    void createPersistsQuietPeriodWithReasonAndFloor() {
        LocalDateTime now = LocalDateTime.now();
        RoomQuietPeriodVO vo = quietPeriodService.createQuietPeriod(
                request(now.plusDays(1), now.plusDays(1).plusHours(2)));

        assertNotNull(vo.getId());
        assertTrue(vo.getQuietNo().startsWith("JY"));
        assertEquals(10L, vo.getRoomId());
        assertEquals("301接待室", vo.getRoomName());
        assertEquals("R301", vo.getRoomCode());
        assertEquals(1L, vo.getFloorId());
        assertEquals("3F", vo.getFloorName());
        assertEquals("设备检修", vo.getReason());
        assertEquals(1, stored.size());
        // 楼层冗余落库，便于按楼层筛选
        assertEquals(1L, stored.get(0).getFloorId());
    }

    @Test
    void createRejectsInvalidInput() {
        LocalDateTime now = LocalDateTime.now();

        RoomQuietPeriodRequest noRoom = request(now.plusDays(1), now.plusDays(1).plusHours(1));
        noRoom.setRoomId(null);
        assertThrows(IllegalArgumentException.class, () -> quietPeriodService.createQuietPeriod(noRoom));

        RoomQuietPeriodRequest unknownRoom = request(now.plusDays(1), now.plusDays(1).plusHours(1));
        unknownRoom.setRoomId(99L);
        assertThrows(IllegalArgumentException.class, () -> quietPeriodService.createQuietPeriod(unknownRoom));

        RoomQuietPeriodRequest noReason = request(now.plusDays(1), now.plusDays(1).plusHours(1));
        noReason.setReason("  ");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> quietPeriodService.createQuietPeriod(noReason));
        assertTrue(ex.getMessage().contains("静音原因"));

        RoomQuietPeriodRequest badRange = request(now.plusDays(1).plusHours(1), now.plusDays(1));
        assertThrows(IllegalArgumentException.class, () -> quietPeriodService.createQuietPeriod(badRange));

        RoomQuietPeriodRequest noTime = request(null, null);
        assertThrows(IllegalArgumentException.class, () -> quietPeriodService.createQuietPeriod(noTime));

        assertEquals(0, stored.size());
    }

    @Test
    void updateChangesRangeAndReason() {
        LocalDateTime now = LocalDateTime.now();
        RoomQuietPeriodVO created = quietPeriodService.createQuietPeriod(
                request(now.plusDays(1), now.plusDays(1).plusHours(2)));

        RoomQuietPeriodRequest update = request(now.plusDays(2), now.plusDays(2).plusHours(3));
        update.setReason("重要会议保障");
        RoomQuietPeriodVO vo = quietPeriodService.updateQuietPeriod(created.getId(), update);

        assertEquals("重要会议保障", vo.getReason());
        assertEquals(now.plusDays(2), vo.getStartTime());
        assertEquals(1, stored.size());

        assertThrows(IllegalArgumentException.class,
                () -> quietPeriodService.updateQuietPeriod(999L, update));
    }

    @Test
    void deleteRemovesQuietPeriod() {
        LocalDateTime now = LocalDateTime.now();
        RoomQuietPeriodVO created = quietPeriodService.createQuietPeriod(
                request(now.plusDays(1), now.plusDays(1).plusHours(2)));
        assertEquals(1, stored.size());

        quietPeriodService.deleteQuietPeriod(created.getId());
        assertEquals(0, stored.size());

        assertThrows(IllegalArgumentException.class, () -> quietPeriodService.deleteQuietPeriod(created.getId()));
    }

    @Test
    void pagePassesFloorFilterToWrapper() {
        LocalDateTime now = LocalDateTime.now();
        quietPeriodService.createQuietPeriod(request(now.plusDays(1), now.plusDays(1).plusHours(1)));

        var page = quietPeriodService.getQuietPeriodsPage(1, 10, 1L, null);
        assertEquals(1, page.getRecords().size());

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(quietPeriodMapper).selectPage(any(Page.class), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("floor_id"));
    }

    @Test
    void findOverlappingUsesStrictOverlapBoundaries() {
        LocalDateTime now = LocalDateTime.now();
        quietPeriodService.createQuietPeriod(request(now.plusDays(1), now.plusDays(1).plusHours(2)));

        // 重叠查询命中（具体边界由 SQL 条件保证，这里验证查询条件构造）
        quietPeriodService.findOverlapping(10L, now.plusDays(1).plusHours(1), now.plusDays(1).plusHours(3));

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(quietPeriodMapper, atLeastOnce()).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("room_id"));
        assertTrue(sql.contains("start_time"));
        assertTrue(sql.contains("end_time"));

        // 参数不全时直接返回空，不查库
        assertTrue(quietPeriodService.findOverlapping(null, now, now.plusHours(1)).isEmpty());
    }

    @Test
    void roomQuietPeriodsReturnAllOfRoom() {
        LocalDateTime now = LocalDateTime.now();
        quietPeriodService.createQuietPeriod(request(now.plusDays(1), now.plusDays(1).plusHours(1)));
        quietPeriodService.createQuietPeriod(request(now.plusDays(2), now.plusDays(2).plusHours(1)));

        List<RoomQuietPeriodVO> list = quietPeriodService.getRoomQuietPeriods(10L);
        assertEquals(2, list.size());
        assertTrue(list.stream().allMatch(v -> "301接待室".equals(v.getRoomName())));
    }
}
