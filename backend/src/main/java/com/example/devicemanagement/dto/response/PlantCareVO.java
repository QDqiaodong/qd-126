package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlantCareVO {

    private Long id;
    private String careNo;

    private Long roomId;
    private String roomName;
    private String roomCode;
    private Long floorId;
    private String floorName;

    /** 绿植名称 */
    private String plantName;
    /** 养护人 */
    private String caretaker;

    private LocalDateTime nextWaterTime;

    private Integer status;
    private String statusText;

    /** 是否逾期：待养护且当前时间已过下次浇水时间（派生字段，刷新后重新计算） */
    private Boolean overdue;

    private LocalDateTime wateredAt;
    private String wateredBy;

    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
