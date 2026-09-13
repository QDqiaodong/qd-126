package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.InterpreterBookingRequest;
import com.example.devicemanagement.dto.response.ApiResponse;
import com.example.devicemanagement.dto.response.InterpreterBookingVO;
import com.example.devicemanagement.dto.response.PageResponse;
import com.example.devicemanagement.service.InterpreterBookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interpreter-booking")
@CrossOrigin(origins = "*")
public class InterpreterBookingController {

    @Autowired
    private InterpreterBookingService bookingService;

    @PostMapping
    public ApiResponse<InterpreterBookingVO> createBooking(@RequestBody InterpreterBookingRequest request) {
        return ApiResponse.success("随行翻译预约成功", bookingService.createBooking(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<InterpreterBookingVO> updateBooking(@PathVariable Long id,
                                                           @RequestBody InterpreterBookingRequest request) {
        return ApiResponse.success("翻译预约已更新", bookingService.updateBooking(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ApiResponse.success("翻译预约已删除", null);
    }

    @GetMapping
    public ApiResponse<PageResponse<InterpreterBookingVO>> getBookings(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) Long roomId) {
        IPage<InterpreterBookingVO> page = bookingService.getBookingsPage(pageNum, pageSize, floorId, roomId);
        return ApiResponse.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<InterpreterBookingVO> getBooking(@PathVariable Long id) {
        return ApiResponse.success(bookingService.getBookingById(id));
    }

    @GetMapping("/room/{roomId}")
    public ApiResponse<List<InterpreterBookingVO>> getRoomBookings(@PathVariable Long roomId) {
        return ApiResponse.success(bookingService.getRoomBookings(roomId));
    }
}
