package com.example.devicemanagement.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomQuietPeriodRequest {

    /** 接待室ID（按楼层选择接待室） */
    private Long roomId;

    /** 静音开始时间 */
    private LocalDateTime startTime;

    /** 静音结束时间 */
    private LocalDateTime endTime;

    /** 静音原因 */
    private String reason;

    /** 备注 */
    private String remark;
}
