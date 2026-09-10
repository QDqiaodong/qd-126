package com.example.devicemanagement.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SpecTemplateRequest {

    /**
     * 设备类型，同一类型只允许存在一个模板
     */
    private String deviceType;

    /**
     * 状态 1启用 0停用
     */
    private Integer status;

    /**
     * 规格字段定义，保存时整体覆盖原字段
     */
    private List<SpecFieldRequest> fields;
}
