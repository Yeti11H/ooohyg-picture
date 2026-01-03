package com.h.ooohygpicture.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片 id（用于修改）
     */
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 图片标签 (重点：这里定义为 List，Spring 会自动把逗号分隔的字符串转成 List)
     */
    private List<String> tags;


    private static final long serialVersionUID = 1L;
}