package com.h.ooohygpicture.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.h.ooohygpicture.exception.BusinessException;
import com.h.ooohygpicture.exception.ErrorCode;
import com.h.ooohygpicture.mapper.SpaceMapper;
import com.h.ooohygpicture.mapper.SpaceUserMapper;
import com.h.ooohygpicture.model.dto.space.SpaceAddRequest;
import com.h.ooohygpicture.model.dto.space.SpaceUserAddRequest;
import com.h.ooohygpicture.model.entity.Space;
import com.h.ooohygpicture.model.entity.SpaceUser;
import com.h.ooohygpicture.model.entity.User;
import com.h.ooohygpicture.model.enums.SpaceRoleEnum;
import com.h.ooohygpicture.model.enums.SpaceTypeEnum;
import com.h.ooohygpicture.service.SpaceService;
import com.h.ooohygpicture.service.SpaceUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private SpaceUserMapper spaceUserMapper;
    @Resource
    private SpaceUserService spaceUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1. 填充参数默认值
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space); // 拷贝 name, level 等

        // 默认值处理
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(0);
        }
        // 🚀 重点：区分空间类型 (0-私有, 1-团队)
        // 如果前端没传 type，默认为 0 (私有)
        if (space.getType() == null) {
            space.setType(SpaceTypeEnum.PRIVATE.getValue());
        }

        space.setUserId(loginUser.getId());

        // 2. 权限与额度校验 (核心逻辑)
        this.fillSpaceQuota(space); // 抽取一个方法设置额度，代码更干净

        // 🔒 逻辑：每个用户只能创建一个私有空间
        if (SpaceTypeEnum.PRIVATE.getValue() == space.getType()) {
            QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userId", loginUser.getId());
            queryWrapper.eq("spaceType", SpaceTypeEnum.PRIVATE.getValue());
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "每位用户仅能创建一个私有空间");
            }
        }

        // 3. 插入空间表
        boolean result = this.save(space);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建空间失败");
        }

        // 4. 🚀 重点：如果是团队空间，需要把“创建者”作为“管理员”加入到 space_user 表
        // 私有空间其实也可以加，为了统一管理建议都加
        if (SpaceTypeEnum.TEAM.getValue() == space.getType()) {
            SpaceUser spaceUser = new SpaceUser();
            spaceUser.setSpaceId(space.getId());
            spaceUser.setUserId(loginUser.getId());
            spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue()); // 设置为管理员

            int insertResult = spaceUserMapper.insert(spaceUser);
            if (insertResult <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
            }
        }

        return space.getId();
    }

    /**
     * 💡 面试加分点：将额度配置逻辑抽离
     * 以后如果要改额度，或者对接配置中心，改这一个方法就行，符合“单一职责原则”
     */
    private void fillSpaceQuota(Space space) {
        // 根据空间级别，设置不同的大小和数量
        // 比如：0-普通版，1-专业版，2-旗舰版
        if (space.getSpaceLevel() == 0) {
            space.setMaxSize(100L * 1024 * 1024); // 100MB
            space.setMaxCount(100L);
        } else if (space.getSpaceLevel() == 1) {
            space.setMaxSize(1024L * 1024 * 1024); // 1GB
            space.setMaxCount(1000L);
        } else {
            // 默认为普通
            space.setMaxSize(100L * 1024 * 1024);
            space.setMaxCount(100L);
        }
        // 初始化当前用量
        space.setTotalSize(0L);
        space.setTotalCount(0L);
    }


    @Override
    public long addSpaceUser(SpaceUserAddRequest request, User loginUser) {
        // 1. 基础参数校验
        if (request == null || request.getSpaceId() == null || request.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long spaceId = request.getSpaceId();
        Long targetUserId = request.getUserId();

        // 2. 鉴权：我是不是管理员？
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", spaceId);
        queryWrapper.eq("userId", loginUser.getId());
        SpaceUser myRole = spaceUserService.getOne(queryWrapper);

        if (myRole == null || !"admin".equals(myRole.getSpaceRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只有管理员可以邀请成员");
        }

        // 3. 防止重复加入
        QueryWrapper<SpaceUser> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("spaceId", spaceId);
        checkWrapper.eq("userId", targetUserId);
        long count = spaceUserService.count(checkWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该用户已经是成员了");
        }

        // 4. 插入新成员
        SpaceUser spaceUser = new SpaceUser();
        spaceUser.setSpaceId(spaceId);
        spaceUser.setUserId(targetUserId);
        // 默认给观察者权限
        spaceUser.setSpaceRole(StrUtil.isBlank(request.getSpaceRole()) ? "viewer" : request.getSpaceRole());

        boolean result = spaceUserService.save(spaceUser);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加成员失败");
        }

        return spaceUser.getId();
    }
}
