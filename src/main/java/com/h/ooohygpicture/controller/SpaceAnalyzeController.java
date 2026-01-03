package com.h.ooohygpicture.controller;


import com.h.ooohygpicture.common.BaseResponse;
import com.h.ooohygpicture.common.ResultUtils;
import com.h.ooohygpicture.exception.BusinessException;
import com.h.ooohygpicture.exception.ErrorCode;
import com.h.ooohygpicture.model.dto.space.SpaceAnalyzeRequest;
import com.h.ooohygpicture.model.entity.User;
import com.h.ooohygpicture.model.vo.analyze.SpaceCategoryAnalyzeResponse;
import com.h.ooohygpicture.model.vo.analyze.SpaceUsageAnalyzeResponse;
import com.h.ooohygpicture.service.SpaceAnalyzeService;
import com.h.ooohygpicture.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;
    @Resource
    private UserService userService;

    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(
            @RequestBody SpaceAnalyzeRequest request, HttpServletRequest httpRequest) {
        // 如果传了 id，又传了 queryPublic，这是逻辑冲突
        if (request.getSpaceId() != null && request.isQueryPublic()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数冲突");
        }

        User loginUser = userService.getLoginUser(httpRequest);
        return ResultUtils.success(spaceAnalyzeService.getSpaceUsageAnalyze(request, loginUser));
    }

    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(
            @RequestBody SpaceAnalyzeRequest request, HttpServletRequest httpRequest) {
        // 如果传了 id，又传了 queryPublic，这是逻辑冲突
        if (request.getSpaceId() != null && request.isQueryPublic()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数冲突");
        }

        User loginUser = userService.getLoginUser(httpRequest);
        return ResultUtils.success(spaceAnalyzeService.getSpaceCategoryAnalyze(request, loginUser));
    }
}
