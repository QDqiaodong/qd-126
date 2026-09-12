package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.devicemanagement.dto.request.WelcomeBoardCreateRequest;
import com.example.devicemanagement.dto.request.WelcomeBoardRemoveRequest;
import com.example.devicemanagement.dto.response.BoardRoomAvailabilityVO;
import com.example.devicemanagement.dto.response.WelcomeBoardVO;
import com.example.devicemanagement.service.WelcomeBoardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WelcomeBoardController.class)
class WelcomeBoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WelcomeBoardService boardService;

    private WelcomeBoardVO buildBoard() {
        WelcomeBoardVO vo = new WelcomeBoardVO();
        vo.setId(1L);
        vo.setBoardNo("YP202609121000");
        vo.setBoardText("热烈欢迎考察团莅临指导");
        vo.setRoomId(10L);
        vo.setRoomName("301接待室");
        vo.setFloorId(1L);
        vo.setFloorName("3F");
        vo.setRegistrar("行政小王");
        vo.setStatus(1);
        vo.setStatusText("已上墙");
        vo.setOverdueRemove(true);
        vo.setMountTime(LocalDateTime.of(2026, 9, 12, 8, 0));
        vo.setPlannedRemoveTime(LocalDateTime.of(2026, 9, 12, 10, 0));
        return vo;
    }

    @Test
    void passesFiltersAndReturnsPagedPayload() throws Exception {
        Page<WelcomeBoardVO> page = new Page<>(1, 10);
        page.setRecords(List.of(buildBoard()));
        page.setTotal(1);
        when(boardService.getBoardsPage(eq(1), eq(10), eq(1L), eq(10L), eq(1), eq(true)))
                .thenReturn(page);

        mockMvc.perform(get("/api/welcome-board")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("floorId", "1")
                        .param("roomId", "10")
                        .param("status", "1")
                        .param("overdueOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].boardText").value("热烈欢迎考察团莅临指导"))
                .andExpect(jsonPath("$.data.records[0].overdueRemove").value(true));

        verify(boardService).getBoardsPage(1, 10, 1L, 10L, 1, true);
    }

    @Test
    void overdueEndpointReturnsPendingRemovalBoards() throws Exception {
        when(boardService.getOverdueBoards()).thenReturn(List.of(buildBoard()));

        mockMvc.perform(get("/api/welcome-board/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].statusText").value("已上墙"))
                .andExpect(jsonPath("$.data[0].overdueRemove").value(true));
    }

    @Test
    void createEndpointReturnsCreatedBoard() throws Exception {
        WelcomeBoardVO created = buildBoard();
        created.setStatus(0);
        created.setStatusText("待上墙");
        created.setOverdueRemove(false);
        when(boardService.createBoard(org.mockito.ArgumentMatchers.any(WelcomeBoardCreateRequest.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/welcome-board")
                        .contentType("application/json")
                        .content("""
                                {
                                  "boardText": "热烈欢迎考察团莅临指导",
                                  "roomId": 10,
                                  "mountTime": "2026-09-13T08:00:00",
                                  "plannedRemoveTime": "2026-09-13T10:00:00",
                                  "registrar": "行政小王"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("欢迎牌排期登记成功"))
                .andExpect(jsonPath("$.data.statusText").value("待上墙"));
    }

    @Test
    void removeEndpointForwardsReceipt() throws Exception {
        WelcomeBoardVO removed = buildBoard();
        removed.setStatus(2);
        removed.setStatusText("已撤下");
        removed.setOverdueRemove(false);
        removed.setRemoveReceipt("已取下归库");
        when(boardService.removeBoard(eq(1L), org.mockito.ArgumentMatchers.any(WelcomeBoardRemoveRequest.class)))
                .thenReturn(removed);

        mockMvc.perform(post("/api/welcome-board/1/remove")
                        .contentType("application/json")
                        .content("""
                                {"receipt": "已取下归库", "operator": "李四"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("撤下回执已登记，欢迎牌已撤下"))
                .andExpect(jsonPath("$.data.status").value(2))
                .andExpect(jsonPath("$.data.removeReceipt").value("已取下归库"));
    }

    @Test
    void roomAvailabilityEndpointReturnsRooms() throws Exception {
        BoardRoomAvailabilityVO vo = new BoardRoomAvailabilityVO();
        vo.setRoomId(10L);
        vo.setRoomName("301接待室");
        vo.setAvailable(false);
        vo.setOverdueRemove(true);
        vo.setBoardText("热烈欢迎考察团莅临指导");
        when(boardService.getRoomAvailability(1L)).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/welcome-board/room-availability").param("floorId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].available").value(false))
                .andExpect(jsonPath("$.data[0].overdueRemove").value(true));

        verify(boardService).getRoomAvailability(1L);
    }
}
