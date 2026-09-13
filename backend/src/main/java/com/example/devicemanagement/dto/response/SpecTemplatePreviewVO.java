package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 规格模板变更预览：保存前展示字段新增/删除/类型变化及受影响设备数量。
 * 变更只影响模板定义，不会覆盖历史设备已保存的规格数据。
 */
@Data
public class SpecTemplatePreviewVO {

    private Long templateId;
    private String deviceType;

    /**
     * 该类型下的设备总数
     */
    private Integer totalDeviceCount;

    /**
     * 已保存规格会受本次字段变更（删除/类型变化）影响的设备数量
     */
    private Integer affectedDeviceCount;

    /**
     * 正在引用该模板的设备名称（该设备类型下的全部设备），保存前弹窗逐台展示
     */
    private List<String> referencingDeviceNames;

    private List<SpecFieldChangeVO> addedFields;
    private List<SpecFieldChangeVO> removedFields;
    private List<SpecFieldChangeVO> typeChangedFields;

    /**
     * 是否存在实质变更
     */
    private Boolean changed;
}
