package com.h.ooohygpicture.model.dto.space;

import lombok.Data;

import java.io.Serializable;

// 在 dto 包里新建一个类
@Data
public class SpaceUserDeleteRequest implements Serializable {
    /**
     * 待删除的成员 ID (spaceUserId)
     */
    private Long id;

    /**
     * 空间 ID (必须传！AOP 要用它来鉴权)
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
