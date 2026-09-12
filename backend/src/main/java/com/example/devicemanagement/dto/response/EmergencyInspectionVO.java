package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EmergencyInspectionVO {

    private Long id;
    private String inspectionNo;

    private Long lightId;
    private String lightCode;
    private String lightName;
    private Long roomId;
    private String roomName;
    private Long floorId;
    private String floorName;

    private String inspector;
    private LocalDate inspectDate;

    /** 1合格 2不合格，null=未关闭 */
    private Integer checkResult;
    private String checkResultText;
    private String resultRemark;
    private String closedBy;
    private LocalDateTime closedAt;
    private Boolean closed;

    private String remark;
    private LocalDateTime createdAt;
}
