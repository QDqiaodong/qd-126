package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ComboRecordVO {

    private Long id;

    private String recordNo;

    private Long comboId;

    private String comboName;

    private Long roomId;

    private String roomName;

    private String roomCode;

    private Long floorId;

    private String floorName;

    private String operator;

    private LocalDateTime applyTime;

    private Integer requiredCount;

    private Integer appliedCount;

    private Integer presentCount;

    private Integer skippedCount;

    private String remark;

    private LocalDateTime createdAt;

    /** 套用前接待室设备清单快照 */
    private List<ComboDeviceVO> beforeDevices;

    /** 套用后接待室设备清单快照 */
    private List<ComboDeviceVO> afterDevices;

    /** 逐台套用结果（含实时台账信息） */
    private List<ComboRecordItemVO> items;
}
