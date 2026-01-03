package com.h.ooohygpicture.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.h.ooohygpicture.annotation.AuthCheck;
import com.h.ooohygpicture.common.BaseResponse;
import com.h.ooohygpicture.common.ResultUtils;
import com.h.ooohygpicture.constant.UserConstant;
import com.h.ooohygpicture.exception.BusinessException;
import com.h.ooohygpicture.exception.ErrorCode;
import com.h.ooohygpicture.exception.ThrowUtils;
import com.h.ooohygpicture.model.dto.picture.*;
import com.h.ooohygpicture.model.entity.Picture;
import com.h.ooohygpicture.model.entity.User;
import com.h.ooohygpicture.model.enums.PictureReviewStatusEnum;
import com.h.ooohygpicture.model.vo.PictureVO;
import com.h.ooohygpicture.service.PictureService;
import com.h.ooohygpicture.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    /**
     * 上传图片 (核心功能)
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {  // 👈 1. 加上这个参数

        // 👈 2. 获取登录用户 (如果未登录，这里会抛出异常，不用你操心)
        User loginUser = userService.getLoginUser(request);

        // 👈 3. 把 loginUser 传进去
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);

        return ResultUtils.success(pictureVO);
    }

    // URL 上传接口也要改
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) { // 👈 1. 加上参数

        User loginUser = userService.getLoginUser(request); // 👈 2. 获取用户
        String fileUrl = pictureUploadRequest.getFileUrl();

        // 👈 3. 传进去
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);

        return ResultUtils.success(pictureVO);
    }

    /**
     * 审核图片 (仅管理员)
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE) // 必须是管理员
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/review/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE) // 必须是管理员
    public BaseResponse<Boolean> doPictureReviewByBatch(@RequestBody PictureReviewByBatchRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getPictureIdList() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        pictureService.doPictureReviewByBatch(request, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 删除图片 (仅管理员)
     */
    /**
     * 删除图片
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deletePicture(@RequestBody com.h.ooohygpicture.common.DeleteRequest deleteRequest,
                                               HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 调用 Service 的完整删除方法
        pictureService.deletePicture(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/delete/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE) // 暂时限制管理员
    public BaseResponse<Boolean> deletePictureByBatch(@RequestBody PictureEditByBatchRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getPictureIdList() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        pictureService.deletePictureByBatch(request, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量编辑 (仅管理员)
     */
    @PostMapping("/edit/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE) // 暂时限制系统管理员，后续可放开给空间管理员
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest,
                                                    HttpServletRequest request) {
        if (pictureEditByBatchRequest == null || pictureEditByBatchRequest.getPictureIdList() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }


    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task_async")
    public BaseResponse<Long> createPictureOutPaintingTaskAsync( // 👈 返回类型改成 Long (Task ID)
                                                                 @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
                                                                 HttpServletRequest request) {
        // ... (校验逻辑不变) ...
        User loginUser = userService.getLoginUser(request);

        // 调用 Service，让它返回 taskId
        // (你需要去 Service 把 void 改成 long 并返回 task.getId())
        long taskId = pictureService.createPictureOutPaintingTaskAsync(createPictureOutPaintingTaskRequest, loginUser);

        return ResultUtils.success(taskId); // 👈 把小票给前端
    }



    /**
     * 根据 ID 获取图片 (封装类)
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 查询数据库
        PictureVO pictureVO = pictureService.getPictureVOById(id, request);
        ThrowUtils.throwIf(pictureVO == null, ErrorCode.NOT_FOUND_ERROR);
        // ------------------ 新增权限校验 ------------------
        // 获取当前用户
        User loginUser = userService.getLoginUserPermitNull(request);

        // 校验：如果是待审核或拒绝，且用户不是管理员，也不是作者本人，则抛异常
        boolean isReviewPass = PictureReviewStatusEnum.PASS.getValue()==(pictureVO.getReviewStatus());
        boolean isAdmin = loginUser != null && userService.isAdmin(loginUser);
        boolean isAuthor = loginUser != null && pictureVO.getUserId().equals(loginUser.getId());

        if (!isReviewPass && !isAdmin && !isAuthor) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该图片");
        }
        // 获取封装类
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页获取图片列表 (封装类)
     * 支持搜索、排序、筛选
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 1. 获取当前登录用户 (可能是 null，即游客)
        User loginUser = userService.getLoginUserPermitNull(request);

        // 2. 只有管理员才能看所有状态，其他人只能看“已通过”
        if (loginUser == null || !userService.isAdmin(loginUser)) {
            // 强制覆盖前端传来的参数
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            // 甚至可以隐藏 reviewMessage 等敏感字段
        }

        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));

        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }
}
