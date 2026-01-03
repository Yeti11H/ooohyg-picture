package com.h.ooohygpicture.model.dto.space;

import lombok.Data;
import java.io.Serializable;

@Data
public class SpaceAnalyzeRequest implements Serializable {

    /**
     * 空间 ID (必须传)
     */
    private Long spaceId;

    /**
     * 是否查询全部空间 (仅管理员可用)
     */
    private boolean queryAll;

    /**
     * 是否查询公共空间 (即 spaceId 为空的情况)
     */
    private boolean queryPublic;

    private static final long serialVersionUID = 1L;
}
