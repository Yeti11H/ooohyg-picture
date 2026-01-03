package com.h.ooohygpicture.model.dto.picture;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PictureReviewByBatchRequest implements Serializable {

    /**
     * 图片 ID 列表
     */
    private List<Long> pictureIdList;

    /**
     * 审核状态：1-通过，2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 空间 ID (用于鉴权)
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
