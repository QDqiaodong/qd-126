package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("floor")
public class Floor {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("floor_name")
    private String floorName;

    @TableField("floor_number")
    private Integer floorNumber;

    @TableField("building_name")
    private String buildingName;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
