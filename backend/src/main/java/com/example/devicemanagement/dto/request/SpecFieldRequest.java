package com.example.devicemanagement.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SpecFieldRequest {

    /**
     * 编辑已有字段时传入，新增字段为空
     */
    private Long id;

    private String fieldKey;

    private String fieldLabel;

    /**
     * 字段类型 text/textarea/number/select/date/boolean
     */
    private String fieldType;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 可选项，仅select类型使用
     */
    private List<String> options;

    /**
     * 排序号
     */
    private Integer sortOrder;
}
