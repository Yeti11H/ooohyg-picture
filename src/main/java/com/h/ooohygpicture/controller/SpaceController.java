package com.h.ooohygpicture.controller;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.h.ooohygpicture.common.BaseResponse;
import com.h.ooohygpicture.common.ResultUtils;
import com.h.ooohygpicture.exception.BusinessException;
import com.h.ooohygpicture.exception.ErrorCode;
import com.h.ooohygpicture.exception.ThrowUtils;
import com.h.ooohygpicture.mapper.SpaceUserMapper;
import com.h.ooohygpicture.model.dto.space.SpaceAddRequest;
import com.h.ooohygpicture.model.dto.space.SpaceUserAddRequest;
import com.h.ooohygpicture.model.entity.Space;
import com.h.ooohygpicture.model.entity.SpaceUser;
import com.h.ooohygpicture.model.entity.User;
import com.h.ooohygpicture.model.vo.SpaceVO;
import com.h.ooohygpicture.service.SpaceService;
import com.h.ooohygpicture.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;
    @Resource
    private SpaceUserMapper spaceUserMapper;

    /**
     * 创建空间
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        if (spaceAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long newId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(newId);
    }

    @PostMapping("/list/my")
    public BaseResponse<List<SpaceVO>> listMySpaces(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        // 1. 先查 space_user 表，找所有我加入的 spaceId
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserMapper.selectList(queryWrapper);

        // 如果没加入任何空间，直接返回空
        if (CollUtil.isEmpty(spaceUserList)) {
            return ResultUtils.success(new ArrayList<>());
        }

        // 2. 提取 spaceId 列表
        Set<Long> spaceIds = spaceUserList.stream()
                .map(SpaceUser::getSpaceId)
                .collect(Collectors.toSet());

        // 3. 根据 spaceId 查 space 表
        List<Space> spaceList = spaceService.listByIds(spaceIds);

        // 4. 转 VO (为了安全，脱敏一些字段)
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo) // 假设你有 SpaceVO，没有的话先用 Space
                .collect(Collectors.toList());

        return ResultUtils.success(spaceVOList);
    }


    /**
     * 邀请成员进空间
     */
    @PostMapping("/add/user")
    public BaseResponse<Long> addUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequest==null,ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request==null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long newId = spaceService.addSpaceUser(spaceUserAddRequest, loginUser);
        return ResultUtils.success(newId);
    }


}
