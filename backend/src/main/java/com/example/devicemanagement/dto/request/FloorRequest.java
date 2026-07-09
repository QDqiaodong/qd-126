package com.example.devicemanagement.dto.request;

import lombok.Data;

@Data
public class FloorRequest {

    private String floorName;
    private Integer floorNumber;
    private String buildingName;
}
