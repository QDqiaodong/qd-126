package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.DeviceReplacementRequest;
import com.example.devicemanagement.dto.response.DeviceReplacementVO;
import com.example.devicemanagement.dto.response.SpareDeviceVO;
import com.example.devicemanagement.service.DeviceReplacementService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceReplacementController.class)
class DeviceReplacementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceReplacementService replacementService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DeviceReplacementVO buildVO(long id) {
        DeviceReplacementVO vo = new DeviceReplacementVO();
        vo.setId(id);
        vo.setReplacementNo("TH2026090112000001");
        vo.setFloorId(1L);
        vo.setFloorName("3F");
        vo.setRoomId(10L);
        vo.setRoomName("301接待室");
        vo.setFaultyDeviceCode("MIC-001");
        vo.setFaultyDeviceName("故障麦克风");
        vo.setSpareDeviceCode("MIC-B1");
        vo.setSpareDeviceName("备用麦克风");
        vo.setFaultPhenomenon("无声音");
        vo.setOperator("值班员甲");
        vo.setProcessResult(1);
        vo.setProcessResultText("待维修");
        return vo;
    }

    @Test
    void passesFloorRoomResultFiltersAndReturnsPagedPayload() throws Exception {
        Page<DeviceReplacementVO> page = new Page<>(1, 10);
        page.setRecords(List.of(buildVO(1L)));
        page.setTotal(15);
        when(replacementService.getReplacementsPage(eq(1), eq(10), eq(1L), eq(10L), eq(1)))
                .thenReturn(page);

        mockMvc.perform(get("/api/replacement")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("floorId", "1")
                        .param("roomId", "10")
                        .param("processResult", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.records[0].faultyDeviceName").value("故障麦克风"))
                .andExpect(jsonPath("$.data.records[0].spareDeviceName").value("备用麦克风"));
    }

    @Test
    void getSparesForwardsFaultyDeviceId() throws Exception {
        SpareDeviceVO spare = new SpareDeviceVO();
        spare.setId(201L);
        spare.setDeviceCode("MIC-B1");
        spare.setDeviceName("备用麦克风");
        spare.setSameDeviceType(true);
        when(replacementService.getSpares(101L)).thenReturn(List.of(spare));

        mockMvc.perform(get("/api/replacement/spares").param("faultyDeviceId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].deviceCode").value("MIC-B1"));

        verify(replacementService).getSpares(101L);
    }

    @Test
    void createReplacementReturnsCreatedRecord() throws Exception {
        DeviceReplacementRequest request = new DeviceReplacementRequest();
        request.setFaultyDeviceId(101L);
        request.setSpareDeviceId(201L);
        request.setFaultPhenomenon("无声音");
        request.setOperator("值班员甲");
        when(replacementService.createReplacement(org.mockito.ArgumentMatchers.any()))
                .thenReturn(buildVO(1L));

        mockMvc.perform(post("/api/replacement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.replacementNo").value("TH2026090112000001"));
    }

    @Test
    void validationErrorReturns400() throws Exception {
        when(replacementService.createReplacement(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalArgumentException("请选择同楼层的备用设备"));

        DeviceReplacementRequest request = new DeviceReplacementRequest();
        request.setFaultyDeviceId(101L);

        mockMvc.perform(post("/api/replacement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请选择同楼层的备用设备"));
    }
}
