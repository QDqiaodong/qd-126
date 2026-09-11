package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.InventoryBatchCreateRequest;
import com.example.devicemanagement.dto.request.InventoryItemCheckRequest;
import com.example.devicemanagement.dto.request.InventoryItemResolveRequest;
import com.example.devicemanagement.dto.request.InventoryTransferRequest;
import com.example.devicemanagement.dto.response.ApiResponse;
import com.example.devicemanagement.dto.response.InventoryBatchVO;
import com.example.devicemanagement.dto.response.InventoryItemVO;
import com.example.devicemanagement.dto.response.InventorySubmitResultVO;
import com.example.devicemanagement.dto.response.PageResponse;
import com.example.devicemanagement.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @PostMapping("/batch")
    public ApiResponse<InventoryBatchVO> createBatch(@RequestBody InventoryBatchCreateRequest request) {
        return ApiResponse.success("盘点批次创建成功", inventoryService.createBatch(request));
    }

    @GetMapping("/batch")
    public ApiResponse<PageResponse<InventoryBatchVO>> getBatches(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String batchName) {
        IPage<InventoryBatchVO> page = inventoryService.getBatchesPage(pageNum, pageSize, status, batchName);
        return ApiResponse.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/batch/{batchId}")
    public ApiResponse<InventoryBatchVO> getBatch(@PathVariable Long batchId) {
        return ApiResponse.success(inventoryService.getBatchById(batchId));
    }

    @GetMapping("/batch/{batchId}/items")
    public ApiResponse<List<InventoryItemVO>> getBatchItems(
            @PathVariable Long batchId,
            @RequestParam(required = false) Integer checkResult) {
        return ApiResponse.success(inventoryService.getBatchItems(batchId, checkResult));
    }

    @PutMapping("/batch/{batchId}/item/{itemId}")
    public ApiResponse<InventoryItemVO> checkItem(
            @PathVariable Long batchId,
            @PathVariable Long itemId,
            @RequestBody InventoryItemCheckRequest request) {
        return ApiResponse.success(inventoryService.checkItem(batchId, itemId, request));
    }

    @PostMapping("/batch/{batchId}/submit")
    public ApiResponse<InventorySubmitResultVO> submitBatch(@PathVariable Long batchId) {
        return ApiResponse.success("盘点提交成功", inventoryService.submitBatch(batchId));
    }

    @PostMapping("/batch/{batchId}/item/{itemId}/transfer")
    public ApiResponse<InventoryItemVO> transferItem(
            @PathVariable Long batchId,
            @PathVariable Long itemId,
            @RequestBody InventoryTransferRequest request) {
        return ApiResponse.success("调配成功", inventoryService.transferItem(batchId, itemId, request));
    }

    @PostMapping("/batch/{batchId}/item/{itemId}/resolve")
    public ApiResponse<InventoryItemVO> resolveItem(
            @PathVariable Long batchId,
            @PathVariable Long itemId,
            @RequestBody(required = false) InventoryItemResolveRequest request) {
        return ApiResponse.success("处理结果已登记", inventoryService.resolveItem(batchId, itemId, request));
    }

    @PostMapping("/batch/{batchId}/close")
    public ApiResponse<InventoryBatchVO> closeBatch(@PathVariable Long batchId) {
        return ApiResponse.success("批次已关闭", inventoryService.closeBatch(batchId));
    }
}
