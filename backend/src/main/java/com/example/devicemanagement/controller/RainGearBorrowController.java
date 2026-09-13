package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.RainGearBorrowRequest;
import com.example.devicemanagement.dto.request.RainGearReturnRequest;
import com.example.devicemanagement.dto.response.ApiResponse;
import com.example.devicemanagement.dto.response.PageResponse;
import com.example.devicemanagement.dto.response.RainGearBorrowStatsVO;
import com.example.devicemanagement.dto.response.RainGearBorrowVO;
import com.example.devicemanagement.service.RainGearBorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rain-gear")
@CrossOrigin(origins = "*")
public class RainGearBorrowController {

    @Autowired
    private RainGearBorrowService borrowService;

    @PostMapping("/borrow")
    public ApiResponse<RainGearBorrowVO> createBorrow(@RequestBody RainGearBorrowRequest request) {
        return ApiResponse.success("雨具借用登记成功", borrowService.createBorrow(request));
    }

    @PostMapping("/{id}/return")
    public ApiResponse<RainGearBorrowVO> returnBorrow(@PathVariable Long id,
                                                      @RequestBody(required = false) RainGearReturnRequest request) {
        return ApiResponse.success("归还核销成功", borrowService.returnBorrow(id, request));
    }

    @GetMapping
    public ApiResponse<PageResponse<RainGearBorrowVO>> getBorrows(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String gearType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean overdue) {
        IPage<RainGearBorrowVO> page =
                borrowService.getBorrowsPage(pageNum, pageSize, floorId, roomId, gearType, status, overdue);
        return ApiResponse.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/stats")
    public ApiResponse<RainGearBorrowStatsVO> getStats(@RequestParam(required = false) Long floorId,
                                                       @RequestParam(required = false) Long roomId) {
        return ApiResponse.success(borrowService.getStats(floorId, roomId));
    }

    @GetMapping("/{id}")
    public ApiResponse<RainGearBorrowVO> getBorrow(@PathVariable Long id) {
        return ApiResponse.success(borrowService.getBorrowById(id));
    }
}
