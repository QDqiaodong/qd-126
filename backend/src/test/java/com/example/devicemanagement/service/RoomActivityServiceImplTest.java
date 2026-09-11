package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.example.devicemanagement.service.impl.RoomActivityServiceImpl;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomActivityServiceImplTest {

    @Mock
    private RoomActivityMapper activityMapper;

    @Mock
    private RoomActivityDeviceMapper activityDeviceMapper;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private FloorService floorService;

    @Mock
    private ReceptionRoomService roomService;

    @InjectMocks
    private RoomActivityServiceImpl activityService;

    private Floor floor;
    private ReceptionRoom room;
    private Device tv;
    private Device mic;
    private List<RoomActivity> storedActivities;
    private List<RoomActivityDevice> storedRows;
    private RoomActivity captured;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, RoomActivity.class);
        TableInfoHelper.initTableInfo(assistant, RoomActivityDevice.class);
        TableInfoHelper.initTableInfo(assistant, Device.class);
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

        tv = device(101L, "TV-001", "电视", 10L, 1);
        mic = device(102L, "MIC-001", "麦克风", 10L, 1);

        storedActivities = new ArrayList<>();
        storedRows = new ArrayList<>();
        captured = null;

        when(floorService.getAllFloors()).thenReturn(List.of(floor));
        when(roomService.getAllRooms()).thenReturn(List.of(room));
        when(roomService.getRoomById(10L)).thenReturn(room);

        when(activityMapper.insert(any())).thenAnswer(inv -> {
            RoomActivity a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(500L + storedActivities.size() + 1L);
            }
            captured = a;
            storedActivities.add(a);
            return 1;
        });
        doAnswer(inv -> 1).when(activityMapper).updateById(any());
        when(activityMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return storedActivities.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
        });

        // selectList 按 SQL 片段区分：未结束活动刷新 / 同接待室冲突 / 进行中占用查询
        when(activityMapper.selectList(any())).thenAnswer(inv -> {
            String sql = inv.getArgument(0, com.baomidou.mybatisplus.core.conditions.Wrapper.class).getSqlSegment();
            if (sql.contains("start_time")) {
                // 同接待室时段重叠查询：内存判定
                // 包装器参数无法直接拿到，由各用例用专门 mock 覆盖；默认空
                return new ArrayList<>();
            }
            if (sql.contains("status")) {
                // 进行中活动（占用状态接口）
                return storedActivities.stream()
                        .filter(a -> a.getStatus() != null && a.getStatus() == RoomActivity.STATUS_ONGOING)
                        .collect(java.util.stream.Collectors.toList());
            }
            // 未结束活动（状态推进）
            return storedActivities.stream()
                    .filter(a -> !Integer.valueOf(RoomActivity.STATUS_ENDED).equals(a.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
        });
        when(activityMapper.selectPage(any(Page.class), any())).thenAnswer(inv -> {
            Page<RoomActivity> page = inv.getArgument(0);
            List<RoomActivity> all = storedActivities.stream()
                    .sorted((x, y) -> y.getStartTime().compareTo(x.getStartTime()))
                    .collect(java.util.stream.Collectors.toList());
            page.setRecords(all);
            page.setTotal(all.size());
            return page;
        });
        when(activityMapper.selectBatchIds(any())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return storedActivities.stream().filter(a -> ids.contains(a.getId())).collect(java.util.stream.Collectors.toList());
        });

        when(activityDeviceMapper.insert(any())).thenAnswer(inv -> {
            RoomActivityDevice row = inv.getArgument(0);
            if (row.getId() == null) {
                row.setId((long) storedRows.size() + 1);
            }
            storedRows.add(row);
            return 1;
        });
        when(activityDeviceMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(storedRows));

        when(deviceMapper.selectBatchIds(any())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            List<Device> all = List.of(tv, mic);
            return all.stream().filter(d -> ids.contains(d.getId())).collect(java.util.stream.Collectors.toList());
        });
        when(deviceMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            if (tv.getId().equals(id)) return tv;
            if (mic.getId().equals(id)) return mic;
            return null;
        });
    }

    private Device device(Long id, String code, String type, Long roomId, int status) {
        Device d = new Device();
        d.setId(id);
        d.setDeviceCode(code);
        d.setDeviceName(code + "设备");
        d.setDeviceType(type);
        d.setCurrentFloorId(1L);
        d.setCurrentRoomId(roomId);
        d.setStatus(status);
        return d;
    }

    private RoomActivityCreateRequest request(LocalDateTime start, LocalDateTime end, Long... deviceIds) {
        RoomActivityCreateRequest req = new RoomActivityCreateRequest();
        req.setActivityName("客户参观");
        req.setRoomId(10L);
        req.setStartTime(start);
        req.setEndTime(end);
        req.setManager("张三");
        req.setDeviceIds(java.util.Arrays.asList(deviceIds));
        return req;
    }

    private void mockRoomOverlap(List<RoomActivity> conflicts) {
        when(activityMapper.selectList(any())).thenAnswer(inv -> {
            String sql = inv.getArgument(0, com.baomidou.mybatisplus.core.conditions.Wrapper.class).getSqlSegment();
            if (sql.contains("room_id")) {
                return new ArrayList<>(conflicts);
            }
            if (sql.contains("status")) {
                return storedActivities.stream()
                        .filter(a -> a.getStatus() != null && a.getStatus() == RoomActivity.STATUS_ONGOING)
                        .collect(java.util.stream.Collectors.toList());
            }
            return storedActivities.stream()
                    .filter(a -> !Integer.valueOf(RoomActivity.STATUS_ENDED).equals(a.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
        });
    }

    @Test
    void createActivityPersistsActivityAndDeviceOccupation() {
        LocalDateTime now = LocalDateTime.now();
        RoomActivityVO vo = activityService.createActivity(request(now.plusDays(1), now.plusDays(1).plusHours(2), 101L, 102L));

        assertNotNull(vo.getId());
        assertTrue(vo.getActivityNo().startsWith("HD"));
        assertEquals("客户参观", vo.getActivityName());
        assertEquals(10L, vo.getRoomId());
        assertEquals("301接待室", vo.getRoomName());
        assertEquals("3F", vo.getFloorName());
        assertEquals("张三", vo.getManager());
        assertEquals(RoomActivity.STATUS_PENDING, vo.getStatus());
        assertEquals("待开始", vo.getStatusText());
        assertEquals(2, vo.getDeviceCount());

        // 活动主表与占用明细均落库，明细带设备快照
        assertEquals(1, storedActivities.size());
        assertEquals(2, storedRows.size());
        assertEquals(101L, storedRows.get(0).getDeviceId());
        assertEquals("TV-001", storedRows.get(0).getDeviceCode());
        assertEquals("电视", storedRows.get(0).getDeviceType());
    }

    @Test
    void createActivityStartingNowMarkedOngoing() {
        LocalDateTime now = LocalDateTime.now();
        RoomActivityVO vo = activityService.createActivity(request(now.minusMinutes(5), now.plusHours(2), 101L));
        assertEquals(RoomActivity.STATUS_ONGOING, vo.getStatus());
        assertEquals("进行中", vo.getStatusText());
    }

    @Test
    void createRejectsMissingFields() {
        LocalDateTime now = LocalDateTime.now();
        RoomActivityCreateRequest noName = request(now.plusDays(1), now.plusDays(1).plusHours(1), 101L);
        noName.setActivityName("  ");
        assertThrows(IllegalArgumentException.class, () -> activityService.createActivity(noName));

        RoomActivityCreateRequest noRoom = request(now.plusDays(1), now.plusDays(1).plusHours(1), 101L);
        noRoom.setRoomId(null);
        assertThrows(IllegalArgumentException.class, () -> activityService.createActivity(noRoom));

        RoomActivityCreateRequest badTime = request(now.plusDays(1), now.plusDays(1), 101L);
        assertThrows(IllegalArgumentException.class, () -> activityService.createActivity(badTime));

        RoomActivityCreateRequest pastEnd = request(now.minusHours(2), now.minusHours(1), 101L);
        assertThrows(IllegalArgumentException.class, () -> activityService.createActivity(pastEnd));

        RoomActivityCreateRequest noManager = request(now.plusDays(1), now.plusDays(1).plusHours(1), 101L);
        noManager.setManager("  ");
        assertThrows(IllegalArgumentException.class, () -> activityService.createActivity(noManager));

        RoomActivityCreateRequest noDevice = request(now.plusDays(1), now.plusDays(1).plusHours(1));
        noDevice.setDeviceIds(List.of());
        assertThrows(IllegalArgumentException.class, () -> activityService.createActivity(noDevice));
    }

    @Test
    void createRejectsUnknownRoomAndDisabledRoom() {
        LocalDateTime now = LocalDateTime.now();
        RoomActivityCreateRequest req = request(now.plusDays(1), now.plusDays(1).plusHours(1), 101L);
        req.setRoomId(99L);
        when(roomService.getRoomById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> activityService.createActivity(req));

        room.setStatus(0);
        req.setRoomId(10L);
        assertThrows(IllegalArgumentException.class, () -> activityService.createActivity(req));
        room.setStatus(1);
    }

    @Test
    void createBlocksWhenSameRoomTimeOverlaps() {
        LocalDateTime now = LocalDateTime.now();
        RoomActivity other = new RoomActivity();
        other.setId(900L);
        other.setActivityName("原有会议");
        other.setRoomId(10L);
        other.setStatus(RoomActivity.STATUS_PENDING);
        other.setStartTime(now.plusDays(1).plusHours(1));
        other.setEndTime(now.plusDays(1).plusHours(3));
        mockRoomOverlap(List.of(other));

        RoomActivityCreateRequest req = request(now.plusDays(1).plusHours(2), now.plusDays(1).plusHours(4), 101L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> activityService.createActivity(req));
        assertTrue(ex.getMessage().contains("同一接待室时段冲突"));
        assertTrue(ex.getMessage().contains("原有会议"));
        assertEquals(0, storedActivities.size());
    }

    @Test
    void createRejectsDeviceNotInRoom() {
        LocalDateTime now = LocalDateTime.now();
        mic.setCurrentRoomId(20L);
        RoomActivityCreateRequest req = request(now.plusDays(1), now.plusDays(1).plusHours(1), 102L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> activityService.createActivity(req));
        assertTrue(ex.getMessage().contains("不在该接待室"));
    }

    @Test
    void createRejectsBrokenDevice() {
        LocalDateTime now = LocalDateTime.now();
        tv.setStatus(0);
        RoomActivityCreateRequest req = request(now.plusDays(1), now.plusDays(1).plusHours(1), 101L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> activityService.createActivity(req));
        assertTrue(ex.getMessage().contains("损坏"));
    }

    @Test
    void createBlocksDeviceOccupiedByOtherActivityInOverlap() {
        LocalDateTime now = LocalDateTime.now();
        // 其他接待室的活动已占用电视
        RoomActivity other = new RoomActivity();
        other.setId(901L);
        other.setActivityName("隔壁培训");
        other.setRoomId(20L);
        other.setStatus(RoomActivity.STATUS_PENDING);
        other.setStartTime(now.plusDays(1).plusHours(1));
        other.setEndTime(now.plusDays(1).plusHours(3));

        RoomActivityDevice otherRow = new RoomActivityDevice();
        otherRow.setId(9001L);
        otherRow.setActivityId(901L);
        otherRow.setDeviceId(101L);
        otherRow.setDeviceCode("TV-001");
        otherRow.setDeviceName("TV-001设备");
        otherRow.setDeviceType("电视");
        storedRows.add(otherRow);
        storedActivities.add(other);

        RoomActivityCreateRequest req = request(now.plusDays(1).plusHours(2), now.plusDays(1).plusHours(4), 101L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> activityService.createActivity(req));
        assertTrue(ex.getMessage().contains("隔壁培训"));
        assertEquals(1, storedActivities.size());
    }

    @Test
    void getActivityDetailShowsDevicesAndRoomConflictHints() {
        LocalDateTime now = LocalDateTime.now();
        activityService.createActivity(request(now.plusDays(1), now.plusDays(1).plusHours(2), 101L, 102L));
        Long activityId = captured.getId();

        // 另一活动与本接待室时段重叠（构造冲突提示）
        RoomActivity other = new RoomActivity();
        other.setId(910L);
        other.setActivityName("临时加场");
        other.setRoomId(10L);
        other.setStatus(RoomActivity.STATUS_PENDING);
        other.setStartTime(now.plusDays(1).plusHours(1));
        other.setEndTime(now.plusDays(1).plusHours(1).plusMinutes(30));
        when(activityMapper.selectList(any())).thenAnswer(inv -> {
            String sql = inv.getArgument(0, com.baomidou.mybatisplus.core.conditions.Wrapper.class).getSqlSegment();
            if (sql.contains("room_id")) {
                return new ArrayList<>(List.of(other));
            }
            if (sql.contains("status")) {
                return new ArrayList<>();
            }
            return new ArrayList<>();
        });

        RoomActivityVO detail = activityService.getActivityById(activityId);
        assertEquals(2, detail.getDevices().size());
        RoomActivityDeviceVO tvVo = detail.getDevices().stream()
                .filter(d -> d.getDeviceId() == 101L).findFirst().orElseThrow();
        assertEquals("301接待室", tvVo.getCurrentRoomName());
        assertNull(tvVo.getConflict());
        assertTrue(detail.getConflicts().stream().anyMatch(c -> c.contains("临时加场")));
    }

    @Test
    void detailFlagsDeviceMovedAwayAfterRefresh() {
        LocalDateTime now = LocalDateTime.now();
        activityService.createActivity(request(now.plusDays(1), now.plusDays(1).plusHours(2), 101L));
        Long activityId = captured.getId();

        // 刷新前设备被调配走，详情应给出冲突提示（刷新一致性）
        tv.setCurrentRoomId(20L);
        when(activityMapper.selectList(any())).thenReturn(new ArrayList<>());

        RoomActivityVO detail = activityService.getActivityById(activityId);
        RoomActivityDeviceVO tvVo = detail.getDevices().get(0);
        assertNotNull(tvVo.getConflict());
        assertTrue(tvVo.getConflict().contains("不在本接待室"));
        assertTrue(detail.getConflicts().get(0).contains("TV-001设备"));
    }

    @Test
    void queryingListAutoTransitionsStatusAndReleasesOccupation() {
        LocalDateTime now = LocalDateTime.now();
        // 待开始但已过结束时间：查询时自动结束并释放
        RoomActivity expiredPending = storedActivity(701L, "过期活动", RoomActivity.STATUS_PENDING,
                now.minusHours(3), now.minusHours(1));
        // 进行中但已过结束时间：自动结束
        RoomActivity expiredOngoing = storedActivity(702L, "该散场", RoomActivity.STATUS_ONGOING,
                now.minusHours(2), now.minusMinutes(30));
        // 待开始且已到点：自动进行中
        RoomActivity due = storedActivity(703L, "刚开场", RoomActivity.STATUS_PENDING,
                now.minusMinutes(10), now.plusHours(1));
        // 正常进行中
        RoomActivity ongoing = storedActivity(704L, "进行中活动", RoomActivity.STATUS_ONGOING,
                now.minusMinutes(20), now.plusHours(2));

        activityService.getActivitiesPage(1, 10, null, null, null);

        assertEquals(RoomActivity.STATUS_ENDED, expiredPending.getStatus());
        assertEquals(expiredPending.getEndTime(), expiredPending.getReleasedAt());
        assertEquals(RoomActivity.STATUS_ENDED, expiredOngoing.getStatus());
        assertEquals(RoomActivity.STATUS_ONGOING, due.getStatus());
        assertEquals(RoomActivity.STATUS_ONGOING, ongoing.getStatus());
    }

    @Test
    void filtersByDateFloorAndStatusPassedToWrapper() {
        LocalDateTime now = LocalDateTime.now();
        storedActivity(701L, "活动A", RoomActivity.STATUS_ONGOING, now, now.plusHours(2));

        var page = activityService.getActivitiesPage(2, 20, LocalDate.of(2026, 9, 11), 1L, 1);
        assertEquals(1, page.getRecords().size());

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(activityMapper).selectPage(any(Page.class), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("start_time"));
        assertTrue(sql.contains("floor_id"));
        assertTrue(sql.contains("status"));
    }

    @Test
    void finishActivityReleasesOccupationImmediately() {
        LocalDateTime now = LocalDateTime.now();
        activityService.createActivity(request(now.minusMinutes(10), now.plusHours(2), 101L));
        Long activityId = captured.getId();
        assertEquals(RoomActivity.STATUS_ONGOING, captured.getStatus());

        RoomActivityVO vo = activityService.finishActivity(activityId);
        assertEquals(RoomActivity.STATUS_ENDED, vo.getStatus());
        assertNotNull(captured.getReleasedAt());

        // 重复结束被拒绝
        assertThrows(IllegalArgumentException.class, () -> activityService.finishActivity(activityId));
    }

    @Test
    void roomOccupancyReflectsOnlyOngoingActivities() {
        LocalDateTime now = LocalDateTime.now();
        ReceptionRoom idleRoom = new ReceptionRoom();
        idleRoom.setId(20L);
        idleRoom.setRoomName("302接待室");
        idleRoom.setFloorId(1L);
        idleRoom.setStatus(1);
        when(roomService.getAllRooms()).thenReturn(List.of(room, idleRoom));

        storedActivity(801L, "占用中", RoomActivity.STATUS_ONGOING, now.minusHours(1), now.plusHours(1));
        RoomActivity pending = storedActivity(802L, "未开始", RoomActivity.STATUS_PENDING,
                now.plusDays(1), now.plusDays(1).plusHours(1));
        pending.setRoomId(20L);

        List<RoomOccupancyVO> list = activityService.getRoomOccupancies(null);
        RoomOccupancyVO r301 = list.stream().filter(v -> v.getRoomId() == 10L).findFirst().orElseThrow();
        RoomOccupancyVO r302 = list.stream().filter(v -> v.getRoomId() == 20L).findFirst().orElseThrow();
        assertTrue(r301.getOccupied());
        assertEquals("占用中", r301.getActivityName());
        assertFalse(r302.getOccupied());
        assertNull(r302.getActivityName());
    }

    @Test
    void assertDeviceTransferableBlocksOngoingButAllowsAfterEnd() {
        LocalDateTime now = LocalDateTime.now();
        RoomActivity ongoing = storedActivity(901L, "进行中占用", RoomActivity.STATUS_ONGOING,
                now.minusMinutes(30), now.plusHours(1));
        RoomActivityDevice row = new RoomActivityDevice();
        row.setActivityId(901L);
        row.setDeviceId(101L);
        row.setDeviceCode("TV-001");
        storedRows.add(row);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> activityService.assertDeviceTransferable(101L));
        assertTrue(ex.getMessage().contains("进行中"));

        // 活动到期后自动释放，调配放行
        ongoing.setEndTime(now.minusMinutes(1));
        assertDoesNotThrow(() -> activityService.assertDeviceTransferable(101L));
        assertEquals(RoomActivity.STATUS_ENDED, ongoing.getStatus());

        // 无任何占用记录的设备直接放行
        assertDoesNotThrow(() -> activityService.assertDeviceTransferable(999L));
    }

    private RoomActivity storedActivity(Long id, String name, int status, LocalDateTime start, LocalDateTime end) {
        RoomActivity a = new RoomActivity();
        a.setId(id);
        a.setActivityNo("HD" + id);
        a.setActivityName(name);
        a.setRoomId(10L);
        a.setFloorId(1L);
        a.setStatus(status);
        a.setStartTime(start);
        a.setEndTime(end);
        a.setManager("张三");
        storedActivities.add(a);
        return a;
    }
}
