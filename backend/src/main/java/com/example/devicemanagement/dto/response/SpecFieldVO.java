package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class SpecFieldVO {

    private Long id;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private Boolean required;
    private List<String> options;
    private Integer sortOrder;
}
