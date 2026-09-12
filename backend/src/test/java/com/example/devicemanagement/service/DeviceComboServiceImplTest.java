package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.devicemanagement.dto.request.ComboApplyRequest;
import com.example.devicemanagement.dto.request.DeviceComboRequest;
import com.example.devicemanagement.dto.response.ComboApplyResultVO;
import com.example.devicemanagement.dto.response.ComboRecordItemVO;
import com.example.devicemanagement.dto.response.DeviceComboVO;
import com.example.devicemanagement.entity.Device;
import com.example.devicemanagement.entity.DeviceCombo;
import com.example.devicemanagement.entity.DeviceComboItem;
import com.example.devicemanagement.entity.DeviceComboRecord;
import com.example.devicemanagement.entity.DeviceComboRecordItem;
import com.example.devicemanagement.entity.DeviceTransfer;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.mapper.DeviceComboItemMapper;
import com.example.devicemanagement.mapper.DeviceComboMapper;
import com.example.devicemanagement.mapper.DeviceComboRecordItemMapper;
import com.example.devicemanagement.mapper.DeviceComboRecordMapper;
import com.example.devicemanagement.mapper.DeviceMapper;
import com.example.devicemanagement.mapper.DeviceTransferMapper;
import com.example.devicemanagement.service.impl.DeviceComboServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceComboServiceImplTest {

    @Mock
    private DeviceComboMapper comboMapper;
    @Mock
    private DeviceComboItemMapper comboItemMapper;
    @Mock
    private DeviceComboRecordMapper recordMapper;
    @Mock
    private DeviceComboRecordItemMapper recordItemMapper;
    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private DeviceTransferMapper transferMapper;
    @Mock
    private FloorService floorService;
    @Mock
    private ReceptionRoomService roomService;
    @Mock
    private RoomActivityService roomActivityService;
    @Mock
    private RedisCacheService redisCacheService;

    private final ObjectMapper realObjectMapper = new ObjectMapper();

    private DeviceComboServiceImpl comboService;

    private Map<Long, Device> deviceStore;
    private List<DeviceComboItem> storedItems;
    private List<DeviceTransfer> storedTransfers;
    private DeviceComboRecord savedRecord;
    private List<DeviceComboRecordItem> savedRecordItems;

    private Floor floor;
    private ReceptionRoom room;
    private ReceptionRoom otherRoom;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, DeviceCombo.class);
        TableInfoHelper.initTableInfo(assistant, DeviceComboItem.class);
        TableInfoHelper.initTableInfo(assistant, DeviceComboRecord.class);
        TableInfoHelper.initTableInfo(assistant, DeviceComboRecordItem.class);
        TableInfoHelper.initTableInfo(assistant, Device.class);
        TableInfoHelper.initTableInfo(assistant, DeviceTransfer.class);
    }

    @BeforeEach
    void setUp() {
        // 注入真实 ObjectMapper（@InjectMocks 不会自动 new）
        comboService = new DeviceComboServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(comboService, "comboMapper", comboMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(comboService, "comboItemMapper", comboItemMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(comboService, "recordMapper", recordMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(comboService, "recordItemMapper", recordItemMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(comboService, "deviceMapper", deviceMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(comboService, "transferMapper", transferMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(comboService, "floorService", floorService);
        org.springframework.test.util.ReflectionTestUtils.setField(comboService, "roomService", roomService);
        org.springframework.test.util.ReflectionTestUtils.setField(comboService, "roomActivityService", roomActivityService);
        org.springframework.test.util.ReflectionTestUtils.setField(comboService, "redisCacheService", redisCacheService);
        org.springframework.test.util.ReflectionTestUtils.setField(comboService, "objectMapper", realObjectMapper);

        floor = new Floor();
        floor.setId(1L);
        floor.setFloorName("3F");

        room = new ReceptionRoom();
        room.setId(10L);
        room.setRoomName("301接待室");
        room.setRoomCode("R301");
        room.setFloorId(1L);
        room.setStatus(1);
        room.setEquipmentCount(0);

        otherRoom = new ReceptionRoom();
        otherRoom.setId(20L);
        otherRoom.setRoomName("302接待室");
        otherRoom.setRoomCode("R302");
        otherRoom.setFloorId(1L);
        otherRoom.setStatus(1);

        deviceStore = new HashMap<>();
        // 101 空闲正常、102 空闲正常；103 已在别的接待室；104 待修；105 损坏；106 被进行中活动占用
        deviceStore.put(101L, device(101L, "TV-001", "电视", null, 1));
        deviceStore.put(102L, device(102L, "SPK-001", "音响", null, 1));
        deviceStore.put(103L, device(103L, "MIC-001", "麦克风", 20L, 1));
        deviceStore.put(104L, device(104L, "MIC-002", "麦克风", null, 2));
        deviceStore.put(105L, device(105L, "PRJ-001", "投影仪", null, 0));
        deviceStore.put(106L, device(106L, "TV-002", "电视", null, 1));
        deviceStore.put(107L, device(107L, "TV-003", "电视", 10L, 1));

        storedItems = new ArrayList<>();
        storedTransfers = new ArrayList<>();
        savedRecord = null;
        savedRecordItems = new ArrayList<>();

        when(floorService.getAllFloors()).thenReturn(List.of(floor));
        when(floorService.getFloorById(1L)).thenReturn(floor);
        when(roomService.getAllRooms()).thenReturn(List.of(room, otherRoom));
        when(roomService.getRoomById(10L)).thenReturn(room);
        when(roomService.getRoomById(20L)).thenReturn(otherRoom);
        when(roomService.updateRoom(anyLong(), any())).thenReturn(room);

        // 进行中活动占用 106
        when(roomActivityService.getBlockingActivityName(106L)).thenReturn("重要接待");

        // deviceMapper.selectBatchIds 按传入 ID 从内存库取
        when(deviceMapper.selectBatchIds(any())).thenAnswer(inv -> {
            Iterable<Long> ids = inv.getArgument(0);
            List<Device> result = new ArrayList<>();
            for (Long id : ids) {
                Device d = deviceStore.get(id);
                if (d != null) {
                    result.add(d);
                }
            }
            return result;
        });
        when(deviceMapper.selectById(anyLong())).thenAnswer(inv -> deviceStore.get(inv.getArgument(0)));
        doAnswer(inv -> {
            Device d = inv.getArgument(0);
            deviceStore.put(d.getId(), d);
            return 1;
        }).when(deviceMapper).updateById(any());

        // 组合 / 明细
        when(comboMapper.selectCount(any())).thenReturn(0L);
        when(comboMapper.insert(any())).thenAnswer(inv -> {
            DeviceCombo c = inv.getArgument(0);
            c.setId(900L);
            return 1;
        });
        doAnswer(inv -> {
            DeviceComboItem item = inv.getArgument(0);
            item.setId((long) (storedItems.size() + 1));
            storedItems.add(item);
            return 1;
        }).when(comboItemMapper).insert(any());
        when(comboItemMapper.selectList(any())).thenAnswer(inv -> {
            String sql = inv.getArgument(0, Wrapper.class).getSqlSegment();
            if (sql != null && sql.contains("combo_id")) {
                return storedItems.stream()
                        .filter(i -> i.getComboId().equals(900L))
                        .collect(Collectors.toList());
            }
            return new ArrayList<>(storedItems);
        });
        doAnswer(inv -> 1).when(comboItemMapper).delete(any());

        when(transferMapper.insert(any())).thenAnswer(inv -> {
            storedTransfers.add(inv.getArgument(0));
            return 1;
        });

        // 套用记录
        when(recordMapper.insert(any())).thenAnswer(inv -> {
            savedRecord = inv.getArgument(0);
            savedRecord.setId(700L);
            return 1;
        });
        when(recordMapper.selectById(700L)).thenAnswer(inv -> savedRecord);
        doAnswer(inv -> {
            DeviceComboRecordItem item = inv.getArgument(0);
            item.setId((long) (savedRecordItems.size() + 1));
            item.setRecordId(700L);
            savedRecordItems.add(item);
            return 1;
        }).when(recordItemMapper).insert(any());
        when(recordItemMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(savedRecordItems));
    }

    @Test
    void createCombo_persistsOrderedDevices() {
        DeviceComboRequest request = new DeviceComboRequest();
        request.setComboName("贵宾接待标准组合");
        request.setCreatedBy("管理员");
        request.setDeviceIds(List.of(101L, 102L, 103L));

        DeviceComboComboMock comboMock = mockCombo();
        DeviceComboVO vo = comboService.createCombo(request);

        assertEquals("贵宾接待标准组合", vo.getComboName());
        assertEquals(3, vo.getDeviceCount());
        assertEquals(3, storedItems.size());
        assertEquals(101L, storedItems.get(0).getDeviceId());
        assertEquals("电视", vo.getDevices().get(0).getDeviceType());
        // 103 实时台账在 302
        assertEquals(20L, vo.getDevices().get(2).getCurrentRoomId());
        verify(comboMock.mapper, times(1)).insert(any());
    }

    @Test
    void createCombo_withoutDevices_rejected() {
        DeviceComboRequest request = new DeviceComboRequest();
        request.setComboName("空组合");
        request.setDeviceIds(List.of());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> comboService.createCombo(request));
        assertTrue(ex.getMessage().contains("至少选择一台"));
    }

    @Test
    void applyCombo_movesIdleAndSkipsOthersWithReasons() {
        seedCombo(101L, 102L, 103L, 104L, 105L, 106L, 107L);
        DeviceCombo combo = mockCombo().combo;

        ComboApplyRequest request = new ComboApplyRequest();
        request.setRoomId(10L);
        request.setOperator("值班员小王");

        // 设备清单查询：目标接待室（套用前仅 107）
        mockRoomDeviceListing();

        ComboApplyResultVO result = comboService.applyCombo(900L, request);

        assertEquals(7, result.getRequiredCount());
        assertEquals(2, result.getAppliedCount());   // 101、102 调入
        assertEquals(1, result.getPresentCount());   // 107 已在房间
        assertEquals(4, result.getSkippedCount());   // 103 别的房间 / 104 待修 / 105 损坏 / 106 活动占用

        // 只有空闲的两台写了流转记录并调入
        assertEquals(2, storedTransfers.size());
        assertEquals(10L, deviceStore.get(101L).getCurrentRoomId());
        assertEquals(10L, deviceStore.get(102L).getCurrentRoomId());
        assertEquals(20L, deviceStore.get(103L).getCurrentRoomId());
        assertNull(deviceStore.get(104L).getCurrentRoomId());

        Map<Long, ComboRecordItemVO> itemMap = result.getItems().stream()
                .collect(Collectors.toMap(ComboRecordItemVO::getDeviceId, v -> v));
        assertEquals("调入", itemMap.get(101L).getResultText());
        assertEquals("已在房间", itemMap.get(107L).getResultText());
        assertTrue(itemMap.get(103L).getSkipReason().contains("302接待室"));
        assertTrue(itemMap.get(104L).getSkipReason().contains("待修"));
        assertTrue(itemMap.get(105L).getSkipReason().contains("损坏"));
        assertTrue(itemMap.get(106L).getSkipReason().contains("重要接待"));

        // 套用前后清单：前1台、后3台
        assertEquals(1, result.getBeforeDevices().size());
        assertEquals(3, result.getAfterDevices().size());

        // 记录已落库
        assertNotNull(savedRecord);
        assertEquals(7, savedRecordItems.size());
        verify(roomService).updateRoom(eq(10L), argThat(req ->
                ((com.example.devicemanagement.dto.request.ReceptionRoomRequest) req).getEquipmentCount() == 3));
    }

    @Test
    void applyCombo_allSkippedStillPersistsRecord() {
        seedCombo(103L, 104L);
        mockCombo();
        mockRoomDeviceListing();

        ComboApplyRequest request = new ComboApplyRequest();
        request.setRoomId(10L);
        request.setOperator("值班员小王");

        ComboApplyResultVO result = comboService.applyCombo(900L, request);

        assertEquals(0, result.getAppliedCount());
        assertEquals(2, result.getSkippedCount());
        assertEquals(0, storedTransfers.size());
        assertNotNull(savedRecord);
        assertEquals(2, savedRecordItems.size());
    }

    @Test
    void applyCombo_disabledCombo_rejected() {
        seedCombo(101L);
        DeviceComboComboMock mock = mockCombo();
        mock.combo.setStatus(0);

        ComboApplyRequest request = new ComboApplyRequest();
        request.setRoomId(10L);
        request.setOperator("值班员");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> comboService.applyCombo(900L, request));
        assertTrue(ex.getMessage().contains("停用"));
    }

    @Test
    void getRecordById_snapshotAndLiveConsistency() {
        seedCombo(101L);
        mockCombo();
        mockRoomDeviceListing();
        ComboApplyRequest request = new ComboApplyRequest();
        request.setRoomId(10L);
        request.setOperator("值班员");
        comboService.applyCombo(900L, request);

        var vo = comboService.getRecordById(700L);
        assertEquals(1, vo.getAppliedCount());
        // 刷新后实时状态：101 仍在目标接待室
        ComboRecordItemVO item = vo.getItems().get(0);
        assertTrue(item.getExists());
        assertTrue(item.getLiveInTargetRoom());
        assertEquals("301接待室", item.getLiveRoomName());

        // 事后设备又被调走：实时信息应反映最新台账
        deviceStore.get(101L).setCurrentRoomId(20L);
        var vo2 = comboService.getRecordById(700L);
        assertFalse(vo2.getItems().get(0).getLiveInTargetRoom());
        assertEquals("302接待室", vo2.getItems().get(0).getLiveRoomName());
        // 快照清单不变：后清单仍是套用当时的 101
        assertEquals(1, vo2.getAfterDevices().size());
    }

    // ---------------- 辅助 ----------------

    private void seedCombo(Long... deviceIds) {
        int sort = 0;
        for (Long id : deviceIds) {
            Device d = deviceStore.get(id);
            DeviceComboItem item = new DeviceComboItem();
            item.setComboId(900L);
            item.setDeviceId(id);
            item.setDeviceCode(d.getDeviceCode());
            item.setDeviceName(d.getDeviceName());
            item.setDeviceType(d.getDeviceType());
            item.setSortOrder(sort++);
            storedItems.add(item);
        }
    }

    private DeviceComboComboMock mockCombo() {
        DeviceCombo combo = new DeviceCombo();
        combo.setId(900L);
        combo.setComboName("贵宾接待标准组合");
        combo.setStatus(1);
        when(comboMapper.selectById(900L)).thenReturn(combo);
        DeviceComboComboMock m = new DeviceComboComboMock();
        m.combo = combo;
        m.mapper = comboMapper;
        return m;
    }

    /**
     * 目标接待室 10 设备清单走 selectList（listRoomDevices）；其余 selectList 为组合明细。
     */
    @SuppressWarnings("unchecked")
    private void mockRoomDeviceListing() {
        when(deviceMapper.selectList(any())).thenAnswer(inv -> {
            String sql = inv.getArgument(0, Wrapper.class).getSqlSegment();
            if (sql != null && sql.contains("current_room_id")) {
                // 实时返回当前在 10 的设备：套用前仅 107，调入 101、102 后为三台
                return deviceStore.values().stream()
                        .filter(d -> room.getId().equals(d.getCurrentRoomId()))
                        .sorted((a, b) -> a.getDeviceCode().compareTo(b.getDeviceCode()))
                        .collect(Collectors.toList());
            }
            return new ArrayList<Device>();
        });
    }

    private Device device(Long id, String code, String type, Long roomId, int status) {
        Device d = new Device();
        d.setId(id);
        d.setDeviceCode(code);
        d.setDeviceName(code + "-名称");
        d.setDeviceType(type);
        d.setCurrentFloorId(1L);
        d.setCurrentRoomId(roomId);
        d.setStatus(status);
        return d;
    }

    private static class DeviceComboComboMock {
        DeviceCombo combo;
        DeviceComboMapper mapper;
    }
}
