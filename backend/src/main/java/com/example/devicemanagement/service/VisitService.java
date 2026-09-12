package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.VisitCreateRequest;
import com.example.devicemanagement.dto.response.RoomReceptionVO;
import com.example.devicemanagement.dto.response.VisitVO;

import java.time.LocalDate;
import java.util.List;

public interface VisitService {

    /** 管理员登记来访单（同一接待室时段撞车直接拦截） */
    VisitVO createVisit(VisitCreateRequest request);

    /** 分页查询来访单（可按日期、楼层、状态筛选），查询时推进到期状态 */
    IPage<VisitVO> getVisitsPage(int pageNum, int pageSize, LocalDate date, Long floorId, Integer status);

    /** 来访单详情，重新打开后状态与接待标记一致 */
    VisitVO getVisitById(Long visitId);

    /** 值班员确认到场：待到访 -> 接待中，接待室标出正在接待 */
    VisitVO checkIn(Long visitId);

    /** 取消来访：待到访/接待中 -> 已取消，立即拿掉接待标记 */
    VisitVO cancel(Long visitId);

    /** 接待室列表：正在接待标记 + 是否可接待，与来访状态对得上 */
    List<RoomReceptionVO> getRoomReceptions(Long floorId);
}
