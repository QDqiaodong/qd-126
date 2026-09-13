package com.example.devicemanagement.dto.request;

import lombok.Data;

/**
 * 浇水核销请求：登记实际浇水操作人（可选，默认取当前管理员）。
 */
@Data
public class PlantCareWaterRequest {

    /** 浇水核销登记人 */
    private String wateredBy;
}
