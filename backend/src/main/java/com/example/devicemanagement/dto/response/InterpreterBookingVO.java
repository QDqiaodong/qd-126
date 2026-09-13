package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterpreterBookingVO {

    private Long id;
    private String bookingNo;

    private Long roomId;
    private String roomName;
    private String roomCode;
    private Long floorId;
    private String floorName;

    private String language;
    private String interpreterName;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String statusText;

    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
