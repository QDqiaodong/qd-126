package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接待室雨具借用台账：管理员按楼层选择接待室，
 * 登记雨伞/雨衣、借出人、预计归还时间；归还时核销（状态置为已归还）。
 * 在借且超过预计归还时间的记录派生为逾期，逾期标记不落库，刷新后与在借件数保持一致。
 */
@Data
@TableName("rain_gear_borrow")
public class RainGearBorrow {

    /** 在借 */
    public static final int STATUS_BORROWED = 0;
    /** 已归还（已核销） */
    public static final int STATUS_RETURNED = 1;

    /** 雨伞 */
    public static final String GEAR_UMBRELLA = "UMBRELLA";
    /** 雨衣 */
    public static final String GEAR_RAINCOAT = "RAINCOAT";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("borrow_no")
    private String borrowNo;

    @TableField("room_id")
    private Long roomId;

    /** 冗余接待室所在楼层，便于按楼层筛选 */
    @TableField("floor_id")
    private Long floorId;

    /** 雨具类型 UMBRELLA雨伞 RAINCOAT雨衣 */
    @TableField("gear_type")
    private String gearType;

    /** 借出人 */
    @TableField("borrower")
    private String borrower;

    @TableField("expected_return_time")
    private LocalDateTime expectedReturnTime;

    @TableField("status")
    private Integer status;

    @TableField("returned_at")
    private LocalDateTime returnedAt;

    @TableField("returned_by")
    private String returnedBy;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
