package com.example.devicemanagement.dto.request;

import lombok.Data;

/**
 * 归还核销请求：登记实际归还操作人（可选，默认取当前管理员）。
 */
@Data
public class RainGearReturnRequest {

    /** 归还核销登记人 */
    private String returnedBy;
}
