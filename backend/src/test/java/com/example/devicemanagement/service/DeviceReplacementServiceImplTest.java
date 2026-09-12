package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.DeviceReplacementRequest;
import com.example.devicemanagement.dto.request.ReplacementResultRequest;
import com.example.devicemanagement.dto.response.DeviceReplacementVO;
import com.example.devicemanagement.dto.response.SpareDeviceVO;
import com.example.devicemanagement.entity.Device;
import com.example.devicemanagement.entity.DeviceReplacement;
import com.example.devicemanagement.entity.DeviceTransfer;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.entity.RoomActivity;
import com.example.devicemanagement.entity.RoomActivityDevice;
import com.example.devicemanagement.mapper.DeviceMapper;
import com.example.devicemanagement.mapper.DeviceReplacementMapper;
import com.example.devicemanagement.mapper.DeviceTransferMapper;
import com.example.devicemanagement.mapper.RoomActivityDeviceMapper;
import com.example.devicemanagement.mapper.RoomActivityMapper;
import com.example.devicemanagement.service.impl.DeviceReplacementServiceImpl;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceReplacementServiceImplTest {

    @Mock
    private DeviceReplacementMapper replacementMapper;
    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private DeviceTransferMapper transferMapper;
    @Mock
    private RoomActivityMapper activityMapper;
    @Mock
    private RoomActivityDeviceMapper activityDeviceMapper;
    @Mock
    private FloorService floorService;
    @Mock
    private ReceptionRoomService roomService;
    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private DeviceReplacementServiceImpl replacementService;

    private Floor floor;
    private ReceptionRoom room;
    private Device faulty;
    private Device spare;
    private List<DeviceReplacement> stored;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Device.class);
        TableInfoHelper.initTableInfo(assistant, DeviceReplacement.class);
        TableInfoHelper.initTableInfo(assistant, DeviceTransfer.class);
        TableInfoHelper.initTableInfo(assistant, RoomActivity.class);
        TableInfoHelper.initTableInfo(assistant, RoomActivityDevice.class);
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
        room.setEquipmentCount(3);

        faulty = device(101L, "MIC-001", "麦克风", 1L, 10L, 1);
        spare = device(201L, "MIC-B1", "备用麦克风", 1L, null, 1);

        stored = new ArrayList<>();

        when(floorService.getFloorById(1L)).thenReturn(floor);
        when(roomService.getRoomById(10L)).thenReturn(room);
        when(deviceMapper.selectById(101L)).thenReturn(faulty);
        when(deviceMapper.selectById(201L)).thenReturn(spare);

        when(replacementMapper.insert(any())).thenAnswer(inv -> {
            DeviceReplacement r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(900L + stored.size() + 1L);
            }
            stored.add(r);
            return 1;
        });
        when(replacementMapper.selectById(anyLong())).thenAnswer(inv ->
                stored.stream().filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));

        // 默认无活动占用行、接待室设备数与台账一致
        when(activityDeviceMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectCount(any())).thenReturn(3L);
    }

    private Device device(Long id, String code, String type, Long floorId, Long roomId, int status) {
        Device d = new Device();
        d.setId(id);
        d.setDeviceCode(code);
        d.setDeviceName(code + "-名称");
        d.setDeviceType(type);
        d.setBrand("品牌X");
        d.setModel("型号Y");
        d.setCurrentFloorId(floorId);
        d.setCurrentRoomId(roomId);
        d.setStatus(status);
        return d;
    }

    private DeviceReplacementRequest request() {
        DeviceReplacementRequest req = new DeviceReplacementRequest();
        req.setFaultyDeviceId(101L);
        req.setSpareDeviceId(201L);
        req.setFaultPhenomenon("  发言时无声音  ");
        req.setOperator("  值班员甲 ");
        req.setRemark("接待中紧急处理");
        return req;
    }

    @Test
    void createReplacement_swapsDevicesAndWritesRecord() {
        DeviceReplacementVO vo = replacementService.createReplacement(request());

        // 原设备转待维修并卸下接待室
        assertEquals(2, faulty.getStatus());
        assertNull(faulty.getCurrentRoomId());
        // 备用机转入该接待室
        assertEquals(10L, spare.getCurrentRoomId());
        assertEquals(1L, spare.getCurrentFloorId());

        // 替换记录快照
        assertEquals(1, stored.size());
        DeviceReplacement saved = stored.get(0);
        assertEquals(10L, saved.getRoomId());
        assertEquals(1L, saved.getFloorId());
        assertEquals("MIC-001", saved.getFaultyDeviceCode());
        assertEquals("MIC-B1", saved.getSpareDeviceCode());
        assertEquals("发言时无声音", saved.getFaultPhenomenon());
        assertEquals("值班员甲", saved.getOperator());
        assertEquals(DeviceReplacement.RESULT_PENDING_REPAIR, saved.getProcessResult());

        // 两条流转台账：故障机卸下 + 备用机调入
        ArgumentCaptor<DeviceTransfer> transferCaptor = ArgumentCaptor.forClass(DeviceTransfer.class);
        verify(transferMapper, times(2)).insert(transferCaptor.capture());
        List<DeviceTransfer> transfers = transferCaptor.getAllValues();
        assertEquals(101L, transfers.get(0).getDeviceId());
        assertNull(transfers.get(0).getToRoomId());
        assertEquals(201L, transfers.get(1).getDeviceId());
        assertEquals(10L, transfers.get(1).getToRoomId());

        // 返回实时状态一致
        assertEquals("待维修", vo.getFaultyDeviceStatusText());
        assertNull(vo.getFaultyDeviceRoomId());
        assertEquals("301接待室", vo.getSpareDeviceRoomName());
        assertEquals("正常", vo.getSpareDeviceStatusText());
    }

    @Test
    void createReplacement_swapsOngoingActivityDeviceRow() {
        RoomActivity activity = new RoomActivity();
        activity.setId(500L);
        activity.setRoomId(10L);
        activity.setStatus(RoomActivity.STATUS_ONGOING);
        when(activityMapper.selectBatchIds(any())).thenReturn(List.of(activity));

        RoomActivityDevice row = new RoomActivityDevice();
        row.setId(1L);
        row.setActivityId(500L);
        row.setDeviceId(101L);
        row.setDeviceCode("MIC-001");
        row.setDeviceName("MIC-001-名称");
        row.setDeviceType("麦克风");
        when(activityDeviceMapper.selectList(any())).thenAnswer(inv -> {
            String sql = inv.getArgument(0, Wrapper.class).getSqlSegment();
            if (sql.contains("device_id")) {
                return List.of(row);
            }
            return List.of();
        });
        when(activityDeviceMapper.selectCount(any())).thenReturn(0L);

        replacementService.createReplacement(request());

        // 活动占用明细由故障机改为备用机
        assertEquals(201L, row.getDeviceId());
        assertEquals("MIC-B1", row.getDeviceCode());
        verify(activityDeviceMapper).updateById(row);
        verify(activityDeviceMapper, never()).deleteById(anyLong());
    }

    @Test
    void createReplacement_rejectsWhenFaultyDeviceNotInRoom() {
        faulty.setCurrentRoomId(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> replacementService.createReplacement(request()));
        assertTrue(ex.getMessage().contains("不在任何接待室"));
        verify(replacementMapper, never()).insert(any());
    }

    @Test
    void createReplacement_rejectsSpareFromDifferentFloor() {
        spare.setCurrentFloorId(2L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> replacementService.createReplacement(request()));
        assertTrue(ex.getMessage().contains("同一楼层"));
        verify(replacementMapper, never()).insert(any());
    }

    @Test
    void createReplacement_rejectsSpareAlreadyAssignedToRoom() {
        spare.setCurrentRoomId(99L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> replacementService.createReplacement(request()));
        assertTrue(ex.getMessage().contains("已分配接待室"));
    }

    @Test
    void createReplacement_rejectsAbnormalSpare() {
        spare.setStatus(2);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> replacementService.createReplacement(request()));
        assertTrue(ex.getMessage().contains("不是正常"));
    }

    @Test
    void createReplacement_rejectsBlankPhenomenonOrOperator() {
        DeviceReplacementRequest noPhenomenon = request();
        noPhenomenon.setFaultPhenomenon("  ");
        assertThrows(IllegalArgumentException.class,
                () -> replacementService.createReplacement(noPhenomenon));

        DeviceReplacementRequest noOperator = request();
        noOperator.setOperator(null);
        assertThrows(IllegalArgumentException.class,
                () -> replacementService.createReplacement(noOperator));
        verify(replacementMapper, never()).insert(any());
    }

    @Test
    void getSpares_onlyReturnsSameFloorNormalUnassignedAndSortsSameTypeFirst() {
        Device otherType = device(202L, "TV-B1", "电视", 1L, null, 1);
        device(203L, "MIC-B2", "麦克风", 1L, 11L, 1);   // 已分配接待室，数据库不会返回
        device(204L, "MIC-B3", "麦克风", 1L, null, 0);  // 状态异常，数据库不会返回
        device(205L, "MIC-B4", "麦克风", 2L, null, 1);  // 其他楼层，数据库不会返回
        Device sameType = device(206L, "MIC-B5", "麦克风", 1L, null, 1);

        // 模拟数据库按 楼层=1 且 接待室为空 且 状态正常 过滤后的结果（device_type,device_code 升序）
        when(deviceMapper.selectList(any())).thenReturn(List.of(sameType, otherType));

        List<SpareDeviceVO> spares = replacementService.getSpares(101L);

        // 同型号同品牌（品牌X/型号Y 与故障机一致）排最前
        assertEquals(2, spares.size());
        assertEquals(206L, spares.get(0).getId());
        assertTrue(spares.get(0).getSameType());
        assertEquals(202L, spares.get(1).getId());
        assertFalse(spares.get(1).getSameDeviceType());
    }

    @Test
    void getReplacementsPage_appliesFilters() {
        DeviceReplacement record = new DeviceReplacement();
        record.setId(1L);
        record.setRoomId(10L);
        record.setFloorId(1L);
        record.setFaultyDeviceId(101L);
        record.setSpareDeviceId(201L);
        record.setFaultyDeviceCode("MIC-001");
        record.setFaultyDeviceName("故障麦");
        record.setFaultyDeviceType("麦克风");
        record.setSpareDeviceCode("MIC-B1");
        record.setSpareDeviceName("备用麦");
        record.setSpareDeviceType("麦克风");
        record.setFaultPhenomenon("无声");
        record.setOperator("值班员甲");
        record.setProcessResult(DeviceReplacement.RESULT_PENDING_REPAIR);
        Page<DeviceReplacement> mapperPage = new Page<>(1, 10);
        mapperPage.setRecords(List.of(record));
        mapperPage.setTotal(1);
        when(replacementMapper.selectPage(any(Page.class), any())).thenReturn(mapperPage);

        var page = replacementService.getReplacementsPage(1, 10, 1L, 10L, 1);

        assertEquals(1L, page.getTotal());
        assertEquals("301接待室", page.getRecords().get(0).getRoomName());
        assertEquals("待维修", page.getRecords().get(0).getProcessResultText());

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DeviceReplacement>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(replacementMapper).selectPage(any(Page.class), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("floor_id"));
        assertTrue(sql.contains("room_id"));
        assertTrue(sql.contains("process_result"));
    }

    @Test
    void resolveResult_repaired_setsDeviceNormalAndRejectsDuplicate() {
        replacementService.createReplacement(request());
        Long id = stored.get(0).getId();

        ReplacementResultRequest resolveReq = new ReplacementResultRequest();
        resolveReq.setProcessResult(DeviceReplacement.RESULT_REPAIRED);
        resolveReq.setProcessRemark("更换音头后测试正常");
        resolveReq.setProcessedBy("维修员乙");

        DeviceReplacementVO vo = replacementService.resolveResult(id, resolveReq);

        assertEquals(DeviceReplacement.RESULT_REPAIRED, vo.getProcessResult());
        assertEquals("已修复", vo.getProcessResultText());
        // 设备恢复正常但仍未分配接待室，可再次成为备用机
        assertEquals(1, faulty.getStatus());
        assertNull(faulty.getCurrentRoomId());

        // 不可重复登记
        assertThrows(IllegalArgumentException.class,
                () -> replacementService.resolveResult(id, resolveReq));
    }

    @Test
    void resolveResult_scrapped_setsDeviceBroken() {
        replacementService.createReplacement(request());
        Long id = stored.get(0).getId();

        ReplacementResultRequest resolveReq = new ReplacementResultRequest();
        resolveReq.setProcessResult(DeviceReplacement.RESULT_SCRAPPED);
        resolveReq.setProcessRemark("无维修价值");
        resolveReq.setProcessedBy("维修员乙");

        replacementService.resolveResult(id, resolveReq);

        assertEquals(0, faulty.getStatus());
    }

    @Test
    void resolveResult_rejectsInvalidResultAndBlankFields() {
        replacementService.createReplacement(request());
        Long id = stored.get(0).getId();

        ReplacementResultRequest invalid = new ReplacementResultRequest();
        invalid.setProcessResult(9);
        invalid.setProcessRemark("x");
        invalid.setProcessedBy("y");
        assertThrows(IllegalArgumentException.class,
                () -> replacementService.resolveResult(id, invalid));

        ReplacementResultRequest blank = new ReplacementResultRequest();
        blank.setProcessResult(DeviceReplacement.RESULT_REPAIRED);
        blank.setProcessRemark("  ");
        blank.setProcessedBy("y");
        assertThrows(IllegalArgumentException.class,
                () -> replacementService.resolveResult(id, blank));
    }
}
