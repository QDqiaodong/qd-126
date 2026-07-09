package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_transfer")
public class DeviceTransfer {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("device_id")
    private Long deviceId;

    @TableField("device_code")
    private String deviceCode;

    @TableField("from_floor_id")
    private Long fromFloorId;

    @TableField("from_room_id")
    private Long fromRoomId;

    @TableField("to_floor_id")
    private Long toFloorId;

    @TableField("to_room_id")
    private Long toRoomId;

    @TableField("transfer_reason")
    private String transferReason;

    @TableField("operator")
    private String operator;

    @TableField("transfer_time")
    private LocalDateTime transferTime;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
