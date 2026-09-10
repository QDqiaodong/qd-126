package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_spec_template")
public class DeviceSpecTemplate {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("device_type")
    private String deviceType;

    /**
     * 状态 1启用 0停用。停用后历史设备的规格数据仍然保留。
     */
    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
