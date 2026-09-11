package com.example.devicemanagement.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoomActivityCreateRequest {

    /** 活动名称 */
    private String activityName;

    /** 接待室ID（按楼层选择接待室） */
    private Long roomId;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 预计使用的影音设备ID列表 */
    private List<Long> deviceIds;

    /** 负责人 */
    private String manager;

    /** 备注 */
    private String remark;
}
