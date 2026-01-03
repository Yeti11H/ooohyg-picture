package com.h.ooohygpicture.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.h.ooohygpicture.model.dto.space.SpaceAddRequest;
import com.h.ooohygpicture.model.dto.space.SpaceUserAddRequest;
import com.h.ooohygpicture.model.entity.Space;
import com.h.ooohygpicture.model.entity.User;

public interface SpaceService extends IService<Space> {
    /**
     * 创建空间
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 添加空间成员
     */
    long addSpaceUser(SpaceUserAddRequest request,User loginUser);


}
