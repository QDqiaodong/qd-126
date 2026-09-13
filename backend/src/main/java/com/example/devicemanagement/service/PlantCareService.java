package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.PlantCareCreateRequest;
import com.example.devicemanagement.dto.request.PlantCareWaterRequest;
import com.example.devicemanagement.dto.response.PlantCareStatsVO;
import com.example.devicemanagement.dto.response.PlantCareVO;

public interface PlantCareService {

    /**
     * 登记绿植养护：按楼层选择接待室，登记绿植名称、养护人、下次浇水时间。
     */
    PlantCareVO createCare(PlantCareCreateRequest request);

    /**
     * 浇水核销：待养件数减一，记录实际浇水时间与核销人；已核销的不可重复核销。
     */
    PlantCareVO waterCare(Long careId, PlantCareWaterRequest request);

    /**
     * 养护明细分页：支持按楼层、接待室、养护状态、是否逾期筛选。
     * overdue=true 时只返回待养护且已过下次浇水时间的记录。
     */
    IPage<PlantCareVO> getCaresPage(int pageNum, int pageSize, Long floorId, Long roomId,
                                    Integer status, Boolean overdue);

    /**
     * 养护详情（逾期标记按当前时间实时派生）。
     */
    PlantCareVO getCareById(Long careId);

    /**
     * 待养件数与逾期件数汇总（可按楼层、接待室过滤）。
     */
    PlantCareStatsVO getStats(Long floorId, Long roomId);
}
