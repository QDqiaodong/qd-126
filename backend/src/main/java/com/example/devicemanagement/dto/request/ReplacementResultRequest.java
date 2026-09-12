package com.example.devicemanagement.dto.request;

import lombok.Data;

@Data
public class ReplacementResultRequest {

    /**
     * 处理结果 2已修复 3已报废
     */
    private Integer processResult;

    /**
     * 处理备注（维修/报废说明）
     */
    private String processRemark;

    /**
     * 处理登记人
     */
    private String processedBy;
}
