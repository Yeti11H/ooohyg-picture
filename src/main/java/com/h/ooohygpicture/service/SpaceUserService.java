package com.h.ooohygpicture.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.h.ooohygpicture.model.dto.space.SpaceUserEditRequest;
import com.h.ooohygpicture.model.entity.SpaceUser;

public interface SpaceUserService extends IService<SpaceUser> {
    /**
     * 从空间移除成员
     */
    void deleteSpaceUser(long spaceUserId);

    /**
     * 修改成员权限
     */
    void editSpaceUser(SpaceUserEditRequest request);

}
