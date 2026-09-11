package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.RoomActivityCreateRequest;
import com.example.devicemanagement.dto.response.ApiResponse;
import com.example.devicemanagement.dto.response.PageResponse;
import com.example.devicemanagement.dto.response.RoomActivityVO;
import com.example.devicemanagement.dto.response.RoomOccupancyVO;
import com.example.devicemanagement.service.RoomActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/activity")
@CrossOrigin(origins = "*")
public class RoomActivityController {

    @Autowired
    private RoomActivityService activityService;

    @PostMapping
    public ApiResponse<RoomActivityVO> createActivity(@RequestBody RoomActivityCreateRequest request) {
        return ApiResponse.success("活动登记成功", activityService.createActivity(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<RoomActivityVO>> getActivities(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) Integer status) {
        IPage<RoomActivityVO> page = activityService.getActivitiesPage(pageNum, pageSize, date, floorId, status);
        return ApiResponse.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomActivityVO> getActivity(@PathVariable Long id) {
        return ApiResponse.success(activityService.getActivityById(id));
    }

    @PostMapping("/{id}/finish")
    public ApiResponse<RoomActivityVO> finishActivity(@PathVariable Long id) {
        return ApiResponse.success("活动已结束，占用设备已释放", activityService.finishActivity(id));
    }

    @GetMapping("/room-occupancy")
    public ApiResponse<List<RoomOccupancyVO>> getRoomOccupancies(
            @RequestParam(required = false) Long floorId) {
        return ApiResponse.success(activityService.getRoomOccupancies(floorId));
    }
}
