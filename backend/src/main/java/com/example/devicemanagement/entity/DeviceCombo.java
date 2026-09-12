package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_combo")
public class DeviceCombo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("combo_name")
    private String comboName;

    @TableField("remark")
    private String remark;

    /** 状态 1启用 0停用 */
    @TableField("status")
    private Integer status;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
