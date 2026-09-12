package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.WelcomeBoardCreateRequest;
import com.example.devicemanagement.dto.request.WelcomeBoardRemoveRequest;
import com.example.devicemanagement.dto.response.ApiResponse;
import com.example.devicemanagement.dto.response.BoardRoomAvailabilityVO;
import com.example.devicemanagement.dto.response.PageResponse;
import com.example.devicemanagement.dto.response.WelcomeBoardVO;
import com.example.devicemanagement.service.WelcomeBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/welcome-board")
@CrossOrigin(origins = "*")
public class WelcomeBoardController {

    @Autowired
    private WelcomeBoardService boardService;

    @PostMapping
    public ApiResponse<WelcomeBoardVO> createBoard(@RequestBody WelcomeBoardCreateRequest request) {
        return ApiResponse.success("欢迎牌排期登记成功", boardService.createBoard(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<WelcomeBoardVO>> getBoards(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean overdueOnly) {
        IPage<WelcomeBoardVO> page = boardService.getBoardsPage(pageNum, pageSize, floorId, roomId, status, overdueOnly);
        return ApiResponse.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/overdue")
    public ApiResponse<List<WelcomeBoardVO>> getOverdueBoards() {
        return ApiResponse.success(boardService.getOverdueBoards());
    }

    @GetMapping("/{id}")
    public ApiResponse<WelcomeBoardVO> getBoard(@PathVariable Long id) {
        return ApiResponse.success(boardService.getBoardById(id));
    }

    @PostMapping("/{id}/remove")
    public ApiResponse<WelcomeBoardVO> removeBoard(@PathVariable Long id,
                                                   @RequestBody WelcomeBoardRemoveRequest request) {
        return ApiResponse.success("撤下回执已登记，欢迎牌已撤下", boardService.removeBoard(id, request));
    }

    @GetMapping("/room-availability")
    public ApiResponse<List<BoardRoomAvailabilityVO>> getRoomAvailability(
            @RequestParam(required = false) Long floorId) {
        return ApiResponse.success(boardService.getRoomAvailability(floorId));
    }
}
