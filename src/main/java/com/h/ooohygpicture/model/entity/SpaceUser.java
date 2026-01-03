package com.h.ooohygpicture.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "space_user")
@Data
public class SpaceUser implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long spaceId;

    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private Date createTime;

    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
