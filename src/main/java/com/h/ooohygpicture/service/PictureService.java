package com.h.ooohygpicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.h.ooohygpicture.model.dto.picture.*;
import com.h.ooohygpicture.model.entity.Picture;
import com.h.ooohygpicture.model.entity.User;
import com.h.ooohygpicture.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;

/**
 * 图片服务接口
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource          文件输入源 (MultipartFile)
     * @param pictureUploadRequest 图片上传请求参数
     //* @param loginUser            登录用户
     * @return 图片封装类
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);


    /**
     * 获取图片查询条件
     *
     * @param pictureQueryRequest 查询请求参数
     * @return QueryWrapper
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片封装类 (单条)
     *
     * @param picture 图片实体
     * @param request HttpServletRequest
     * @return PictureVO
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装类 (列表)
     *
     * @param picturePage 分页数据
     * @param request     HttpServletRequest
     * @return Page<PictureVO>
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 删除
     *
     */
    void deletePicture(long pictureId, User loginUser);

    void deletePictureByBatch(PictureEditByBatchRequest request, User loginUser);

    /**
     * 批量编辑图片
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    long createPictureOutPaintingTaskAsync(CreatePictureOutPaintingTaskRequest request, User loginUser);
    /**
     * 编辑图片
     *
     * @param pictureEditRequest
     * @param loginUser
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);



    /**
     * 校验图片参数防止传入脏数据
     *
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void doPictureReviewByBatch(PictureReviewByBatchRequest request, User loginUser);

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);



    PictureVO getPictureVOById(long id, HttpServletRequest request);

    /**
     * 创建 AI 扩图任务
     */
    PictureVO createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);
}
