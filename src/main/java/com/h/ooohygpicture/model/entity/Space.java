package com.h.ooohygpicture.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "space")
@Data
public class Space implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间最大容量
     */
    private Long maxSize;

    /**
     * 空间最大数量
     */
    private Long maxCount;

    /**
     * 当前已用容量
     */
    private Long totalSize;

    /**
     * 当前已用数量
     */
    private Long totalCount;

    private Long userId;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    private Integer type;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
