package com.example.devicemanagement.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlantCareCreateRequest {

    /** 接待室ID（管理员按楼层选择接待室） */
    private Long roomId;

    /** 绿植名称 */
    private String plantName;

    /** 养护人 */
    private String caretaker;

    /** 下次浇水时间 */
    private LocalDateTime nextWaterTime;

    /** 备注 */
    private String remark;
}
