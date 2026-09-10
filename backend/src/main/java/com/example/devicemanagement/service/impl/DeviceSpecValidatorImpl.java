package com.example.devicemanagement.service.impl;

import com.example.devicemanagement.dto.response.SpecFieldVO;
import com.example.devicemanagement.service.DeviceSpecValidator;
import com.example.devicemanagement.service.SpecTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备规格参数按模板校验：
 * 1. 必填字段不能为空；
 * 2. 下拉选择字段的值必须在模板选项内；
 * 3. 数字字段必须为合法数字；
 * 4. 模板停用或类型没有模板时不强制校验，保留历史设备规格。
 */
@Component
public class DeviceSpecValidatorImpl implements DeviceSpecValidator {

    @Autowired
    private SpecTemplateService specTemplateService;

    @Override
    public Map<String, Object> validate(String deviceType, Map<String, Object> specJson) {
        List<SpecFieldVO> fields = specTemplateService.getEnabledFields(deviceType);
        if (fields.isEmpty()) {
            // 没有启用模板（含模板停用的情况），不做强制校验，保留原始规格
            return specJson;
        }
        return validateByFields(fields, specJson, true);
    }

    @Override
    public Map<String, Object> validateByFields(List<SpecFieldVO> fields, Map<String, Object> specJson, boolean enforceRequired) {
        Map<String, Object> input = specJson != null ? specJson : new LinkedHashMap<>();
        Map<String, Object> normalized = new LinkedHashMap<>();

        for (SpecFieldVO field : fields) {
            Object rawValue = input.get(field.getFieldKey());
            String strValue = rawValue == null ? "" : String.valueOf(rawValue).trim();

            if (strValue.isEmpty()) {
                if (enforceRequired && Boolean.TRUE.equals(field.getRequired())) {
                    throw new IllegalArgumentException("规格参数【" + field.getFieldLabel() + "】为必填项");
                }
                continue;
            }

            Object value;
            switch (field.getFieldType()) {
                case "number":
                    value = parseNumber(field, strValue);
                    break;
                case "boolean":
                    value = parseBoolean(strValue);
                    break;
                case "select":
                    value = parseSelect(field, strValue);
                    break;
                default:
                    // text / textarea / date 均按字符串存储
                    value = strValue;
                    break;
            }
            normalized.put(field.getFieldKey(), value);
        }

        // 模板未定义的历史/额外字段原样保留，避免编辑时丢数据
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (!normalized.containsKey(entry.getKey()) && entry.getValue() != null) {
                normalized.put(entry.getKey(), entry.getValue());
            }
        }
        return normalized;
    }

    private Number parseNumber(SpecFieldVO field, String strValue) {
        try {
            if (strValue.contains(".")) {
                return new BigDecimal(strValue).doubleValue();
            }
            return Long.parseLong(strValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("规格参数【" + field.getFieldLabel() + "】必须是数字");
        }
    }

    private Boolean parseBoolean(String strValue) {
        if ("true".equalsIgnoreCase(strValue) || "1".equals(strValue) || "是".equals(strValue)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(strValue) || "0".equals(strValue) || "否".equals(strValue)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("布尔型规格参数值不合法：" + strValue);
    }

    private String parseSelect(SpecFieldVO field, String strValue) {
        List<String> options = field.getOptions() != null ? field.getOptions() : new ArrayList<>();
        if (!options.contains(strValue)) {
            throw new IllegalArgumentException("规格参数【" + field.getFieldLabel()
                    + "】的值必须是以下选项之一：" + String.join("、", options));
        }
        return strValue;
    }
}
