package com.h.ooohygpicture.model.vo.analyze;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceCategoryAnalyzeResponse implements Serializable {
    private String category; // 分类名
    private Long count;      // 数量
    private Long totalSize;  // 该分类占用总大小
}
