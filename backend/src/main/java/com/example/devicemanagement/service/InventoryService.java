package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.InventoryBatchCreateRequest;
import com.example.devicemanagement.dto.request.InventoryItemCheckRequest;
import com.example.devicemanagement.dto.request.InventoryItemResolveRequest;
import com.example.devicemanagement.dto.request.InventoryTransferRequest;
import com.example.devicemanagement.dto.response.InventoryBatchVO;
import com.example.devicemanagement.dto.response.InventoryItemVO;
import com.example.devicemanagement.dto.response.InventorySubmitResultVO;

import java.util.List;

public interface InventoryService {

    /**
     * 按楼层或接待室创建盘点批次，生成创建时刻的设备快照。
     */
    InventoryBatchVO createBatch(InventoryBatchCreateRequest request);

    IPage<InventoryBatchVO> getBatchesPage(int pageNum, int pageSize, Integer status, String batchName);

    /**
     * 批次详情（含汇总、范围名称与待处理差异数）。
     */
    InventoryBatchVO getBatchById(Long batchId);

    /**
     * 批次盘点明细（含快照归属、盘点结果、差异信息）。
     */
    List<InventoryItemVO> getBatchItems(Long batchId, Integer checkResult);

    /**
     * 现场逐台标记盘点结果并填写备注（盘点中可反复保存）。
     */
    InventoryItemVO checkItem(Long batchId, Long itemId, InventoryItemCheckRequest request);

    /**
     * 全部盘点完成后提交：冻结提交时台账归属并产出差异清单。
     */
    InventorySubmitResultVO submitBatch(Long batchId);

    /**
     * 位置不符设备跳转到调配：复用设备调配，更新台账并记录处理状态。
     */
    InventoryItemVO transferItem(Long batchId, Long itemId, InventoryTransferRequest request);

    /**
     * 缺失/待维修设备登记处理结果（缺失保留处理状态）。
     */
    InventoryItemVO resolveItem(Long batchId, Long itemId, InventoryItemResolveRequest request);

    /**
     * 关闭批次，关闭后不可再修改。
     */
    InventoryBatchVO closeBatch(Long batchId);
}
