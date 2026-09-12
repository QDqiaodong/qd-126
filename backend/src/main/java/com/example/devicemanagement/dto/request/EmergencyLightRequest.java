package com.example.devicemanagement.dto.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * 行政登记应急灯：编号、所在接待室、上次检查日、下次到期日。
 */
@Data
public class EmergencyLightRequest {

    private String lightCode;

    private String lightName;

    private Long roomId;

    private LocalDate lastCheckDate;

    private LocalDate nextDueDate;

    /** 检查周期（天），为空默认 30 天 */
    private Integer intervalDays;

    private Integer status;

    private String createdBy;

    private String remark;
}
