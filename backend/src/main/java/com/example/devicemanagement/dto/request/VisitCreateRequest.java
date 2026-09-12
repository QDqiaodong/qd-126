package com.example.devicemanagement.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitCreateRequest {

    /** 来访单位 */
    private String visitorOrg;

    /** 预计人数 */
    private Integer visitorCount;

    /** 接待室ID */
    private Long roomId;

    /** 预计开始时间 */
    private LocalDateTime startTime;

    /** 预计结束时间 */
    private LocalDateTime endTime;

    /** 备注 */
    private String remark;
}
