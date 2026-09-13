package com.example.devicemanagement.dto.response;

import lombok.Data;

/**
 * 雨具在借汇总：当前在借件数与其中逾期件数（逾期按当前时间派生）。
 */
@Data
public class RainGearBorrowStatsVO {

    private Long floorId;
    private String floorName;
    private Long roomId;
    private String roomName;

    /** 在借件数（未核销） */
    private Long activeCount;
    /** 其中已逾期件数 */
    private Long overdueCount;
}
