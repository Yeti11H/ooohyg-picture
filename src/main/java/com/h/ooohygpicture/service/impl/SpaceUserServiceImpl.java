package com.h.ooohygpicture.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.h.ooohygpicture.exception.BusinessException;
import com.h.ooohygpicture.exception.ErrorCode;
import com.h.ooohygpicture.mapper.SpaceUserMapper;
import com.h.ooohygpicture.model.dto.space.SpaceUserEditRequest;
import com.h.ooohygpicture.model.entity.SpaceUser;
import com.h.ooohygpicture.service.SpaceUserService;
import org.springframework.stereotype.Service;

@Service // 👈 加上这个，告诉 Spring 把我交给它管理
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Override
    public void deleteSpaceUser(long spaceUserId) {
        // 1. 待删除的记录是否存在？
        SpaceUser spaceUser = this.getById(spaceUserId);
        if (spaceUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 2. 鉴权：谁在操作？(这里需要获取当前登录用户，你可以把 loginUser 传进来，或者用 RequestHolder)
        // 为了简单，假设 Controller 已经校验了 "我是管理员"
        // ⚠️ 严谨做法：Service 方法加 User loginUser 参数，内部再查一次库判断权限

        // 3. 执行删除
        boolean result = this.removeById(spaceUserId);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    @Override
    public void editSpaceUser(SpaceUserEditRequest request) {
        // 1. 参数校验
        SpaceUser spaceUser = this.getById(request.getId());
        if (spaceUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 2. 修改角色
        spaceUser.setSpaceRole(request.getSpaceRole());
        boolean result = this.updateById(spaceUser);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

}
