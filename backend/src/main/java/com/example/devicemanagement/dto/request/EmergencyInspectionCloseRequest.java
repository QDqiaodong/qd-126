package com.example.devicemanagement.dto.request;

import lombok.Data;

/**
 * 补检关闭：必须写检查结果（1合格 2不合格）才能拿掉待检标记。
 */
@Data
public class EmergencyInspectionCloseRequest {

    /** 检查结果 1合格 2不合格，必填 */
    private Integer checkResult;

    /** 结果说明（合格/不合格情况描述），可填 */
    private String resultRemark;

    /** 关闭登记人 */
    private String closedBy;
}
