package com.h.ooohygpicture.model.dto.picture;

import lombok.Data;
import java.io.Serializable;

@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {

    /**
     * 图片 ID (基于哪张图进行扩充)
     */
    private Long pictureId;

    /**
     * 扩图参数
     */
    private CreatePictureOutPaintingTaskParam parameters;

    private static final long serialVersionUID = 1L;

    @Data
    public static class CreatePictureOutPaintingTaskParam implements Serializable {
        /**
         * 扩图比例，例如 "16:9", "4:3", "1:1"
         */
        private String ratio = "1:1";
        /**
         * 🚀 新增：风格索引
         * 0-复古, 1-3D, 2-二次元, 3-插画, 4-水彩
         * 默认给个 2 (二次元)
         */
        private Integer style = 2;
    }
}
