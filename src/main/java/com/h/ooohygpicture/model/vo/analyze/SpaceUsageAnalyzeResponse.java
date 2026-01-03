package com.h.ooohygpicture.model.vo.analyze;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUsageAnalyzeResponse implements Serializable {
    private Long usedSize;   // 已用空间 (Byte)
    private Long maxSize;    // 总空间
    private Double sizeUsageRatio; // 空间使用率 (0.5 = 50%)

    private Long usedCount;  // 已传数量
    private Long maxCount;   // 总数量
    private Double countUsageRatio; // 数量使用率
}
