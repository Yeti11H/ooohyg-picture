package com.h.ooohygpicture.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@TableName(value = "credit_log")
@Data
public class CreditLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * 变动金额(正数为加，负数为减)
     */
    private Long amount;

    /**
     * 类型：0-签到 1-画图扣费
     */
    private Integer type;

    private String remark;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
