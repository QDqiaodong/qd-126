package com.example.devicemanagement.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RainGearBorrowRequest {

    /** 接待室ID（管理员按楼层选择接待室） */
    private Long roomId;

    /** 雨具类型 UMBRELLA雨伞 RAINCOAT雨衣 */
    private String gearType;

    /** 借出人 */
    private String borrower;

    /** 预计归还时间 */
    private LocalDateTime expectedReturnTime;

    /** 备注 */
    private String remark;
}
