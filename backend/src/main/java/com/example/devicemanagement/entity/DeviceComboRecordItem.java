package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_combo_record_item")
public class DeviceComboRecordItem {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("record_id")
    private Long recordId;

    @TableField("device_id")
    private Long deviceId;

    @TableField("device_code")
    private String deviceCode;

    @TableField("device_name")
    private String deviceName;

    @TableField("device_type")
    private String deviceType;

    /** 套用结果 1调入 2已在房间 3跳过 */
    @TableField("result")
    private Integer result;

    @TableField("skip_reason")
    private String skipReason;

    @TableField("from_room_id")
    private Long fromRoomId;

    @TableField("from_room_name")
    private String fromRoomName;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
