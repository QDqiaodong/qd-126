package com.example.devicemanagement.dto.request;

import lombok.Data;

@Data
public class InventoryItemResolveRequest {

    /** 处理备注，说明缺失找回等处理情况 */
    private String processRemark;
}
