package com.example.devicemanagement.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 盘点提交结果：批次汇总 + 差异清单（缺失、位置不符、待维修）。
 * 差异以提交时冻结的台账归属为基准，刷新后保持一致。
 */
@Data
public class InventorySubmitResultVO {

    private InventoryBatchVO batch;

    /** 差异明细（缺失 / 位置不符 / 待维修） */
    private List<InventoryItemVO> diffItems;

    /** 位置不符条数 */
    private Integer mismatchCount;
    /** 缺失条数 */
    private Integer missingCount;
    /** 待维修条数 */
    private Integer repairCount;
    /** 在场条数 */
    private Integer presentCount;
}
