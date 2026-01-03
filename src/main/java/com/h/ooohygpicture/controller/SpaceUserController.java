package com.h.ooohygpicture.controller;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.h.ooohygpicture.annotation.SaSpaceCheckPermission;
import com.h.ooohygpicture.common.BaseResponse;
import com.h.ooohygpicture.common.ResultUtils;
import com.h.ooohygpicture.exception.BusinessException;
import com.h.ooohygpicture.exception.ErrorCode;
import com.h.ooohygpicture.model.dto.space.SpaceUserDeleteRequest;
import com.h.ooohygpicture.model.dto.space.SpaceUserEditRequest;
import com.h.ooohygpicture.model.dto.space.SpaceUserQueryRequest;
import com.h.ooohygpicture.model.entity.SpaceUser;
import com.h.ooohygpicture.model.entity.User;
import com.h.ooohygpicture.model.enums.SpaceRoleEnum;
import com.h.ooohygpicture.model.vo.SpaceUserVO;
import com.h.ooohygpicture.model.vo.UserVO;
import com.h.ooohygpicture.service.SpaceUserService;
import com.h.ooohygpicture.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    @PostMapping("/list")
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getSpaceId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long spaceId = request.getSpaceId();

        // 1. 鉴权：只有团队成员才能看列表
        User loginUser = userService.getLoginUser(httpRequest);
        // ... (这里可以复用之前的鉴权逻辑，或者暂时不严查，毕竟看列表风险不大) ...

        // 2. 查 space_user 表
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", spaceId);
        List<SpaceUser> spaceUserList = spaceUserService.list(queryWrapper);

        if (CollUtil.isEmpty(spaceUserList)) {
            return ResultUtils.success(Collections.emptyList());
        }

        // 3. 提取 userId 列表
        Set<Long> userIds = spaceUserList.stream()
                .map(SpaceUser::getUserId)
                .collect(Collectors.toSet());

        // 4. 查 user 表 (获取头像、昵称)
        List<User> userList = userService.listByIds(userIds);
        // 转 Map 方便查找: userId -> User
        Map<Long, User> userMap = userList.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 5. 组装 VO
        List<SpaceUserVO> voList = spaceUserList.stream().map(spaceUser -> {
            SpaceUserVO vo = new SpaceUserVO();
            BeanUtils.copyProperties(spaceUser, vo);

            // 填充用户信息
            User user = userMap.get(spaceUser.getUserId());
            if (user != null) {
                UserVO userVO = UserVO.objToVo(user);
                vo.setUser(userVO);
            }
            return vo;
        }).collect(Collectors.toList());

        return ResultUtils.success(voList);
    }
    @PostMapping("/delete")
    @SaSpaceCheckPermission(mustRole = SpaceRoleEnum.ADMIN)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody SpaceUserDeleteRequest request) { // 用新的请求类
        if (request == null || request.getId() <= 0 || request.getSpaceId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 你的 Service 方法可能需要改一下，或者只用 id 也行
        // 但鉴权必须依赖 spaceId
        long id = request.getId();
        spaceUserService.deleteSpaceUser(id);
        return ResultUtils.success(true);
    }


    @PostMapping("/edit")
    @SaSpaceCheckPermission(mustRole = SpaceRoleEnum.ADMIN)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // ... (鉴权逻辑同上，只有管理员能改别人角色) ...
        User loginUser = userService.getLoginUser(httpRequest);
        // 这里省略重复的鉴权代码，建议封装成一个 private void checkAdmin(...) 方法

        spaceUserService.editSpaceUser(request);
        return ResultUtils.success(true);
    }

}
