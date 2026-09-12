package com.example.devicemanagement.controller;

import com.example.devicemanagement.dto.response.WarrantyDeviceVO;
import com.example.devicemanagement.dto.response.WarrantyFloorGroupVO;
import com.example.devicemanagement.dto.response.WarrantyOverviewVO;
import com.example.devicemanagement.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceController.class)
class DeviceControllerWarrantyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceService deviceService;

    private WarrantyOverviewVO buildOverview() {
        WarrantyDeviceVO device = new WarrantyDeviceVO();
        device.setId(1L);
        device.setDeviceCode("TV-001");
        device.setDeviceName("大厅电视");
        device.setDeviceType("电视");
        device.setCurrentFloorId(1L);
        device.setCurrentFloorName("3F");
        device.setCurrentRoomId(10L);
        device.setCurrentRoomName("301接待室");
        device.setWarrantyEndDate(LocalDate.of(2026, 9, 20));
        device.setDaysRemaining(8L);
        device.setWarrantyStatus("EXPIRING");
        device.setWarrantyStatusText("临期");

        WarrantyFloorGroupVO group = new WarrantyFloorGroupVO();
        group.setFloorId(1L);
        group.setFloorName("3F");
        group.setDevices(List.of(device));

        WarrantyDeviceVO undated = new WarrantyDeviceVO();
        undated.setId(2L);
        undated.setDeviceCode("MIC-001");
        undated.setWarrantyStatus("NONE");
        undated.setWarrantyStatusText("未设置");

        WarrantyOverviewVO overview = new WarrantyOverviewVO();
        overview.setExpiringSoonDays(15);
        overview.setGroups(List.of(group));
        overview.setNoWarrantyDevices(List.of(undated));
        return overview;
    }

    @Test
    void returnsWarrantyOverviewWithFloorAndRoomFilters() throws Exception {
        when(deviceService.getWarrantyOverview(1L, 10L)).thenReturn(buildOverview());

        mockMvc.perform(get("/api/device/warranty-overview")
                        .param("floorId", "1")
                        .param("roomId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.expiringSoonDays").value(15))
                .andExpect(jsonPath("$.data.groups[0].floorName").value("3F"))
                .andExpect(jsonPath("$.data.groups[0].devices[0].warrantyEndDate").value("2026-09-20"))
                .andExpect(jsonPath("$.data.groups[0].devices[0].daysRemaining").value(8))
                .andExpect(jsonPath("$.data.groups[0].devices[0].warrantyStatus").value("EXPIRING"))
                .andExpect(jsonPath("$.data.noWarrantyDevices[0].deviceCode").value("MIC-001"))
                .andExpect(jsonPath("$.data.noWarrantyDevices[0].warrantyStatus").value("NONE"));

        verify(deviceService).getWarrantyOverview(1L, 10L);
    }

    @Test
    void passesNullFiltersWhenParamsAbsent() throws Exception {
        when(deviceService.getWarrantyOverview(isNull(), isNull())).thenReturn(buildOverview());

        mockMvc.perform(get("/api/device/warranty-overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.groups[0].devices[0].deviceName").value("大厅电视"));

        verify(deviceService).getWarrantyOverview(null, null);
    }
}
