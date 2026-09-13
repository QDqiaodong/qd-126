package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.response.InterpreterBookingVO;
import com.example.devicemanagement.exception.InterpreterConflictException;
import com.example.devicemanagement.service.InterpreterBookingService;
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

@WebMvcTest(InterpreterBookingController.class)
class InterpreterBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InterpreterBookingService bookingService;

    private InterpreterBookingVO buildBooking() {
        InterpreterBookingVO vo = new InterpreterBookingVO();
        vo.setId(1L);
        vo.setBookingNo("FY20260912100001");
        vo.setRoomId(10L);
        vo.setRoomName("301接待室");
        vo.setRoomCode("R301");
        vo.setFloorId(1L);
        vo.setFloorName("3F");
        vo.setLanguage("英语");
        vo.setInterpreterName("王芳");
        vo.setStatus(0);
        vo.setStatusText("待开始");
        vo.setStartTime(LocalDateTime.of(2026, 9, 13, 14, 0));
        vo.setEndTime(LocalDateTime.of(2026, 9, 13, 16, 0));
        return vo;
    }

    @Test
    void passesFloorFilterAndReturnsPagedPayload() throws Exception {
        Page<InterpreterBookingVO> page = new Page<>(1, 10);
        page.setRecords(List.of(buildBooking()));
        page.setTotal(1);
        when(bookingService.getBookingsPage(eq(1), eq(10), eq(1L), eq(null)))
                .thenReturn(page);

        mockMvc.perform(get("/api/interpreter-booking")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("floorId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].roomName").value("301接待室"))
                .andExpect(jsonPath("$.data.records[0].language").value("英语"))
                .andExpect(jsonPath("$.data.records[0].interpreterName").value("王芳"));

        verify(bookingService).getBookingsPage(1, 10, 1L, null);
    }

    @Test
    void createReturnsRegisteredBooking() throws Exception {
        when(bookingService.createBooking(any())).thenReturn(buildBooking());

        mockMvc.perform(post("/api/interpreter-booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":10,\"language\":\"英语\",\"interpreterName\":\"王芳\","
                                + "\"startTime\":\"2026-09-13T14:00:00\","
                                + "\"endTime\":\"2026-09-13T16:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("随行翻译预约成功"))
                .andExpect(jsonPath("$.data.bookingNo").value("FY20260912100001"));
    }

    @Test
    void interpreterConflictReturnsBusinessCode461() throws Exception {
        when(bookingService.createBooking(any())).thenThrow(
                new InterpreterConflictException("译员「王芳」时段撞车，同一时段已被以下接待室预约：301接待室"));

        mockMvc.perform(post("/api/interpreter-booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":11,\"language\":\"日语\",\"interpreterName\":\"王芳\","
                                + "\"startTime\":\"2026-09-13T15:00:00\","
                                + "\"endTime\":\"2026-09-13T17:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(461))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("301接待室")));
    }

    @Test
    void deleteReturnsSuccessMessage() throws Exception {
        mockMvc.perform(delete("/api/interpreter-booking/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("翻译预约已删除"));

        verify(bookingService).deleteBooking(1L);
    }
}
