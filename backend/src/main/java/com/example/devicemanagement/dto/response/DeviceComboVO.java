package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeviceComboVO {

    private Long id;

    private String comboName;

    private String remark;

    private Integer status;

    private String statusText;

    private String createdBy;

    private Integer deviceCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 组合内设备（按保存顺序）
     */
    private List<ComboMemberVO> devices;

    @Data
    public static class ComboMemberVO {

        private Long deviceId;

        private String deviceCode;

        private String deviceName;

        private String deviceType;

        private String brand;

        private String model;

        private Integer sortOrder;

        // ---- 实时台账信息：刷新后组合设备状态、所在位置与台账保持一致 ----

        private Integer liveStatus;

        private String liveStatusText;

        private Long currentFloorId;

        private String currentFloorName;

        private Long currentRoomId;

        private String currentRoomName;

        /**
         * 设备是否已从台账删除
         */
        private Boolean exists;
    }
}
