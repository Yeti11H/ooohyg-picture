package com.h.ooohygpicture.model.vo.analyze;

import lombok.Data;
import java.io.Serializable;

@Data
public class SpaceTagAnalyzeResponse implements Serializable {
    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 使用次数
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}
