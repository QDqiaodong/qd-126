package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoomActivityVO {

    private Long id;
    private String activityNo;
    private String activityName;

    private Long roomId;
    private String roomName;
    private String roomCode;
    private Long floorId;
    private String floorName;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String manager;

    /** 状态 0待开始 1进行中 2已结束 */
    private Integer status;
    private String statusText;

    /** 占用设备数 */
    private Integer deviceCount;

    private LocalDateTime releasedAt;
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 详情：占用设备清单（含冲突提示），列表接口不填充 */
    private List<RoomActivityDeviceVO> devices;

    /** 详情：冲突提示列表（时段冲突 / 设备冲突），列表接口不填充 */
    private List<String> conflicts;
}
