package com.example.devicemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.DeviceTransferRequest;
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
import com.example.devicemanagement.service.DeviceService;
import com.example.devicemanagement.service.FloorService;
import com.example.devicemanagement.service.InventoryService;
import com.example.devicemanagement.service.ReceptionRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private InventoryBatchMapper batchMapper;

    @Autowired
    private InventoryItemMapper itemMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private FloorService floorService;

    @Autowired
    private ReceptionRoomService roomService;

    @Override
    @Transactional
    public InventoryBatchVO createBatch(InventoryBatchCreateRequest request) {
        if (request.getScopeType() == null
                || (!InventoryBatch.SCOPE_FLOOR.equals(request.getScopeType())
                && !InventoryBatch.SCOPE_ROOM.equals(request.getScopeType()))) {
            throw new IllegalArgumentException("盘点范围类型必须为楼层或接待室");
        }
        if (request.getOperator() == null || request.getOperator().trim().isEmpty()) {
            throw new IllegalArgumentException("盘点负责人不能为空");
        }

        Long floorId;
        Long roomId = null;
        String scopeName;

        if (InventoryBatch.SCOPE_ROOM.equals(request.getScopeType())) {
            if (request.getRoomId() == null) {
                throw new IllegalArgumentException("按接待室盘点时必须选择接待室");
            }
            ReceptionRoom room = roomService.getRoomById(request.getRoomId());
            if (room == null) {
                throw new IllegalArgumentException("所选接待室不存在");
            }
            roomId = room.getId();
            floorId = room.getFloorId();
            scopeName = room.getRoomName();
        } else {
            if (request.getFloorId() == null) {
                throw new IllegalArgumentException("按楼层盘点时必须选择楼层");
            }
            Floor floor = floorService.getFloorById(request.getFloorId());
            if (floor == null) {
                throw new IllegalArgumentException("所选楼层不存在");
            }
            floorId = floor.getId();
            scopeName = floor.getFloorName();
        }

        // 生成创建时刻的设备快照
        LambdaQueryWrapper<Device> deviceWrapper = new LambdaQueryWrapper<>();
        if (roomId != null) {
            deviceWrapper.eq(Device::getCurrentRoomId, roomId);
        } else {
            deviceWrapper.eq(Device::getCurrentFloorId, floorId);
        }
        deviceWrapper.orderByAsc(Device::getDeviceCode);
        List<Device> devices = deviceMapper.selectList(deviceWrapper);
        if (devices.isEmpty()) {
            throw new IllegalArgumentException("该范围内暂无可盘点设备，无法创建批次");
        }

        // 同一进行中的批次内设备不可重复：拒绝为重叠范围再建盘点中批次（DB 唯一键兜底）
        LambdaQueryWrapper<InventoryBatch> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(InventoryBatch::getStatus, InventoryBatch.STATUS_COUNTING);
        if (roomId != null) {
            activeWrapper.eq(InventoryBatch::getRoomId, roomId);
        } else {
            activeWrapper.eq(InventoryBatch::getFloorId, floorId);
        }
        Long activeCount = batchMapper.selectCount(activeWrapper);
        if (activeCount != null && activeCount > 0) {
            throw new IllegalArgumentException("该范围已存在盘点中的批次，请先完成或关闭后再创建");
        }

        LocalDateTime now = LocalDateTime.now();
        InventoryBatch batch = new InventoryBatch();
        batch.setBatchNo(generateBatchNo());
        String name = request.getBatchName();
        if (name == null || name.trim().isEmpty()) {
            name = scopeName + "盘点-" + now.format(DAY_FMT);
        }
        batch.setBatchName(name.trim());
        batch.setScopeType(request.getScopeType());
        batch.setFloorId(floorId);
        batch.setRoomId(roomId);
        batch.setOperator(request.getOperator().trim());
        batch.setStatus(InventoryBatch.STATUS_COUNTING);
        batch.setSnapshotTime(now);
        batch.setTotalCount(devices.size());
        batch.setPresentCount(0);
        batch.setMissingCount(0);
        batch.setMismatchCount(0);
        batch.setRepairCount(0);
        batch.setCheckedCount(0);
        batch.setRemark(request.getRemark());
        batchMapper.insert(batch);

        for (Device device : devices) {
            InventoryItem item = new InventoryItem();
            item.setBatchId(batch.getId());
            item.setDeviceId(device.getId());
            item.setDeviceCode(device.getDeviceCode());
            item.setDeviceName(device.getDeviceName());
            item.setDeviceType(device.getDeviceType());
            item.setSnapshotFloorId(device.getCurrentFloorId());
            item.setSnapshotRoomId(device.getCurrentRoomId());
            item.setLocationMismatch(0);
            itemMapper.insert(item);
        }

        return toBatchVO(batch, loadFloorMap(), loadRoomMap(), 0);
    }

    @Override
    public IPage<InventoryBatchVO> getBatchesPage(int pageNum, int pageSize, Integer status, String batchName) {
        Page<InventoryBatch> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<InventoryBatch> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(InventoryBatch::getStatus, status);
        }
        if (batchName != null && !batchName.trim().isEmpty()) {
            wrapper.like(InventoryBatch::getBatchName, batchName.trim());
        }
        wrapper.orderByDesc(InventoryBatch::getCreatedAt);
        IPage<InventoryBatch> batchPage = batchMapper.selectPage(page, wrapper);

        Map<Long, String> floorMap = loadFloorMap();
        Map<Long, String> roomMap = loadRoomMap();
        Map<Long, Integer> pendingMap = loadPendingCounts(batchPage.getRecords());

        return batchPage.convert(batch -> toBatchVO(
                batch, floorMap, roomMap, pendingMap.getOrDefault(batch.getId(), 0)));
    }

    @Override
    public InventoryBatchVO getBatchById(Long batchId) {
        InventoryBatch batch = requireBatch(batchId);
        return toBatchVO(batch, loadFloorMap(), loadRoomMap(), countPending(batchId));
    }

    @Override
    public List<InventoryItemVO> getBatchItems(Long batchId, Integer checkResult) {
        InventoryBatch batch = requireBatch(batchId);
        LambdaQueryWrapper<InventoryItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryItem::getBatchId, batchId);
        if (checkResult != null) {
            wrapper.eq(InventoryItem::getCheckResult, checkResult);
        }
        wrapper.orderByAsc(InventoryItem::getDeviceCode);
        List<InventoryItem> items = itemMapper.selectList(wrapper);

        Map<Long, String> floorMap = loadFloorMap();
        Map<Long, String> roomMap = loadRoomMap();
        final Map<Long, Device> liveDeviceMap;
        if (batch.getStatus() == InventoryBatch.STATUS_COUNTING && !items.isEmpty()) {
            List<Long> deviceIds = items.stream().map(InventoryItem::getDeviceId).collect(Collectors.toList());
            liveDeviceMap = deviceMapper.selectBatchIds(deviceIds).stream()
                    .collect(Collectors.toMap(Device::getId, Function.identity(), (a, b) -> a));
        } else {
            liveDeviceMap = Collections.emptyMap();
        }
        final int batchStatus = batch.getStatus();
        return items.stream()
                .map(item -> toItemVO(item, floorMap, roomMap, liveDeviceMap, batchStatus))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryItemVO checkItem(Long batchId, Long itemId, InventoryItemCheckRequest request) {
        InventoryBatch batch = requireBatch(batchId);
        if (batch.getStatus() != InventoryBatch.STATUS_COUNTING) {
            throw new IllegalArgumentException("批次已提交或关闭，盘点结果不可修改");
        }
        InventoryItem item = requireItem(batchId, itemId);
        if (request.getCheckResult() == null
                || request.getCheckResult() < 1 || request.getCheckResult() > 4) {
            throw new IllegalArgumentException("盘点结果必须为在场、缺失、位置不符或待维修");
        }
        item.setCheckResult(request.getCheckResult());
        item.setRemark(request.getRemark());
        itemMapper.updateById(item);

        refreshCounts(batch);
        // 盘点中：位置提示实时比对快照与当前台账
        Device live = deviceMapper.selectById(item.getDeviceId());
        Map<Long, Device> liveMap = live != null
                ? Map.of(live.getId(), live) : Collections.emptyMap();
        return toItemVO(item, loadFloorMap(), loadRoomMap(), liveMap, batch.getStatus());
    }

    @Override
    @Transactional
    public InventorySubmitResultVO submitBatch(Long batchId) {
        InventoryBatch batch = requireBatch(batchId);
        if (batch.getStatus() != InventoryBatch.STATUS_COUNTING) {
            throw new IllegalArgumentException("批次已提交或关闭，不能重复提交");
        }

        List<InventoryItem> items = listItems(batchId);
        long unchecked = items.stream().filter(i -> i.getCheckResult() == null).count();
        if (unchecked > 0) {
            throw new IllegalArgumentException("还有 " + unchecked + " 台设备未完成盘点，无法提交");
        }

        Map<Long, Device> deviceMap = loadLiveDevices(batchId);
        List<Device> repairDevices = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (InventoryItem item : items) {
            // 冻结提交时台账归属，作为差异基准；刷新后差异以此为准保持稳定
            Device device = deviceMap.get(item.getDeviceId());
            Long ledgerFloorId = device != null ? device.getCurrentFloorId() : null;
            Long ledgerRoomId = device != null ? device.getCurrentRoomId() : null;
            item.setLedgerFloorId(ledgerFloorId);
            item.setLedgerRoomId(ledgerRoomId);
            boolean mismatch = !Objects.equals(ledgerFloorId, item.getSnapshotFloorId())
                    || !Objects.equals(ledgerRoomId, item.getSnapshotRoomId());
            item.setLocationMismatch(mismatch ? 1 : 0);

            // 缺失/位置不符/待维修进入待处理；在场无需处理
            if (item.getCheckResult() == InventoryItem.RESULT_MISSING
                    || item.getCheckResult() == InventoryItem.RESULT_MISMATCH
                    || item.getCheckResult() == InventoryItem.RESULT_REPAIR) {
                item.setProcessStatus(InventoryItem.PROCESS_PENDING);
            } else {
                item.setProcessStatus(null);
            }
            itemMapper.updateById(item);

            // 现场标记待维修：同步设备台账状态为待维修
            if (item.getCheckResult() == InventoryItem.RESULT_REPAIR
                    && device != null && !Integer.valueOf(2).equals(device.getStatus())) {
                device.setStatus(2);
                repairDevices.add(device);
            }
        }
        repairDevices.forEach(deviceMapper::updateById);

        batch.setStatus(InventoryBatch.STATUS_SUBMITTED);
        batch.setSubmittedAt(now);
        refreshCounts(batch);

        Map<Long, String> floorMap = loadFloorMap();
        Map<Long, String> roomMap = loadRoomMap();
        List<InventoryItemVO> diffItems = items.stream()
                .filter(i -> i.getCheckResult() != InventoryItem.RESULT_PRESENT)
                .map(i -> toItemVO(i, floorMap, roomMap, Collections.emptyMap(), batch.getStatus()))
                .collect(Collectors.toList());

        InventorySubmitResultVO result = new InventorySubmitResultVO();
        result.setBatch(toBatchVO(batch, floorMap, roomMap, countPending(batchId)));
        result.setDiffItems(diffItems);
        result.setPresentCount(batch.getPresentCount());
        result.setMissingCount(batch.getMissingCount());
        result.setMismatchCount(batch.getMismatchCount());
        result.setRepairCount(batch.getRepairCount());
        return result;
    }

    @Override
    @Transactional
    public InventoryItemVO transferItem(Long batchId, Long itemId, InventoryTransferRequest request) {
        InventoryBatch batch = requireBatch(batchId);
        if (batch.getStatus() != InventoryBatch.STATUS_SUBMITTED) {
            throw new IllegalArgumentException("仅已提交批次可处理差异，关闭后不可修改");
        }
        InventoryItem item = requireItem(batchId, itemId);
        if (item.getCheckResult() != InventoryItem.RESULT_MISMATCH) {
            throw new IllegalArgumentException("仅位置不符设备可执行调配");
        }
        if (!Integer.valueOf(InventoryItem.PROCESS_PENDING).equals(item.getProcessStatus())) {
            throw new IllegalArgumentException("该位置不符差异已处理");
        }
        if (request.getToFloorId() == null || request.getToRoomId() == null) {
            throw new IllegalArgumentException("请选择调配目标楼层和接待室");
        }

        // 复用设备调配：更新台账、记录流转
        DeviceTransferRequest transferRequest = new DeviceTransferRequest();
        transferRequest.setDeviceId(item.getDeviceId());
        transferRequest.setToFloorId(request.getToFloorId());
        transferRequest.setToRoomId(request.getToRoomId());
        transferRequest.setTransferReason(
                request.getTransferReason() != null && !request.getTransferReason().trim().isEmpty()
                        ? request.getTransferReason() : "盘点位置不符调配，批次" + batch.getBatchNo());
        transferRequest.setOperator(
                request.getOperator() != null && !request.getOperator().trim().isEmpty()
                        ? request.getOperator() : batch.getOperator());
        transferRequest.setRemark(request.getRemark());
        DeviceVO updated = deviceService.transferDevice(transferRequest);

        item.setProcessStatus(InventoryItem.PROCESS_DONE);
        item.setProcessRemark(request.getRemark());
        item.setProcessedAt(LocalDateTime.now());
        // 以调配后台账重新比对快照
        boolean stillMismatch = !Objects.equals(updated.getCurrentFloorId(), item.getSnapshotFloorId())
                || !Objects.equals(updated.getCurrentRoomId(), item.getSnapshotRoomId());
        item.setLocationMismatch(stillMismatch ? 1 : 0);
        itemMapper.updateById(item);

        refreshCounts(batch);
        return toItemVO(item, loadFloorMap(), loadRoomMap(), Collections.emptyMap(), batch.getStatus());
    }

    @Override
    @Transactional
    public InventoryItemVO resolveItem(Long batchId, Long itemId, InventoryItemResolveRequest request) {
        InventoryBatch batch = requireBatch(batchId);
        if (batch.getStatus() != InventoryBatch.STATUS_SUBMITTED) {
            throw new IllegalArgumentException("仅已提交批次可处理差异，关闭后不可修改");
        }
        InventoryItem item = requireItem(batchId, itemId);
        if (item.getCheckResult() != InventoryItem.RESULT_MISSING
                && item.getCheckResult() != InventoryItem.RESULT_REPAIR) {
            throw new IllegalArgumentException("仅缺失或待维修设备可登记处理结果");
        }
        if (!Integer.valueOf(InventoryItem.PROCESS_PENDING).equals(item.getProcessStatus())) {
            throw new IllegalArgumentException("该差异已处理，请勿重复登记");
        }
        // 缺失设备保留处理状态：由待处理变更为已处理，并留存处理备注
        item.setProcessStatus(InventoryItem.PROCESS_DONE);
        item.setProcessRemark(request != null ? request.getProcessRemark() : null);
        item.setProcessedAt(LocalDateTime.now());
        itemMapper.updateById(item);

        refreshCounts(batch);
        return toItemVO(item, loadFloorMap(), loadRoomMap(), Collections.emptyMap(), batch.getStatus());
    }

    @Override
    @Transactional
    public InventoryBatchVO closeBatch(Long batchId) {
        InventoryBatch batch = requireBatch(batchId);
        if (batch.getStatus() == InventoryBatch.STATUS_CLOSED) {
            throw new IllegalArgumentException("批次已关闭，不可重复关闭");
        }
        if (batch.getStatus() != InventoryBatch.STATUS_SUBMITTED) {
            throw new IllegalArgumentException("盘点中的批次需提交后才能关闭");
        }
        batch.setStatus(InventoryBatch.STATUS_CLOSED);
        batch.setClosedAt(LocalDateTime.now());
        batchMapper.updateById(batch);
        return toBatchVO(batch, loadFloorMap(), loadRoomMap(), countPending(batchId));
    }

    // ---------------- 私有辅助方法 ----------------

    private InventoryBatch requireBatch(Long batchId) {
        InventoryBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("盘点批次不存在");
        }
        return batch;
    }

    private InventoryItem requireItem(Long batchId, Long itemId) {
        InventoryItem item = itemMapper.selectById(itemId);
        if (item == null || !item.getBatchId().equals(batchId)) {
            throw new IllegalArgumentException("盘点明细不存在");
        }
        return item;
    }

    private List<InventoryItem> listItems(Long batchId) {
        LambdaQueryWrapper<InventoryItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryItem::getBatchId, batchId);
        return itemMapper.selectList(wrapper);
    }

    /**
     * 依据明细实时重算批次汇总与进度。
     */
    private void refreshCounts(InventoryBatch batch) {
        List<InventoryItem> items = listItems(batch.getId());
        int present = 0, missing = 0, mismatch = 0, repair = 0, checked = 0;
        for (InventoryItem item : items) {
            if (item.getCheckResult() == null) {
                continue;
            }
            checked++;
            switch (item.getCheckResult()) {
                case InventoryItem.RESULT_PRESENT -> present++;
                case InventoryItem.RESULT_MISSING -> missing++;
                case InventoryItem.RESULT_MISMATCH -> mismatch++;
                case InventoryItem.RESULT_REPAIR -> repair++;
                default -> { }
            }
        }
        batch.setTotalCount(items.size());
        batch.setPresentCount(present);
        batch.setMissingCount(missing);
        batch.setMismatchCount(mismatch);
        batch.setRepairCount(repair);
        batch.setCheckedCount(checked);
        batchMapper.updateById(batch);
    }

    private int countPending(Long batchId) {
        LambdaQueryWrapper<InventoryItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryItem::getBatchId, batchId);
        wrapper.eq(InventoryItem::getProcessStatus, InventoryItem.PROCESS_PENDING);
        return Math.toIntExact(itemMapper.selectCount(wrapper));
    }

    private Map<Long, Integer> loadPendingCounts(List<InventoryBatch> batches) {
        if (batches.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> batchIds = batches.stream().map(InventoryBatch::getId).collect(Collectors.toList());
        LambdaQueryWrapper<InventoryItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(InventoryItem::getBatchId, batchIds);
        wrapper.eq(InventoryItem::getProcessStatus, InventoryItem.PROCESS_PENDING);
        Map<Long, Integer> result = new HashMap<>();
        for (InventoryItem item : itemMapper.selectList(wrapper)) {
            result.merge(item.getBatchId(), 1, Integer::sum);
        }
        return result;
    }

    private Map<Long, Device> loadLiveDevices(Long batchId) {
        List<InventoryItem> items = listItems(batchId);
        if (items.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> deviceIds = items.stream().map(InventoryItem::getDeviceId).collect(Collectors.toList());
        return deviceMapper.selectBatchIds(deviceIds).stream()
                .collect(Collectors.toMap(Device::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, String> loadFloorMap() {
        return floorService.getAllFloors().stream()
                .collect(Collectors.toMap(Floor::getId, Floor::getFloorName, (a, b) -> a));
    }

    private Map<Long, String> loadRoomMap() {
        return roomService.getAllRooms().stream()
                .collect(Collectors.toMap(ReceptionRoom::getId, ReceptionRoom::getRoomName, (a, b) -> a));
    }

    private InventoryBatchVO toBatchVO(InventoryBatch batch, Map<Long, String> floorMap,
                                       Map<Long, String> roomMap, int pendingDiffCount) {
        InventoryBatchVO vo = new InventoryBatchVO();
        vo.setId(batch.getId());
        vo.setBatchNo(batch.getBatchNo());
        vo.setBatchName(batch.getBatchName());
        vo.setScopeType(batch.getScopeType());
        vo.setScopeTypeText(InventoryBatch.SCOPE_ROOM.equals(batch.getScopeType()) ? "接待室" : "楼层");
        vo.setFloorId(batch.getFloorId());
        vo.setFloorName(batch.getFloorId() != null ? floorMap.get(batch.getFloorId()) : null);
        vo.setRoomId(batch.getRoomId());
        vo.setRoomName(batch.getRoomId() != null ? roomMap.get(batch.getRoomId()) : null);
        vo.setOperator(batch.getOperator());
        vo.setStatus(batch.getStatus());
        vo.setStatusText(statusText(batch.getStatus()));
        vo.setSnapshotTime(batch.getSnapshotTime());
        vo.setSubmittedAt(batch.getSubmittedAt());
        vo.setClosedAt(batch.getClosedAt());
        vo.setTotalCount(batch.getTotalCount());
        vo.setPresentCount(batch.getPresentCount());
        vo.setMissingCount(batch.getMissingCount());
        vo.setMismatchCount(batch.getMismatchCount());
        vo.setRepairCount(batch.getRepairCount());
        vo.setCheckedCount(batch.getCheckedCount());
        int total = batch.getTotalCount() != null ? batch.getTotalCount() : 0;
        vo.setProgressPercent(total == 0 ? 0 : (int) Math.round(batch.getCheckedCount() * 100.0 / total));
        vo.setPendingDiffCount(pendingDiffCount);
        vo.setRemark(batch.getRemark());
        vo.setCreatedAt(batch.getCreatedAt());
        vo.setUpdatedAt(batch.getUpdatedAt());
        return vo;
    }

    private InventoryItemVO toItemVO(InventoryItem item, Map<Long, String> floorMap,
                                     Map<Long, String> roomMap, Map<Long, Device> liveDeviceMap,
                                     int batchStatus) {
        InventoryItemVO vo = new InventoryItemVO();
        vo.setId(item.getId());
        vo.setBatchId(item.getBatchId());
        vo.setDeviceId(item.getDeviceId());
        vo.setDeviceCode(item.getDeviceCode());
        vo.setDeviceName(item.getDeviceName());
        vo.setDeviceType(item.getDeviceType());

        vo.setSnapshotFloorId(item.getSnapshotFloorId());
        vo.setSnapshotFloorName(item.getSnapshotFloorId() != null ? floorMap.get(item.getSnapshotFloorId()) : null);
        vo.setSnapshotRoomId(item.getSnapshotRoomId());
        vo.setSnapshotRoomName(item.getSnapshotRoomId() != null ? roomMap.get(item.getSnapshotRoomId()) : null);

        vo.setCheckResult(item.getCheckResult());
        vo.setCheckResultText(resultText(item.getCheckResult()));
        vo.setRemark(item.getRemark());

        vo.setLedgerFloorId(item.getLedgerFloorId());
        vo.setLedgerFloorName(item.getLedgerFloorId() != null ? floorMap.get(item.getLedgerFloorId()) : null);
        vo.setLedgerRoomId(item.getLedgerRoomId());
        vo.setLedgerRoomName(item.getLedgerRoomId() != null ? roomMap.get(item.getLedgerRoomId()) : null);

        if (batchStatus == InventoryBatch.STATUS_COUNTING) {
            // 盘点中：现场参考，实时比对快照与当前台账；刷新即更新
            Device live = liveDeviceMap.get(item.getDeviceId());
            boolean mismatch = live != null
                    && (!Objects.equals(live.getCurrentFloorId(), item.getSnapshotFloorId())
                    || !Objects.equals(live.getCurrentRoomId(), item.getSnapshotRoomId()));
            vo.setLocationMismatch(mismatch);
        } else {
            // 提交/关闭：以提交时冻结的差异基准为准，刷新保持一致
            vo.setLocationMismatch(Integer.valueOf(1).equals(item.getLocationMismatch()));
        }

        vo.setProcessStatus(item.getProcessStatus());
        vo.setProcessStatusText(processText(item.getProcessStatus()));
        vo.setProcessRemark(item.getProcessRemark());
        vo.setProcessedAt(item.getProcessedAt());
        vo.setUpdatedAt(item.getUpdatedAt());
        return vo;
    }

    private String generateBatchNo() {
        return "PD" + LocalDateTime.now().format(NO_FMT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String statusText(int status) {
        switch (status) {
            case InventoryBatch.STATUS_COUNTING: return "盘点中";
            case InventoryBatch.STATUS_SUBMITTED: return "已提交";
            case InventoryBatch.STATUS_CLOSED: return "已关闭";
            default: return "未知";
        }
    }

    private String resultText(Integer result) {
        if (result == null) return "未盘点";
        switch (result) {
            case InventoryItem.RESULT_PRESENT: return "在场";
            case InventoryItem.RESULT_MISSING: return "缺失";
            case InventoryItem.RESULT_MISMATCH: return "位置不符";
            case InventoryItem.RESULT_REPAIR: return "待维修";
            default: return "未知";
        }
    }

    private String processText(Integer processStatus) {
        if (processStatus == null) return null;
        switch (processStatus) {
            case InventoryItem.PROCESS_PENDING: return "待处理";
            case InventoryItem.PROCESS_DONE: return "已处理";
            default: return null;
        }
    }
}
