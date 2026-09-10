package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SpecTemplateVO {

    private Long id;
    private String deviceType;
    private Integer status;
    private String statusText;
    private List<SpecFieldVO> fields;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
