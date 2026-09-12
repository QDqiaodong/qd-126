package com.example.devicemanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.ComboApplyRequest;
import com.example.devicemanagement.dto.request.DeviceComboRequest;
import com.example.devicemanagement.dto.response.ComboApplyResultVO;
import com.example.devicemanagement.dto.response.ComboRecordVO;
import com.example.devicemanagement.dto.response.DeviceComboVO;

import java.util.List;

public interface DeviceComboService {

    /**
     * 管理员创建常用影音组合（一组固定设备）。
     */
    DeviceComboVO createCombo(DeviceComboRequest request);

    /**
     * 编辑组合：改名、改说明、启停、替换设备清单。
     */
    DeviceComboVO updateCombo(Long id, DeviceComboRequest request);

    /**
     * 删除组合（历史套用记录保留快照，不受影响）。
     */
    void deleteCombo(Long id);

    DeviceComboVO getComboById(Long id);

    /**
     * 组合列表，可按状态过滤；设备位置/状态取实时台账。
     */
    List<DeviceComboVO> listCombos(Integer status);

    /**
     * 值班员一键套用：空闲设备调入接待室，已在别的房间/待修/损坏/活动占用的跳过并写明原因。
     */
    ComboApplyResultVO applyCombo(Long comboId, ComboApplyRequest request);

    /**
     * 套用记录分页，可按楼层、接待室、组合过滤。
     */
    IPage<ComboRecordVO> getRecordsPage(int pageNum, int pageSize, Long floorId, Long roomId, Long comboId);

    /**
     * 套用记录详情：套用前后清单快照 + 逐台结果（实时台账信息）。
     */
    ComboRecordVO getRecordById(Long id);
}
