package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("device")
public class Device {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("device_code")
    private String deviceCode;

    @TableField("device_name")
    private String deviceName;

    @TableField("device_type")
    private String deviceType;

    @TableField("brand")
    private String brand;

    @TableField("model")
    private String model;

    @TableField("spec_json")
    private String specJson;

    @TableField("image_url")
    private String imageUrl;

    @TableField("current_floor_id")
    private Long currentFloorId;

    @TableField("current_room_id")
    private Long currentRoomId;

    @TableField("status")
    private Integer status;

    @TableField("purchase_date")
    private LocalDate purchaseDate;

    @TableField("warranty_end_date")
    private LocalDate warrantyEndDate;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
