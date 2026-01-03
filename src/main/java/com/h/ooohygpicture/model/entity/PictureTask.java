package com.h.ooohygpicture.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * AI 绘图任务表
 * @TableName picture_task
 */
@TableName(value ="picture_task")
@Data
public class PictureTask {
    /**
     * 主键 ID
     */
    @TableId
    private Long id;

    /**
     * 创建用户 ID
     */
    private Long userId;

    /**
     * AI 提示词
     */
    private String prompt;

    /**
     * 生成结果 URL (阿里云的临时地址)
     */
    private String outputUrl;

    /**
     * 转存后的图片 ID (对应 picture 表)
     */
    private Long pictureId;

    /**
     * 任务状态: PENDING-生成中, SUCCESS-成功, FAILED-失败
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
}