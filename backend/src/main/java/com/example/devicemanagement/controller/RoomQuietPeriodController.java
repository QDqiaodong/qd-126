package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.RoomQuietPeriodRequest;
import com.example.devicemanagement.dto.response.ApiResponse;
import com.example.devicemanagement.dto.response.PageResponse;
import com.example.devicemanagement.dto.response.RoomQuietPeriodVO;
import com.example.devicemanagement.service.RoomQuietPeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiet-period")
@CrossOrigin(origins = "*")
public class RoomQuietPeriodController {

    @Autowired
    private RoomQuietPeriodService quietPeriodService;

    @PostMapping
    public ApiResponse<RoomQuietPeriodVO> createQuietPeriod(@RequestBody RoomQuietPeriodRequest request) {
        return ApiResponse.success("静音时段登记成功", quietPeriodService.createQuietPeriod(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoomQuietPeriodVO> updateQuietPeriod(@PathVariable Long id,
                                                            @RequestBody RoomQuietPeriodRequest request) {
        return ApiResponse.success("静音时段已更新", quietPeriodService.updateQuietPeriod(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteQuietPeriod(@PathVariable Long id) {
        quietPeriodService.deleteQuietPeriod(id);
        return ApiResponse.success("静音时段已删除", null);
    }

    @GetMapping
    public ApiResponse<PageResponse<RoomQuietPeriodVO>> getQuietPeriods(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) Long roomId) {
        IPage<RoomQuietPeriodVO> page = quietPeriodService.getQuietPeriodsPage(pageNum, pageSize, floorId, roomId);
        return ApiResponse.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/room/{roomId}")
    public ApiResponse<List<RoomQuietPeriodVO>> getRoomQuietPeriods(@PathVariable Long roomId) {
        return ApiResponse.success(quietPeriodService.getRoomQuietPeriods(roomId));
    }
}
