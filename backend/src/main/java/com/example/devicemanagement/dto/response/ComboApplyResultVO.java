package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ComboApplyResultVO {

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

    /** 本次调入台数 */
    private Integer appliedCount;

    /** 套用前已在房间台数 */
    private Integer presentCount;

    /** 跳过台数 */
    private Integer skippedCount;

    private String remark;

    /** 套用前接待室设备清单 */
    private List<ComboDeviceVO> beforeDevices;

    /** 套用后接待室设备清单 */
    private List<ComboDeviceVO> afterDevices;

    /** 组合内每台设备的套用结果 */
    private List<ComboRecordItemVO> items;
}
