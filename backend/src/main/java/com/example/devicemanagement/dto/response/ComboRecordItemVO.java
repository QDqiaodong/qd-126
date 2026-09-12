package com.example.devicemanagement.dto.response;

import lombok.Data;

@Data
public class ComboRecordItemVO {

    private Long id;

    private Long recordId;

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private String deviceType;

    /** 套用结果 1调入 2已在房间 3跳过 */
    private Integer result;

    private String resultText;

    /** 跳过原因（在别的房间/待修/损坏/活动占用等） */
    private String skipReason;

    private Long fromRoomId;

    private String fromRoomName;

    private Integer sortOrder;

    // ---- 实时台账信息：刷新后套用结果与设备实际归属、状态保持一致 ----

    private Integer liveStatus;

    private String liveStatusText;

    private Long liveRoomId;

    private String liveRoomName;

    /**
     * 设备是否仍在目标接待室（仅调入/已在房间有意义；刷新后可发现事后又被调走）
     */
    private Boolean liveInTargetRoom;

    /**
     * 设备是否已从台账删除
     */
    private Boolean exists;
}
