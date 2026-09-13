package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.RoomActivityCreateRequest;
import com.example.devicemanagement.dto.response.RoomActivityVO;
import com.example.devicemanagement.dto.response.RoomOccupancyVO;

import java.time.LocalDate;
import java.util.List;

public interface RoomActivityService {

    /**
     * 登记接待室活动占用：校验同一接待室时段重叠、静音时段重叠、设备占用冲突。
     */
    RoomActivityVO createActivity(RoomActivityCreateRequest request);

    /**
     * 修改待开始活动的占用信息（名称、接待室、时段、设备、负责人）。
     * 时段与静音重叠同样拦截；已开始/已结束的活动只提示，不改历史。
     */
    RoomActivityVO updateActivity(Long activityId, RoomActivityCreateRequest request);

    /**
     * 按日期、楼层、状态分页筛选活动（状态按当前时间懒推进）。
     */
    IPage<RoomActivityVO> getActivitiesPage(int pageNum, int pageSize,
                                            LocalDate date, Long floorId, Integer status);

    /**
     * 活动详情：占用设备清单与冲突提示。
     */
    RoomActivityVO getActivityById(Long activityId);

    /**
     * 活动结束，释放占用（手动提前结束；到期活动在查询时自动释放）。
     */
    RoomActivityVO finishActivity(Long activityId);

    /**
     * 接待室当前占用状态，按楼层可过滤。
     */
    List<RoomOccupancyVO> getRoomOccupancies(Long floorId);

    /**
     * 设备是否被进行中的活动占用；设备调配前调用，占用则拒绝调配。
     */
    void assertDeviceTransferable(Long deviceId);

    /**
     * 设备被进行中的活动占用时返回该活动名称，未占用返回 null。用于组合套用跳过原因展示。
     */
    String getBlockingActivityName(Long deviceId);
}
