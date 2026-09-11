package com.example.devicemanagement.controller;

import com.example.devicemanagement.dto.request.SpecTemplateRequest;
import com.example.devicemanagement.dto.response.ApiResponse;
import com.example.devicemanagement.dto.response.SpecTemplatePreviewVO;
import com.example.devicemanagement.dto.response.SpecTemplateVO;
import com.example.devicemanagement.service.SpecTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spec-template")
@CrossOrigin(origins = "*")
public class SpecTemplateController {

    @Autowired
    private SpecTemplateService specTemplateService;

    @PostMapping
    public ApiResponse<SpecTemplateVO> createTemplate(@RequestBody SpecTemplateRequest request) {
        return ApiResponse.success(specTemplateService.createTemplate(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SpecTemplateVO> updateTemplate(@PathVariable Long id,
                                                      @RequestBody SpecTemplateRequest request) {
        return ApiResponse.success(specTemplateService.updateTemplate(id, request));
    }

    /**
     * 编辑保存前的变更预览：新增/删除/类型变化字段及受影响设备数量，不落库。
     */
    @PostMapping("/{id}/preview")
    public ApiResponse<SpecTemplatePreviewVO> previewChanges(@PathVariable Long id,
                                                             @RequestBody SpecTemplateRequest request) {
        return ApiResponse.success(specTemplateService.previewChanges(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<SpecTemplateVO> updateStatus(@PathVariable Long id,
                                                    @RequestBody Map<String, Integer> body) {
        return ApiResponse.success(specTemplateService.updateStatus(id, body.get("status")));
    }

    @GetMapping("/{id}")
    public ApiResponse<SpecTemplateVO> getTemplateById(@PathVariable Long id) {
        return ApiResponse.success(specTemplateService.getTemplateById(id));
    }

    @GetMapping("/type/{deviceType}")
    public ApiResponse<SpecTemplateVO> getTemplateByType(@PathVariable String deviceType) {
        return ApiResponse.success(specTemplateService.getTemplateByType(deviceType));
    }

    @GetMapping
    public ApiResponse<List<SpecTemplateVO>> getAllTemplates() {
        return ApiResponse.success(specTemplateService.getAllTemplates());
    }

    @GetMapping("/enabled")
    public ApiResponse<List<SpecTemplateVO>> getEnabledTemplates() {
        return ApiResponse.success(specTemplateService.getEnabledTemplates());
    }
}
