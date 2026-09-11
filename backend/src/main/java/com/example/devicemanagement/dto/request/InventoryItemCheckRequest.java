package com.example.devicemanagement.dto.request;

import lombok.Data;

@Data
public class InventoryItemCheckRequest {

    /** 盘点结果 1在场 2缺失 3位置不符 4待维修 */
    private Integer checkResult;

    private String remark;
}
