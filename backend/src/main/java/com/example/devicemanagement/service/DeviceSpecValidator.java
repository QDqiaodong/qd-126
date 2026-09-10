package com.example.devicemanagement.service;

import com.example.devicemanagement.dto.response.SpecFieldVO;

import java.util.Map;

/**
 * 按设备类型规格模板校验并归一化设备规格参数。
 */
public interface DeviceSpecValidator {

    /**
     * 按启用中的模板校验规格参数。
     * 模板停用时不做强制校验，历史设备规格原样保留。
     *
     * @param deviceType 设备类型
     * @param specJson   提交的规格参数
     * @return 归一化后的规格参数，未启用模板时原样返回
     */
    Map<String, Object> validate(String deviceType, Map<String, Object> specJson);

    /**
     * 按指定字段定义校验（编辑设备时模板已停用也可复用）。
     */
    Map<String, Object> validateByFields(java.util.List<SpecFieldVO> fields, Map<String, Object> specJson, boolean enforceRequired);
}
