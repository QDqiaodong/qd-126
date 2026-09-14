package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.devicemanagement.dto.request.InventoryBatchCreateRequest;
import com.example.devicemanagement.dto.request.InventoryItemCheckRequest;
import com.example.devicemanagement.dto.request.InventoryItemResolveRequest;
import com.example.devicemanagement.dto.request.InventoryTransferRequest;
import com.example.devicemanagement.dto.response.DeviceVO;
import com.example.devicemanagement.dto.response.InventoryBatchVO;
import com.example.devicemanagement.dto.response.InventoryItemVO;
import com.example.devicemanagement.dto.response.InventorySubmitResultVO;
import com.example.devicemanagement.entity.Device;
import com.example.devicemanagement.entity.Floor;
import com.example.devicemanagement.entity.InventoryBatch;
import com.example.devicemanagement.entity.InventoryItem;
import com.example.devicemanagement.entity.ReceptionRoom;
import com.example.devicemanagement.mapper.DeviceMapper;
import com.example.devicemanagement.mapper.InventoryBatchMapper;
import com.example.devicemanagement.mapper.InventoryItemMapper;
import com.example.devicemanagement.service.impl.InventoryServiceImpl;
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
class InventoryServiceImplTest {

    @Mock
    private InventoryBatchMapper batchMapper;

    @Mock
    private InventoryItemMapper itemMapper;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DeviceService deviceService;

    @Mock
    private FloorService floorService;

    @Mock
    private ReceptionRoomService roomService;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Floor floor;
    private ReceptionRoom room;
    private List<Device> floorDevices;
    /** 由 insert 模拟累积的明细，便于断言快照 */
    private List<InventoryItem> insertedItems;
    private InventoryBatch capturedBatch;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Device.class);
        TableInfoHelper.initTableInfo(assistant, InventoryBatch.class);
        TableInfoHelper.initTableInfo(assistant, InventoryItem.class);
    }

    @BeforeEach
    void setUp() {
        floor = new Floor();
        floor.setId(1L);
        floor.setFloorName("3F");

        room = new ReceptionRoom();
        room.setId(10L);
        room.setRoomName("301接待室");
        room.setFloorId(1L);

        Device d1 = device(101L, "TV-001", "电视", 1L, 10L);
        Device d2 = device(102L, "MIC-001", "麦克风", 1L, 10L);
        floorDevices = new ArrayList<>(List.of(d1, d2));

        insertedItems = new ArrayList<>();
        capturedBatch = null;

        when(floorService.getFloorById(1L)).thenReturn(floor);
        when(floorService.getAllFloors()).thenReturn(List.of(floor));
        when(roomService.getRoomById(10L)).thenReturn(room);
        when(roomService.getAllRooms()).thenReturn(List.of(room));

        // 按包装器实体表名区分设备查询与盘点明细查询
        when(deviceMapper.selectList(any())).thenReturn(floorDevices);
        when(deviceMapper.selectBatchIds(any())).thenReturn(floorDevices);

        // 默认无进行中的重叠批次
        when(batchMapper.selectCount(any())).thenReturn(0L);

        when(batchMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            if (capturedBatch != null && capturedBatch.getId().equals(id)) {
                return capturedBatch;
            }
            return null;
        });
        doAnswer(inv -> {
            InventoryBatch batch = inv.getArgument(0);
            if (batch.getId() == null) {
                batch.setId(500L);
            }
            capturedBatch = batch;
            return 1;
        }).when(batchMapper).insert(any());
        doAnswer(inv -> 1).when(batchMapper).updateById(any());

        doAnswer(inv -> {
            InventoryItem item = inv.getArgument(0);
            if (item.getId() == null) {
                item.setId((long) (insertedItems.size() + 1));
            }
            insertedItems.add(item);
            return 1;
        }).when(itemMapper).insert(any());
        doAnswer(inv -> 1).when(itemMapper).updateById(any());
        when(itemMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(insertedItems));
        when(itemMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return insertedItems.stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);
        });
        when(itemMapper.selectCount(any())).thenAnswer(inv ->
                insertedItems.stream().filter(i -> i.getProcessStatus() != null
                        && i.getProcessStatus() == InventoryItem.PROCESS_PENDING).count());
    }

    private Device device(Long id, String code, String type, Long floorId, Long roomId) {
        Device d = new Device();
        d.setId(id);
        d.setDeviceCode(code);
        d.setDeviceName(code + "名称");
        d.setDeviceType(type);
        d.setCurrentFloorId(floorId);
        d.setCurrentRoomId(roomId);
        d.setStatus(1);
        return d;
    }

    private InventoryBatchCreateRequest floorRequest() {
        InventoryBatchCreateRequest req = new InventoryBatchCreateRequest();
        req.setScopeType("FLOOR");
        req.setFloorId(1L);
        req.setOperator("张三");
        return req;
    }

    @Test
    void createBatchByFloorGeneratesSnapshotOfAllDevicesInFloor() {
        InventoryBatchVO vo = inventoryService.createBatch(floorRequest());

        assertNotNull(vo.getId());
        assertTrue(vo.getBatchNo().startsWith("PD"));
        assertEquals("FLOOR", vo.getScopeType());
        assertEquals("3F", vo.getFloorName());
        assertEquals(2, vo.getTotalCount());
        assertEquals(0, vo.getCheckedCount());
        assertEquals(0, vo.getProgressPercent());
        assertEquals(InventoryBatch.STATUS_COUNTING, vo.getStatus());

        // 同一批次设备不可重复：唯一键 + 每个设备恰好一条快照
        assertEquals(2, insertedItems.size());
        assertEquals(List.of(101L, 102L),
                insertedItems.stream().map(InventoryItem::getDeviceId).toList());
        assertEquals(1L, insertedItems.get(0).getSnapshotFloorId());
        assertEquals(10L, insertedItems.get(0).getSnapshotRoomId());
    }

    @Test
    void createBatchRejectsEmptyScopeAndMissingOperator() {
        InventoryBatchCreateRequest noOperator = floorRequest();
        noOperator.setOperator("  ");
        assertThrows(IllegalArgumentException.class, () -> inventoryService.createBatch(noOperator));

        InventoryBatchCreateRequest roomWithoutId = new InventoryBatchCreateRequest();
        roomWithoutId.setScopeType("ROOM");
        roomWithoutId.setOperator("张三");
        assertThrows(IllegalArgumentException.class, () -> inventoryService.createBatch(roomWithoutId));

        InventoryBatchCreateRequest badType = floorRequest();
        badType.setScopeType("BUILDING");
        assertThrows(IllegalArgumentException.class, () -> inventoryService.createBatch(badType));
    }

    @Test
    void createBatchRejectsScopeWithoutDevices() {
        when(deviceMapper.selectList(any())).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class, () -> inventoryService.createBatch(floorRequest()));
    }

    @Test
    void createBatchRejectsWhenAnActiveCountingBatchOverlapsScope() {
        when(batchMapper.selectCount(any())).thenReturn(1L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> inventoryService.createBatch(floorRequest()));
        assertTrue(ex.getMessage().contains("盘点中"));
    }

    @Test
    void checkItemUpdatesResultAndProgressButRejectsNonCountingBatch() {
        InventoryBatchVO created = inventoryService.createBatch(floorRequest());
        Long batchId = created.getId();

        InventoryItemCheckRequest req = new InventoryItemCheckRequest();
        req.setCheckResult(InventoryItem.RESULT_PRESENT);
        req.setRemark("正常");
        InventoryItemVO first = inventoryService.checkItem(batchId, insertedItems.get(0).getId(), req);
        assertEquals("在场", first.getCheckResultText());
        assertEquals(1, capturedBatch.getCheckedCount());
        assertEquals(1, capturedBatch.getPresentCount());

        // 提交批次
        markAll(InventoryItem.RESULT_PRESENT);
        InventorySubmitResultVO result = inventoryService.submitBatch(batchId);
        assertEquals(InventoryBatch.STATUS_SUBMITTED, capturedBatch.getStatus());

        // 已提交批次不可再修改盘点结果
        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.checkItem(batchId, insertedItems.get(0).getId(), req));
    }

    @Test
    void submitRejectsWhenSomeDevicesUnchecked() {
        InventoryBatchVO created = inventoryService.createBatch(floorRequest());
        // 仅盘点一台
        InventoryItemCheckRequest req = new InventoryItemCheckRequest();
        req.setCheckResult(InventoryItem.RESULT_PRESENT);
        inventoryService.checkItem(created.getId(), insertedItems.get(0).getId(), req);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> inventoryService.submitBatch(created.getId()));
        assertTrue(ex.getMessage().contains("1"));
        assertEquals(InventoryBatch.STATUS_COUNTING, capturedBatch.getStatus());
    }

    @Test
    void submitFreezesLedgerAndProducesStableDifferenceList() {
        InventoryBatchVO created = inventoryService.createBatch(floorRequest());
        Long batchId = created.getId();

        // 设备 102 在盘点期间被调配到别的房间（台账变化）
        Device moved = floorDevices.get(1);
        moved.setCurrentFloorId(2L);
        moved.setCurrentRoomId(20L);

        InventoryItemCheckRequest missing = new InventoryItemCheckRequest();
        missing.setCheckResult(InventoryItem.RESULT_MISSING);
        InventoryItemCheckRequest mismatch = new InventoryItemCheckRequest();
        mismatch.setCheckResult(InventoryItem.RESULT_MISMATCH);

        inventoryService.checkItem(batchId, insertedItems.get(0).getId(), missing);
        inventoryService.checkItem(batchId, insertedItems.get(1).getId(), mismatch);

        InventorySubmitResultVO result = inventoryService.submitBatch(batchId);

        // 差异清单包含缺失与位置不符，不含在场
        assertEquals(2, result.getDiffItems().size());
        assertEquals(1, result.getMissingCount());
        assertEquals(1, result.getMismatchCount());

        InventoryItem frozenMismatch = insertedItems.get(1);
        assertEquals(2L, frozenMismatch.getLedgerFloorId());
        assertEquals(20L, frozenMismatch.getLedgerRoomId());
        assertEquals(1, frozenMismatch.getLocationMismatch());
        assertEquals(InventoryItem.PROCESS_PENDING, frozenMismatch.getProcessStatus());

        InventoryItem frozenMissing = insertedItems.get(0);
        assertEquals(InventoryItem.PROCESS_PENDING, frozenMissing.getProcessStatus());

        // 待处理差异数
        assertEquals(2, result.getBatch().getPendingDiffCount());

        // 提交后台账再次变化，差异仍以提交时冻结基准为准（刷新一致性）
        moved.setCurrentFloorId(3L);
        moved.setCurrentRoomId(30L);
        List<InventoryItemVO> items = inventoryService.getBatchItems(batchId, null);
        InventoryItemVO stillMismatch = items.stream()
                .filter(i -> i.getDeviceId() == 102L).findFirst().orElseThrow();
        assertTrue(stillMismatch.getLocationMismatch());
        assertEquals(2L, stillMismatch.getLedgerFloorId());
    }

    @Test
    void transferItemDelegatesToDeviceTransferAndMarksProcessed() {
        InventoryBatchVO created = inventoryService.createBatch(floorRequest());
        Long batchId = created.getId();

        // 101 在场，102 位置不符
        InventoryItemCheckRequest present = new InventoryItemCheckRequest();
        present.setCheckResult(InventoryItem.RESULT_PRESENT);
        InventoryItemCheckRequest mismatch = new InventoryItemCheckRequest();
        mismatch.setCheckResult(InventoryItem.RESULT_MISMATCH);
        inventoryService.checkItem(batchId, insertedItems.get(0).getId(), present);
        inventoryService.checkItem(batchId, insertedItems.get(1).getId(), mismatch);

        inventoryService.submitBatch(batchId);
        Long itemId = insertedItems.get(1).getId();

        DeviceVO afterTransfer = new DeviceVO();
        afterTransfer.setId(102L);
        afterTransfer.setCurrentFloorId(1L);
        afterTransfer.setCurrentRoomId(10L);
        when(deviceService.transferDevice(any())).thenReturn(afterTransfer);

        InventoryTransferRequest transferReq = new InventoryTransferRequest();
        transferReq.setToFloorId(1L);
        transferReq.setToRoomId(10L);
        transferReq.setOperator("李四");
        InventoryItemVO vo = inventoryService.transferItem(batchId, itemId, transferReq);

        ArgumentCaptor<com.example.devicemanagement.dto.request.DeviceTransferRequest> captor =
                ArgumentCaptor.forClass(com.example.devicemanagement.dto.request.DeviceTransferRequest.class);
        verify(deviceService).transferDevice(captor.capture());
        assertEquals(102L, captor.getValue().getDeviceId());

        assertEquals(InventoryItem.PROCESS_DONE, vo.getProcessStatus());
        assertFalse(vo.getLocationMismatch());
        assertNotNull(insertedItems.get(1).getProcessedAt());
    }

    @Test
    void resolveMissingKeepsProcessTraceAndTransferRejectsAfterClose() {
        InventoryBatchVO created = inventoryService.createBatch(floorRequest());
        Long batchId = created.getId();

        InventoryItemCheckRequest missing = new InventoryItemCheckRequest();
        missing.setCheckResult(InventoryItem.RESULT_MISSING);
        InventoryItemCheckRequest present = new InventoryItemCheckRequest();
        present.setCheckResult(InventoryItem.RESULT_PRESENT);
        inventoryService.checkItem(batchId, insertedItems.get(0).getId(), missing);
        inventoryService.checkItem(batchId, insertedItems.get(1).getId(), present);
        inventoryService.submitBatch(batchId);

        Long missingItemId = insertedItems.get(0).getId();
        InventoryItemResolveRequest resolveReq = new InventoryItemResolveRequest();
        resolveReq.setProcessRemark("设备已找回");
        InventoryItemVO resolved = inventoryService.resolveItem(batchId, missingItemId, resolveReq);
        assertEquals(InventoryItem.PROCESS_DONE, resolved.getProcessStatus());
        assertEquals("设备已找回", resolved.getProcessRemark());

        // 重复登记被拒绝
        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.resolveItem(batchId, missingItemId, resolveReq));

        // 关闭后所有修改被拒绝
        InventoryBatchVO closed = inventoryService.closeBatch(batchId);
        assertEquals(InventoryBatch.STATUS_CLOSED, closed.getStatus());

        InventoryTransferRequest transferReq = new InventoryTransferRequest();
        transferReq.setToFloorId(1L);
        transferReq.setToRoomId(10L);
        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.transferItem(batchId, insertedItems.get(1).getId(), transferReq));
        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.resolveItem(batchId, missingItemId, resolveReq));
    }

    @Test
    void closeAllowedDirectlyFromCountingAndReleasesUncheckedDevices() {
        InventoryBatchVO created = inventoryService.createBatch(floorRequest());
        Long batchId = created.getId();

        // 仅盘点第一台，第二台保持未盘
        InventoryItemCheckRequest present = new InventoryItemCheckRequest();
        present.setCheckResult(InventoryItem.RESULT_PRESENT);
        inventoryService.checkItem(batchId, insertedItems.get(0).getId(), present);

        Device unchecked = floorDevices.get(1);
        Long floorBefore = unchecked.getCurrentFloorId();
        Long roomBefore = unchecked.getCurrentRoomId();
        Integer statusBefore = unchecked.getStatus();

        // 盘点中可直接关闭，批次结束
        InventoryBatchVO closed = inventoryService.closeBatch(batchId);
        assertEquals(InventoryBatch.STATUS_CLOSED, closed.getStatus());
        assertNotNull(capturedBatch.getClosedAt());

        // 未盘设备台账不被盘点流程改动：仍在原房间、状态正常（在库可调配）
        assertEquals(floorBefore, unchecked.getCurrentFloorId());
        assertEquals(roomBefore, unchecked.getCurrentRoomId());
        assertEquals(statusBefore, unchecked.getStatus());
        assertNull(insertedItems.get(1).getCheckResult());
        assertNull(insertedItems.get(1).getProcessStatus());

        // 关闭后所有修改仍被拒绝
        InventoryItemCheckRequest checkReq = new InventoryItemCheckRequest();
        checkReq.setCheckResult(InventoryItem.RESULT_PRESENT);
        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.checkItem(batchId, insertedItems.get(1).getId(), checkReq));

        // 重复关闭被拒绝
        assertThrows(IllegalArgumentException.class, () -> inventoryService.closeBatch(batchId));
    }

    private void markAll(int result) {
        for (InventoryItem item : insertedItems) {
            item.setCheckResult(result);
        }
    }
}
