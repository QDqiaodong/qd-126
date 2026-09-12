package com.example.devicemanagement.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WelcomeBoardCreateRequest {

    /** 欢迎牌文案 */
    private String boardText;

    /** 接待室ID */
    private Long roomId;

    /** 上墙时间 */
    private LocalDateTime mountTime;

    /** 撤下时间（计划） */
    private LocalDateTime plannedRemoveTime;

    /** 登记人（行政） */
    private String registrar;

    /** 备注 */
    private String remark;
}
