package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_spec_field")
public class DeviceSpecField {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("template_id")
    private Long templateId;

    @TableField("field_key")
    private String fieldKey;

    @TableField("field_label")
    private String fieldLabel;

    /**
     * 字段类型 text/textarea/number/select/date/boolean
     */
    @TableField("field_type")
    private String fieldType;

    /**
     * 是否必填 1是 0否
     */
    @TableField("`required`")
    private Integer required;

    /**
     * 可选项JSON，仅select类型使用
     */
    @TableField("options")
    private String options;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
