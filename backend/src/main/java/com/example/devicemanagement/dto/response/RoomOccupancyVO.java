package com.example.devicemanagement.dto.response;

import lombok.Data;

/**
 * 接待室当前占用状态（进行中活动即占用）。
 */
@Data
public class RoomOccupancyVO {

    private Long roomId;
    private String roomName;
    private Long floorId;
    private String floorName;

    /** 是否正被进行中的活动占用 */
    private Boolean occupied;

    /** 当前进行中的活动ID（占用时返回） */
    private Long activityId;
    private String activityName;
    private String manager;
}
