package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接待室绿植养护台账：管理员按楼层选择接待室，
 * 登记绿植名称、养护人和下次浇水时间；浇完水后核销（状态置为已浇水）。
 * 待养护且超过下次浇水时间的记录派生为逾期，逾期标记不落库，刷新后与待养件数保持一致。
 */
@Data
@TableName("plant_care")
public class PlantCare {

    /** 待养护（未浇水核销） */
    public static final int STATUS_PENDING = 0;
    /** 已浇水（已核销） */
    public static final int STATUS_WATERED = 1;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("care_no")
    private String careNo;

    @TableField("room_id")
    private Long roomId;

    /** 冗余接待室所在楼层，便于按楼层筛选 */
    @TableField("floor_id")
    private Long floorId;

    /** 绿植名称 */
    @TableField("plant_name")
    private String plantName;

    /** 养护人 */
    @TableField("caretaker")
    private String caretaker;

    /** 下次浇水时间 */
    @TableField("next_water_time")
    private LocalDateTime nextWaterTime;

    @TableField("status")
    private Integer status;

    @TableField("watered_at")
    private LocalDateTime wateredAt;

    @TableField("watered_by")
    private String wateredBy;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
