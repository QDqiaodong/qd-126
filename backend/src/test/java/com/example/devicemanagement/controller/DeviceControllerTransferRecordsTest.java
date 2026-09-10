package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.response.TransferRecordVO;
import com.example.devicemanagement.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceController.class)
class DeviceControllerTransferRecordsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceService deviceService;

    private TransferRecordVO buildRecord() {
        TransferRecordVO vo = new TransferRecordVO();
        vo.setId(1L);
        vo.setDeviceId(7L);
        vo.setDeviceCode("DEV007");
        vo.setDeviceName("高清投影仪");
        vo.setOperator("张三");
        vo.setTransferTime(LocalDateTime.of(2026, 9, 1, 10, 0));
        return vo;
    }

    @Test
    void passesFiltersToServiceAndReturnsPagedPayload() throws Exception {
        Page<TransferRecordVO> page = new Page<>(1, 10);
        page.setRecords(List.of(buildRecord()));
        page.setTotal(23);
        when(deviceService.getAllTransferRecords(eq(1), eq(10), eq("投影仪"), eq("张三")))
                .thenReturn(page);

        mockMvc.perform(get("/api/device/transfer-records")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("deviceName", "投影仪")
                        .param("operator", "张三"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(23))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.pages").value(3))
                .andExpect(jsonPath("$.data.records[0].deviceName").value("高清投影仪"))
                .andExpect(jsonPath("$.data.records[0].operator").value("张三"));
    }

    @Test
    void usesDefaultPagingAndNullFiltersWhenParamsAbsent() throws Exception {
        Page<TransferRecordVO> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(deviceService.getAllTransferRecords(eq(1), eq(10), isNull(), isNull()))
                .thenReturn(page);

        mockMvc.perform(get("/api/device/transfer-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isArray());

        verify(deviceService).getAllTransferRecords(1, 10, null, null);
    }
}
