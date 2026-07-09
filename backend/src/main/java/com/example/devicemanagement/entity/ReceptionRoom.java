package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reception_room")
public class ReceptionRoom {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("room_name")
    private String roomName;

    @TableField("room_code")
    private String roomCode;

    @TableField("floor_id")
    private Long floorId;

    @TableField("capacity")
    private Integer capacity;

    @TableField("equipment_count")
    private Integer equipmentCount;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
