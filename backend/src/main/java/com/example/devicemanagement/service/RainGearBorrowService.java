package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.RainGearBorrowRequest;
import com.example.devicemanagement.dto.request.RainGearReturnRequest;
import com.example.devicemanagement.dto.response.RainGearBorrowStatsVO;
import com.example.devicemanagement.dto.response.RainGearBorrowVO;

public interface RainGearBorrowService {

    /**
     * 登记雨具借用：按楼层选择接待室，登记雨伞/雨衣、借出人、预计归还时间。
     */
    RainGearBorrowVO createBorrow(RainGearBorrowRequest request);

    /**
     * 归还核销：在借件数减一，记录实际归还时间与核销人；已核销的不可重复核销。
     */
    RainGearBorrowVO returnBorrow(Long borrowId, RainGearReturnRequest request);

    /**
     * 借用明细分页：支持按楼层、接待室、雨具类型、是否在借、是否逾期筛选。
     * overdue=true 时只返回在借且已过预计归还时间的记录。
     */
    IPage<RainGearBorrowVO> getBorrowsPage(int pageNum, int pageSize, Long floorId, Long roomId,
                                           String gearType, Integer status, Boolean overdue);

    /**
     * 借用详情（逾期标记按当前时间实时派生）。
     */
    RainGearBorrowVO getBorrowById(Long borrowId);

    /**
     * 在借件数与逾期件数汇总（可按楼层、接待室过滤）。
     */
    RainGearBorrowStatsVO getStats(Long floorId, Long roomId);
}
