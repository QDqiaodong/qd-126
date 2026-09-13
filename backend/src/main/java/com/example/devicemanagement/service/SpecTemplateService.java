package com.example.devicemanagement.service;

import com.example.devicemanagement.dto.request.SpecTemplateRequest;
import com.example.devicemanagement.dto.response.SpecFieldVO;
import com.example.devicemanagement.dto.response.SpecTemplatePreviewVO;
import com.example.devicemanagement.dto.response.SpecTemplateVO;
import com.example.devicemanagement.entity.DeviceSpecTemplate;

import java.util.List;

public interface SpecTemplateService {

    SpecTemplateVO createTemplate(SpecTemplateRequest request);

    SpecTemplateVO updateTemplate(Long id, SpecTemplateRequest request);

    /**
     * 编辑保存前的变更预览：字段新增/删除/类型变化、受影响设备数量及正在引用该模板的设备名称。
     * 只做差异计算，不落库；同时对请求做完整校验，非法字段定义直接拦截。
     */
    SpecTemplatePreviewVO previewChanges(Long id, SpecTemplateRequest request);

    /**
     * 启用/停用模板。停用不影响历史设备已保存的规格数据。
     */
    SpecTemplateVO updateStatus(Long id, Integer status);

    SpecTemplateVO getTemplateById(Long id);

    SpecTemplateVO getTemplateByType(String deviceType);

    /**
     * 查询全部模板（含停用），管理页使用
     */
    List<SpecTemplateVO> getAllTemplates();

    /**
     * 查询启用中的模板，设备添加/编辑表单使用
     */
    List<SpecTemplateVO> getEnabledTemplates();

    /**
     * 取启用模板字段定义（按排序号）；无启用模板时返回空列表
     */
    List<SpecFieldVO> getEnabledFields(String deviceType);

    /**
     * 取任意状态的模板字段定义（停用模板也要支撑历史设备详情渲染/编辑）
     */
    List<SpecFieldVO> getAnyStatusFields(String deviceType);

    DeviceSpecTemplate getEnabledTemplateEntity(String deviceType);
}
