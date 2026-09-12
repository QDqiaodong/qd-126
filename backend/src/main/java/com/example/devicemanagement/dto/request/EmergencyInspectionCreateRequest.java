package com.example.devicemanagement.dto.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * 到期补检开单：只记录检查人和检查日期，结果在关闭时填写。
 */
@Data
public class EmergencyInspectionCreateRequest {

    private Long lightId;

    private String inspector;

    private LocalDate inspectDate;

    private String remark;
}
