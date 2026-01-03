package com.h.ooohygpicture.model.dto.picture;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PictureEditByBatchRequest implements Serializable {

    /**
     * 图片 ID 列表 (必填)
     */
    private List<Long> pictureIdList;

    /**
     * 空间 ID (必填，用于鉴权)
     */
    private Long spaceId;

    /**
     * 修改的分类
     */
    private String category;

    /**
     * 修改的标签 (JSON数组字符串，省得转换了)
     */
    private List<String> tags;

    /**
     * 命名规则 (可选，比如 "图片{i}")
     */
    private String nameRule;

    private static final long serialVersionUID = 1L;
}
