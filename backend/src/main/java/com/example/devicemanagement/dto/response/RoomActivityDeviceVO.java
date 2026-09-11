package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomActivityDeviceVO {

    private Long id;
    private Long activityId;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String deviceType;

    /** 设备当前台账归属楼层ID（用于占用一致性提示） */
    private Long currentFloorId;
    private String currentFloorName;
    private Long currentRoomId;
    private String currentRoomName;

    /** 设备状态 1正常 0损坏 2待维修 */
    private Integer deviceStatus;
    private String deviceStatusText;

    /** 冲突提示：设备已不在登记接待室 / 被其他进行中活动占用 等 */
    private String conflict;
}
