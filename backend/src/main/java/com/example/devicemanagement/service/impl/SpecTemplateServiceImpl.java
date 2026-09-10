package com.example.devicemanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.devicemanagement.dto.request.SpecFieldRequest;
import com.example.devicemanagement.dto.request.SpecTemplateRequest;
import com.example.devicemanagement.dto.response.SpecFieldVO;
import com.example.devicemanagement.dto.response.SpecTemplateVO;
import com.example.devicemanagement.entity.DeviceSpecField;
import com.example.devicemanagement.entity.DeviceSpecTemplate;
import com.example.devicemanagement.mapper.DeviceSpecFieldMapper;
import com.example.devicemanagement.mapper.DeviceSpecTemplateMapper;
import com.example.devicemanagement.service.SpecTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SpecTemplateServiceImpl implements SpecTemplateService {

    private static final Set<String> SUPPORTED_FIELD_TYPES = Set.of(
            "text", "textarea", "number", "select", "date", "boolean");

    @Autowired
    private DeviceSpecTemplateMapper templateMapper;

    @Autowired
    private DeviceSpecFieldMapper fieldMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public SpecTemplateVO createTemplate(SpecTemplateRequest request) {
        validateTemplateRequest(request);

        DeviceSpecTemplate existing = findByType(request.getDeviceType());
        if (existing != null) {
            throw new IllegalArgumentException("设备类型【" + request.getDeviceType() + "】的规格模板已存在");
        }

        DeviceSpecTemplate template = new DeviceSpecTemplate();
        template.setDeviceType(request.getDeviceType().trim());
        template.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        templateMapper.insert(template);

        replaceFields(template.getId(), request.getFields());
        return getTemplateById(template.getId());
    }

    @Override
    @Transactional
    public SpecTemplateVO updateTemplate(Long id, SpecTemplateRequest request) {
        DeviceSpecTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new IllegalArgumentException("规格模板不存在");
        }
        validateTemplateRequest(request);

        DeviceSpecTemplate sameType = findByType(request.getDeviceType());
        if (sameType != null && !sameType.getId().equals(id)) {
            throw new IllegalArgumentException("设备类型【" + request.getDeviceType() + "】已存在其他规格模板");
        }

        template.setDeviceType(request.getDeviceType().trim());
        if (request.getStatus() != null) {
            template.setStatus(request.getStatus());
        }
        templateMapper.updateById(template);

        replaceFields(template.getId(), request.getFields());
        return getTemplateById(id);
    }

    @Override
    @Transactional
    public SpecTemplateVO updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态值不合法");
        }
        DeviceSpecTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new IllegalArgumentException("规格模板不存在");
        }
        template.setStatus(status);
        templateMapper.updateById(template);
        return getTemplateById(id);
    }

    @Override
    public SpecTemplateVO getTemplateById(Long id) {
        DeviceSpecTemplate template = templateMapper.selectById(id);
        if (template == null) {
            return null;
        }
        return convertToVO(template, listFieldEntities(template.getId()));
    }

    @Override
    public SpecTemplateVO getTemplateByType(String deviceType) {
        DeviceSpecTemplate template = findByType(deviceType);
        if (template == null) {
            return null;
        }
        return convertToVO(template, listFieldEntities(template.getId()));
    }

    @Override
    public List<SpecTemplateVO> getAllTemplates() {
        LambdaQueryWrapper<DeviceSpecTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(DeviceSpecTemplate::getId);
        List<DeviceSpecTemplate> templates = templateMapper.selectList(wrapper);
        return templates.stream()
                .map(t -> convertToVO(t, listFieldEntities(t.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpecTemplateVO> getEnabledTemplates() {
        LambdaQueryWrapper<DeviceSpecTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceSpecTemplate::getStatus, 1);
        wrapper.orderByAsc(DeviceSpecTemplate::getId);
        List<DeviceSpecTemplate> templates = templateMapper.selectList(wrapper);
        return templates.stream()
                .map(t -> convertToVO(t, listFieldEntities(t.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpecFieldVO> getEnabledFields(String deviceType) {
        DeviceSpecTemplate template = getEnabledTemplateEntity(deviceType);
        if (template == null) {
            return new ArrayList<>();
        }
        return listFieldEntities(template.getId()).stream()
                .map(this::convertFieldToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpecFieldVO> getAnyStatusFields(String deviceType) {
        DeviceSpecTemplate template = findByType(deviceType);
        if (template == null) {
            return new ArrayList<>();
        }
        return listFieldEntities(template.getId()).stream()
                .map(this::convertFieldToVO)
                .collect(Collectors.toList());
    }

    @Override
    public DeviceSpecTemplate getEnabledTemplateEntity(String deviceType) {
        if (deviceType == null || deviceType.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<DeviceSpecTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceSpecTemplate::getDeviceType, deviceType.trim());
        wrapper.eq(DeviceSpecTemplate::getStatus, 1);
        return templateMapper.selectOne(wrapper);
    }

    private DeviceSpecTemplate findByType(String deviceType) {
        if (deviceType == null || deviceType.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<DeviceSpecTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceSpecTemplate::getDeviceType, deviceType.trim());
        return templateMapper.selectOne(wrapper);
    }

    private List<DeviceSpecField> listFieldEntities(Long templateId) {
        LambdaQueryWrapper<DeviceSpecField> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceSpecField::getTemplateId, templateId);
        wrapper.orderByAsc(DeviceSpecField::getSortOrder);
        wrapper.orderByAsc(DeviceSpecField::getId);
        return fieldMapper.selectList(wrapper);
    }

    /**
     * 保存模板时整体覆盖字段定义；历史设备 spec_json 不受影响。
     */
    private void replaceFields(Long templateId, List<SpecFieldRequest> fields) {
        LambdaQueryWrapper<DeviceSpecField> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(DeviceSpecField::getTemplateId, templateId);
        fieldMapper.delete(deleteWrapper);

        if (fields == null || fields.isEmpty()) {
            return;
        }

        Set<String> keySet = new HashSet<>();
        int index = 0;
        for (SpecFieldRequest field : fields) {
            String key = field.getFieldKey() == null ? "" : field.getFieldKey().trim();
            String label = field.getFieldLabel() == null ? "" : field.getFieldLabel().trim();
            String type = field.getFieldType() == null ? "" : field.getFieldType().trim();

            if (key.isEmpty() || label.isEmpty()) {
                throw new IllegalArgumentException("规格字段的字段键和字段名称不能为空");
            }
            if (!SUPPORTED_FIELD_TYPES.contains(type)) {
                throw new IllegalArgumentException("字段【" + label + "】的字段类型不合法：" + type);
            }
            if (!keySet.add(key)) {
                throw new IllegalArgumentException("字段键【" + key + "】在同一模板内重复");
            }
            if ("select".equals(type)
                    && (field.getOptions() == null || field.getOptions().isEmpty())) {
                throw new IllegalArgumentException("下拉选择字段【" + label + "】必须配置选项");
            }

            DeviceSpecField entity = new DeviceSpecField();
            entity.setTemplateId(templateId);
            entity.setFieldKey(key);
            entity.setFieldLabel(label);
            entity.setFieldType(type);
            entity.setRequired(Boolean.TRUE.equals(field.getRequired()) ? 1 : 0);
            entity.setOptions(writeOptions(field.getOptions()));
            entity.setSortOrder(field.getSortOrder() != null ? field.getSortOrder() : index);
            fieldMapper.insert(entity);
            index++;
        }
    }

    private void validateTemplateRequest(SpecTemplateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (request.getDeviceType() == null || request.getDeviceType().trim().isEmpty()) {
            throw new IllegalArgumentException("设备类型不能为空");
        }
        if (request.getStatus() != null && request.getStatus() != 0 && request.getStatus() != 1) {
            throw new IllegalArgumentException("状态值不合法");
        }
    }

    private String writeOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        List<String> trimmed = options.stream()
                .filter(o -> o != null && !o.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toList());
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(trimmed);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("可选项序列化失败");
        }
    }

    private List<String> readOptions(String options) {
        if (options == null || options.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(options, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private SpecTemplateVO convertToVO(DeviceSpecTemplate template, List<DeviceSpecField> fields) {
        SpecTemplateVO vo = new SpecTemplateVO();
        vo.setId(template.getId());
        vo.setDeviceType(template.getDeviceType());
        vo.setStatus(template.getStatus());
        vo.setStatusText(template.getStatus() != null && template.getStatus() == 1 ? "启用" : "停用");
        vo.setFields(fields.stream().map(this::convertFieldToVO).collect(Collectors.toList()));
        vo.setCreatedAt(template.getCreatedAt());
        vo.setUpdatedAt(template.getUpdatedAt());
        return vo;
    }

    private SpecFieldVO convertFieldToVO(DeviceSpecField field) {
        SpecFieldVO vo = new SpecFieldVO();
        vo.setId(field.getId());
        vo.setFieldKey(field.getFieldKey());
        vo.setFieldLabel(field.getFieldLabel());
        vo.setFieldType(field.getFieldType());
        vo.setRequired(field.getRequired() != null && field.getRequired() == 1);
        vo.setOptions(readOptions(field.getOptions()));
        vo.setSortOrder(field.getSortOrder());
        return vo;
    }
}
