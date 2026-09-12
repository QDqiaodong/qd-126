package com.example.devicemanagement.dto.request;

import lombok.Data;

@Data
public class ComboApplyRequest {

    /**
     * 目标接待室ID
     */
    private Long roomId;

    /**
     * 套用人（值班员）
     */
    private String operator;

    /**
     * 备注
     */
    private String remark;
}
