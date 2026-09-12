package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_combo_item")
public class DeviceComboItem {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("combo_id")
    private Long comboId;

    @TableField("device_id")
    private Long deviceId;

    @TableField("device_code")
    private String deviceCode;

    @TableField("device_name")
    private String deviceName;

    @TableField("device_type")
    private String deviceType;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
