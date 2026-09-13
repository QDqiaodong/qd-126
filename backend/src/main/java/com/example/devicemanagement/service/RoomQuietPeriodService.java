package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.RoomQuietPeriodRequest;
import com.example.devicemanagement.dto.response.RoomQuietPeriodVO;
import com.example.devicemanagement.entity.RoomQuietPeriod;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomQuietPeriodService {

    /**
     * 给接待室登记静音时段（起止时间 + 原因）。
     */
    RoomQuietPeriodVO createQuietPeriod(RoomQuietPeriodRequest request);

    /**
     * 修改静音时段（起止时间、原因可改）。
     */
    RoomQuietPeriodVO updateQuietPeriod(Long quietPeriodId, RoomQuietPeriodRequest request);

    /**
     * 删除静音时段；删除后该接待室对应时段恢复可正常占用。
     */
    void deleteQuietPeriod(Long quietPeriodId);

    /**
     * 静音时段分页列表，按楼层、接待室筛选查看。
     */
    IPage<RoomQuietPeriodVO> getQuietPeriodsPage(int pageNum, int pageSize, Long floorId, Long roomId);

    /**
     * 某接待室全部静音时段（按开始时间升序），用于活动登记表单提示。
     */
    List<RoomQuietPeriodVO> getRoomQuietPeriods(Long roomId);

    /**
     * 与指定时段重叠的静音时段（边界相接不算重叠）；无静音设置的接待室返回空。
     */
    List<RoomQuietPeriod> findOverlapping(Long roomId, LocalDateTime start, LocalDateTime end);
}
