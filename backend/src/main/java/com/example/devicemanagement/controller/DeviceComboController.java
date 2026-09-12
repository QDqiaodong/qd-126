package com.example.devicemanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.devicemanagement.dto.request.ComboApplyRequest;
import com.example.devicemanagement.dto.request.DeviceComboRequest;
import com.example.devicemanagement.dto.response.ApiResponse;
import com.example.devicemanagement.dto.response.ComboApplyResultVO;
import com.example.devicemanagement.dto.response.ComboRecordVO;
import com.example.devicemanagement.dto.response.DeviceComboVO;
import com.example.devicemanagement.dto.response.PageResponse;
import com.example.devicemanagement.service.DeviceComboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/combo")
@CrossOrigin(origins = "*")
public class DeviceComboController {

    @Autowired
    private DeviceComboService comboService;

    /**
     * 管理员保存常用影音组合。
     */
    @PostMapping
    public ApiResponse<DeviceComboVO> create(@RequestBody DeviceComboRequest request) {
        return ApiResponse.success("组合创建成功", comboService.createCombo(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DeviceComboVO> update(@PathVariable Long id, @RequestBody DeviceComboRequest request) {
        return ApiResponse.success("组合更新成功", comboService.updateCombo(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        comboService.deleteCombo(id);
        return ApiResponse.success("组合已删除", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceComboVO> getById(@PathVariable Long id) {
        return ApiResponse.success(comboService.getComboById(id));
    }

    /**
     * 组合列表（值班员套用时只取启用组合）。
     */
    @GetMapping
    public ApiResponse<List<DeviceComboVO>> list(@RequestParam(required = false) Integer status) {
        return ApiResponse.success(comboService.listCombos(status));
    }

    /**
     * 值班员一键套用：空闲设备调入接待室，在别的房间/待修等设备跳过并写明原因。
     */
    @PostMapping("/{id}/apply")
    public ApiResponse<ComboApplyResultVO> apply(@PathVariable("id") Long comboId,
                                                  @RequestBody ComboApplyRequest request) {
        return ApiResponse.success("套用完成", comboService.applyCombo(comboId, request));
    }

    /**
     * 套用记录分页，可按楼层、接待室、组合过滤。
     */
    @GetMapping("/records")
    public ApiResponse<PageResponse<ComboRecordVO>> getRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long comboId) {
        IPage<ComboRecordVO> page = comboService.getRecordsPage(pageNum, pageSize, floorId, roomId, comboId);
        return ApiResponse.success(PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @GetMapping("/records/{id}")
    public ApiResponse<ComboRecordVO> getRecordById(@PathVariable Long id) {
        return ApiResponse.success(comboService.getRecordById(id));
    }
}
