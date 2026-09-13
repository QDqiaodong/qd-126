package com.example.devicemanagement.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterpreterBookingRequest {

    /** 接待室ID（按楼层选择接待室） */
    private Long roomId;

    /** 翻译语种 */
    private String language;

    /** 随行译员姓名 */
    private String interpreterName;

    /** 预约开始时间 */
    private LocalDateTime startTime;

    /** 预约结束时间 */
    private LocalDateTime endTime;

    /** 备注 */
    private String remark;
}
