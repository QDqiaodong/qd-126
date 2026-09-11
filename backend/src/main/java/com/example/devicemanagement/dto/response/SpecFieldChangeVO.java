package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 模板字段变更项：用于编辑保存前的变更预览（新增/删除/类型变化）
 */
@Data
public class SpecFieldChangeVO {

    private String fieldKey;
    private String fieldLabel;
    private String fieldType;

    /**
     * 类型变化时的原类型
     */
    private String oldFieldType;

    /**
     * 类型变化时的新类型
     */
    private String newFieldType;

    private Boolean required;
    private List<String> options;
    private Integer sortOrder;
}
