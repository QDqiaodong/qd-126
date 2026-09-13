package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.response.RoomActivityVO;
import com.example.devicemanagement.service.RoomActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomActivityController.class)
class RoomActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomActivityService activityService;

    private RoomActivityVO buildActivity() {
        RoomActivityVO vo = new RoomActivityVO();
        vo.setId(1L);
        vo.setActivityNo("HD202609111000");
        vo.setActivityName("客户参观");
        vo.setRoomId(10L);
        vo.setRoomName("301接待室");
        vo.setFloorId(1L);
        vo.setFloorName("3F");
        vo.setManager("张三");
        vo.setStatus(1);
        vo.setStatusText("进行中");
        vo.setStartTime(LocalDateTime.of(2026, 9, 11, 10, 0));
        vo.setEndTime(LocalDateTime.of(2026, 9, 11, 12, 0));
        vo.setDeviceCount(2);
        return vo;
    }

    @Test
    void passesDateFloorAndStatusFiltersAndReturnsPagedPayload() throws Exception {
        Page<RoomActivityVO> page = new Page<>(1, 10);
        page.setRecords(List.of(buildActivity()));
        page.setTotal(1);
        when(activityService.getActivitiesPage(eq(1), eq(10), eq(LocalDate.of(2026, 9, 11)), eq(1L), eq(1)))
                .thenReturn(page);

        mockMvc.perform(get("/api/activity")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("date", "2026-09-11")
                        .param("floorId", "1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].activityName").value("客户参观"))
                .andExpect(jsonPath("$.data.records[0].statusText").value("进行中"));

        verify(activityService).getActivitiesPage(1, 10, LocalDate.of(2026, 9, 11), 1L, 1);
    }

    @Test
    void finishReleasesOccupation() throws Exception {
        RoomActivityVO ended = buildActivity();
        ended.setStatus(2);
        ended.setStatusText("已结束");
        when(activityService.finishActivity(1L)).thenReturn(ended);

        mockMvc.perform(post("/api/activity/1/finish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("活动已结束，占用设备已释放"))
                .andExpect(jsonPath("$.data.status").value(2));
    }

    @Test
    void updatePassesPayloadAndReturnsUpdatedActivity() throws Exception {
        RoomActivityVO updated = buildActivity();
        updated.setActivityName("改名发布会");
        when(activityService.updateActivity(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/activity/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityName\":\"改名发布会\",\"roomId\":10,"
                                + "\"startTime\":\"2026-09-12T10:00:00\",\"endTime\":\"2026-09-12T12:00:00\","
                                + "\"deviceIds\":[101],\"manager\":\"张三\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("活动占用已更新"))
                .andExpect(jsonPath("$.data.activityName").value("改名发布会"));

        verify(activityService).updateActivity(eq(1L), any());
    }
}
