package com.h.ooohygpicture.service;


import com.h.ooohygpicture.model.dto.space.SpaceAnalyzeRequest;
import com.h.ooohygpicture.model.entity.User;
import com.h.ooohygpicture.model.vo.analyze.SpaceCategoryAnalyzeResponse;
import com.h.ooohygpicture.model.vo.analyze.SpaceUsageAnalyzeResponse;

import java.util.List;

public interface SpaceAnalyzeService {

    /**
     * 空间使用量分析
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceAnalyzeRequest request, User loginUser);

    /**
     * 空间图片分类分析
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceAnalyzeRequest request, User loginUser);

    /**
     * 空间图片标签分析
     */
    //List<com.h.ooohygpicture.model.vo.analyze.SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceAnalyzeRequest request, User loginUser);

    /**
     * 空间图片大小分析
     */
    //List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceAnalyzeRequest request, User loginUser);

    /**
     * 空间用户上传行为分析
     */
   // List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceAnalyzeRequest request, User loginUser);

    /**
     * 空间图片排行分析 (每日上传趋势)
     */
    //List<SpaceSizeAnalyzeResponse> getSpaceRankAnalyze(SpaceAnalyzeRequest request, User loginUser);
}
