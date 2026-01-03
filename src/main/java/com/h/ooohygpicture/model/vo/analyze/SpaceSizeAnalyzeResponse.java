package com.h.ooohygpicture.model.vo.analyze;



import lombok.Data;
import java.io.Serializable;

@Data
public class SpaceSizeAnalyzeResponse implements Serializable {
    /**
     * 大小范围 / 时间区间
     */
    private String sizeRange;

    /**
     * 数量
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}
