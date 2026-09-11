package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.InventoryBatchCreateRequest;
import com.example.devicemanagement.dto.request.InventoryItemCheckRequest;
import com.example.devicemanagement.dto.response.InventoryBatchVO;
import com.example.devicemanagement.dto.response.InventoryItemVO;
import com.example.devicemanagement.dto.response.InventorySubmitResultVO;
import com.example.devicemanagement.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private InventoryBatchVO batchVO(long id, int status, String statusText) {
        InventoryBatchVO vo = new InventoryBatchVO();
        vo.setId(id);
        vo.setBatchNo("PD2026091101");
        vo.setBatchName("3F盘点");
        vo.setScopeType("FLOOR");
        vo.setFloorName("3F");
        vo.setStatus(status);
        vo.setStatusText(statusText);
        vo.setTotalCount(10);
        vo.setCheckedCount(10);
        vo.setProgressPercent(100);
        vo.setPendingDiffCount(2);
        return vo;
    }

    @Test
    void createBatchReturnsCreatedSnapshotSummary() throws Exception {
        InventoryBatchCreateRequest req = new InventoryBatchCreateRequest();
        req.setScopeType("FLOOR");
        req.setFloorId(1L);
        req.setOperator("张三");

        when(inventoryService.createBatch(org.mockito.ArgumentMatchers.any()))
                .thenReturn(batchVO(500L, 0, "盘点中"));

        mockMvc.perform(post("/api/inventory/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(500))
                .andExpect(jsonPath("$.data.totalCount").value(10))
                .andExpect(jsonPath("$.data.statusText").value("盘点中"));
    }

    @Test
    void listBatchesPassesFiltersAndPaging() throws Exception {
        Page<InventoryBatchVO> page = new Page<>(1, 10);
        page.setRecords(List.of(batchVO(1L, 1, "已提交"), batchVO(2L, 0, "盘点中")));
        page.setTotal(2);
        when(inventoryService.getBatchesPage(eq(1), eq(10), eq(1), eq("3F"))).thenReturn(page);

        mockMvc.perform(get("/api/inventory/batch")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("status", "1")
                        .param("batchName", "3F"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data.records[0].pendingDiffCount").value(2));
    }

    @Test
    void submitReturnsDifferenceList() throws Exception {
        InventorySubmitResultVO result = new InventorySubmitResultVO();
        result.setBatch(batchVO(500L, 1, "已提交"));
        InventoryItemVO missing = new InventoryItemVO();
        missing.setId(1L);
        missing.setCheckResult(2);
        missing.setCheckResultText("缺失");
        missing.setProcessStatus(1);
        result.setDiffItems(List.of(missing));
        result.setMissingCount(1);
        result.setMismatchCount(0);
        result.setRepairCount(0);
        result.setPresentCount(9);
        when(inventoryService.submitBatch(500L)).thenReturn(result);

        mockMvc.perform(post("/api/inventory/batch/500/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.missingCount").value(1))
                .andExpect(jsonPath("$.data.presentCount").value(9))
                .andExpect(jsonPath("$.data.diffItems[0].checkResultText").value("缺失"));
    }

    @Test
    void checkItemRoutesToService() throws Exception {
        InventoryItemCheckRequest req = new InventoryItemCheckRequest();
        req.setCheckResult(3);
        req.setRemark("放到隔壁房间");

        InventoryItemVO vo = new InventoryItemVO();
        vo.setId(9L);
        vo.setCheckResult(3);
        when(inventoryService.checkItem(eq(500L), eq(9L), org.mockito.ArgumentMatchers.any())).thenReturn(vo);

        mockMvc.perform(put("/api/inventory/batch/500/item/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkResult").value(3));

        verify(inventoryService).checkItem(eq(500L), eq(9L), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void closeBatchRoutesToService() throws Exception {
        when(inventoryService.closeBatch(500L)).thenReturn(batchVO(500L, 2, "已关闭"));

        mockMvc.perform(post("/api/inventory/batch/500/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(2))
                .andExpect(jsonPath("$.data.statusText").value("已关闭"));
    }
}
