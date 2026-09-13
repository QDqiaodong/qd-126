package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.response.RoomQuietPeriodVO;
import com.example.devicemanagement.service.RoomQuietPeriodService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomQuietPeriodController.class)
class RoomQuietPeriodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomQuietPeriodService quietPeriodService;

    private RoomQuietPeriodVO buildQuietPeriod() {
        RoomQuietPeriodVO vo = new RoomQuietPeriodVO();
        vo.setId(1L);
        vo.setQuietNo("JY202609121000");
        vo.setRoomId(10L);
        vo.setRoomName("301接待室");
        vo.setFloorId(1L);
        vo.setFloorName("3F");
        vo.setReason("设备检修");
        vo.setStartTime(LocalDateTime.of(2026, 9, 13, 14, 0));
        vo.setEndTime(LocalDateTime.of(2026, 9, 13, 16, 0));
        return vo;
    }

    @Test
    void passesFloorFilterAndReturnsPagedPayload() throws Exception {
        Page<RoomQuietPeriodVO> page = new Page<>(1, 10);
        page.setRecords(List.of(buildQuietPeriod()));
        page.setTotal(1);
        when(quietPeriodService.getQuietPeriodsPage(eq(1), eq(10), eq(1L), eq(null)))
                .thenReturn(page);

        mockMvc.perform(get("/api/quiet-period")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("floorId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].roomName").value("301接待室"))
                .andExpect(jsonPath("$.data.records[0].reason").value("设备检修"));

        verify(quietPeriodService).getQuietPeriodsPage(1, 10, 1L, null);
    }

    @Test
    void createReturnsRegisteredQuietPeriod() throws Exception {
        when(quietPeriodService.createQuietPeriod(any())).thenReturn(buildQuietPeriod());

        mockMvc.perform(post("/api/quiet-period")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":10,\"startTime\":\"2026-09-13T14:00:00\","
                                + "\"endTime\":\"2026-09-13T16:00:00\",\"reason\":\"设备检修\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("静音时段登记成功"))
                .andExpect(jsonPath("$.data.quietNo").value("JY202609121000"));
    }

    @Test
    void deleteReturnsSuccessMessage() throws Exception {
        mockMvc.perform(delete("/api/quiet-period/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("静音时段已删除"));

        verify(quietPeriodService).deleteQuietPeriod(1L);
    }
}
