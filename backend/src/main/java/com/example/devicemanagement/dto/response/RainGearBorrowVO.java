package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RainGearBorrowVO {

    private Long id;
    private String borrowNo;

    private Long roomId;
    private String roomName;
    private String roomCode;
    private Long floorId;
    private String floorName;

    /** 雨具类型编码 UMBRELLA/RAINCOAT */
    private String gearType;
    /** 雨具类型文案 雨伞/雨衣 */
    private String gearTypeText;

    private String borrower;
    private LocalDateTime expectedReturnTime;

    private Integer status;
    private String statusText;

    /** 是否逾期：在借且当前时间已过预计归还时间（派生字段，刷新后重新计算） */
    private Boolean overdue;

    private LocalDateTime returnedAt;
    private String returnedBy;

    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
