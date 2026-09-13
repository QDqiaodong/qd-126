package com.example.devicemanagement.dto.response;

import lombok.Data;

/**
 * 绿植待养汇总：当前待养件数与其中逾期件数（逾期按当前时间派生）。
 */
@Data
public class PlantCareStatsVO {

    private Long floorId;
    private String floorName;
    private Long roomId;
    private String roomName;

    /** 待养件数（未浇水核销） */
    private Long activeCount;
    /** 其中已逾期件数 */
    private Long overdueCount;
}
