package com.example.devicemanagement.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class DeviceComboRequest {

    /**
     * 组合名称
     */
    private String comboName;

    /**
     * 组合说明
     */
    private String remark;

    /**
     * 状态 1启用 0停用
     */
    private Integer status;

    /**
     * 创建人（管理员）
     */
    private String createdBy;

    /**
     * 组合内设备ID（按选择顺序保存）
     */
    private List<Long> deviceIds;
}
