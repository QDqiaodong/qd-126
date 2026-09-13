package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.InterpreterBookingRequest;
import com.example.devicemanagement.dto.response.InterpreterBookingVO;

import java.util.List;

public interface InterpreterBookingService {

    /**
     * 给接待室预约随行翻译（语种、译员、时段）。
     * 同一译员在重叠时段已被其他接待室预约时拦截，并带出已约接待室。
     */
    InterpreterBookingVO createBooking(InterpreterBookingRequest request);

    /**
     * 修改翻译预约；活动已开始（进行中/已结束）后只允许查看，不能改译员或时段。
     */
    InterpreterBookingVO updateBooking(Long bookingId, InterpreterBookingRequest request);

    /**
     * 删除待开始的翻译预约；活动已开始后只允许查看。
     */
    void deleteBooking(Long bookingId);

    /**
     * 翻译预约分页列表，按楼层、接待室筛选查看。
     */
    IPage<InterpreterBookingVO> getBookingsPage(int pageNum, int pageSize, Long floorId, Long roomId);

    /**
     * 预约详情（查看）。
     */
    InterpreterBookingVO getBookingById(Long bookingId);

    /**
     * 某接待室全部翻译预约（按开始时间升序）。
     */
    List<InterpreterBookingVO> getRoomBookings(Long roomId);
}
