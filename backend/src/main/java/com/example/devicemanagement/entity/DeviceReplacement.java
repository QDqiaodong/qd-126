package com.example.devicemanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_replacement")
public class DeviceReplacement {

    /** 处理结果：待维修 */
    public static final int RESULT_PENDING_REPAIR = 1;
    /** 处理结果：已修复 */
    public static final int RESULT_REPAIRED = 2;
    /** 处理结果：已报废 */
    public static final int RESULT_SCRAPPED = 3;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("replacement_no")
    private String replacementNo;

    @TableField("room_id")
    private Long roomId;

    @TableField("floor_id")
    private Long floorId;

    @TableField("faulty_device_id")
    private Long faultyDeviceId;

    @TableField("faulty_device_code")
    private String faultyDeviceCode;

    @TableField("faulty_device_name")
    private String faultyDeviceName;

    @TableField("faulty_device_type")
    private String faultyDeviceType;

    @TableField("spare_device_id")
    private Long spareDeviceId;

    @TableField("spare_device_code")
    private String spareDeviceCode;

    @TableField("spare_device_name")
    private String spareDeviceName;

    @TableField("spare_device_type")
    private String spareDeviceType;

    @TableField("fault_phenomenon")
    private String faultPhenomenon;

    @TableField("operator")
    private String operator;

    @TableField("replacement_time")
    private LocalDateTime replacementTime;

    @TableField("process_result")
    private Integer processResult;

    @TableField("process_remark")
    private String processRemark;

    @TableField("processed_by")
    private String processedBy;

    @TableField("processed_at")
    private LocalDateTime processedAt;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
