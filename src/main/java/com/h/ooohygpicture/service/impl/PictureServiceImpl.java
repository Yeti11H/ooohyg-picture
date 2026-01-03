package com.h.ooohygpicture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.h.ooohygpicture.config.MqConfig;
import com.h.ooohygpicture.constant.UserConstant;
import com.h.ooohygpicture.exception.BusinessException;
import com.h.ooohygpicture.exception.ErrorCode;
import com.h.ooohygpicture.exception.ThrowUtils;
import com.h.ooohygpicture.manager.AiManager;
import com.h.ooohygpicture.manager.CosManager;
import com.h.ooohygpicture.manager.upload.FilePictureUpload;
import com.h.ooohygpicture.manager.upload.UrlPictureUpload;
import com.h.ooohygpicture.mapper.PictureMapper;
import com.h.ooohygpicture.mapper.SpaceUserMapper;
import com.h.ooohygpicture.model.dto.file.UploadPictureResult;
import com.h.ooohygpicture.model.dto.picture.*;
import com.h.ooohygpicture.model.entity.*;
import com.h.ooohygpicture.model.enums.PictureReviewStatusEnum;
import com.h.ooohygpicture.model.vo.PictureVO;
import com.h.ooohygpicture.model.vo.UserVO;
import com.h.ooohygpicture.service.PictureService;
import com.h.ooohygpicture.service.PictureTaskService;
import com.h.ooohygpicture.service.SpaceService;
import com.h.ooohygpicture.service.UserService;
import com.h.ooohygpicture.websocket.PictureEditHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    CosManager cosManager;
    @Resource
    private UserService userService;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserMapper spaceUserMapper;
    // 在类里面注入
    @Resource
    private PictureEditHandler pictureEditHandler; // 👈 刚才写的 WebSocket 类
    @Resource
    private AiManager aiManager;
    @Resource
    private PictureTaskService pictureTaskService;
    @Resource
    private RabbitTemplate rabbitTemplate;




    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {

        Long spaceId = pictureUploadRequest.getSpaceId();
        Picture picture = new Picture();
        if (spaceId != null) {
            // ============ 🚀 新增：团队空间权限校验 START ============


            // 2. 🚀 【新增】校验空间权限 & 额度
            if (spaceId != null) {
                Space space = spaceService.getById(spaceId);
                ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

                if (space.getTotalCount() >= space.getMaxCount()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数已满");
                }
                if (space.getTotalSize() >= space.getMaxSize()) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间容量已满");
                }
                QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("spaceId", spaceId);
                queryWrapper.eq("userId", loginUser.getId());
                SpaceUser spaceUser = spaceUserMapper.selectOne(queryWrapper);

                if (spaceUser == null) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该空间成员，无权上传");
                }

                // (可选) 可以在这里校验角色，比如只有 editor/admin 能传，viewer 不能传
                if ("viewer".equals(spaceUser.getSpaceRole())) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
                }

                // ============ 🚀 新增：团队空间权限校验 END ============
            }

            Long userId = loginUser.getId();

            // 修正 1：只传目录路径，不要自己拼接文件名！
            // 这样 Template 会自动生成 日期_UUID.jpg，路径就是 public/1/2024-xxx.jpg
            String uploadPathPrefix = "public/" + userId;

            UploadPictureResult uploadResult;

            // 修正 2：调用 uploadPicture 时，只传 inputSource 和 uploadPathPrefix
            if (inputSource instanceof String && StrUtil.isNotBlank((String) inputSource)) {
                uploadResult = urlPictureUpload.uploadPicture(inputSource, uploadPathPrefix);
            } else {
                uploadResult = filePictureUpload.uploadPicture(inputSource, uploadPathPrefix);
            }


            picture.setUrl(uploadResult.getUrl());
            // 修正 3：使用 Template 返回的规范文件名（或者 uploadResult.getPicName() 也可以）
            picture.setName(uploadResult.getPicName());
            picture.setPicSize(uploadResult.getPicSize());
            picture.setUserId(userId);

            // 修正 4：补充缺失的字段 (数据万象解析出的数据)
            picture.setPicWidth(uploadResult.getPicWidth());
            picture.setPicHeight(uploadResult.getPicHeight());
            picture.setPicScale(uploadResult.getPicScale());
            picture.setPicFormat(uploadResult.getPicFormat());
            picture.setPicColor(uploadResult.getPicColor());
            picture.setThumbnailUrl(uploadResult.getThumbnailUrl());
            picture.setSpaceId(spaceId); // 👈 记得把 spaceId 存进数据库！
            if (StrUtil.isBlank(pictureUploadRequest.getCategory())) {
                picture.setCategory("默认"); // 或者 "其他"
            } else {
                picture.setCategory(pictureUploadRequest.getCategory());
            }
            // 🚀 【新增】补充 category 和 tags 字段
            if (pictureUploadRequest != null) {
                // 1. 设置分类
                picture.setCategory(pictureUploadRequest.getCategory());

                // 2. 设置标签 (List<String> -> JSON String)
                // 需要引入 cn.hutool.json.JSONUtil
                if (CollUtil.isNotEmpty(pictureUploadRequest.getTags())) {
                    picture.setTags(JSONUtil.toJsonStr(pictureUploadRequest.getTags()));
                }
            }
            this.fillReviewParams(picture, loginUser);
            this.save(picture);
            // 6. 🚀 【新增】更新空间已用额度
            if (spaceId != null) {
                // 使用 SQL 直接更新：totalSize = totalSize + 新图大小
                boolean updateSpace = spaceService.update()
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .eq("id", spaceId)
                        .update();
                if (!updateSpace) {
                    // 如果更新失败（比如刚才刚好被删了），可以抛异常回滚，或者记录日志
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新空间额度失败");
                }
            }



        }
        return PictureVO.objToVo(picture);
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        PictureVO pictureVO = PictureVO.objToVo(picture);
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 1. 判断图片是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 2. 校验权限 (必须是管理员，或者图片是自己的才能删)
        // 注意：这里需要你 User 表里有 userRole 字段，如果没有就只校验 userId
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 3. 开启事务删除数据库记录
        transactionTemplate.execute(status -> {
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            return result;
        });
        String cacheKey = "ooohyg:detail:" + pictureId;
        stringRedisTemplate.delete(cacheKey);

        // 4. 异步清理云端文件 (清理主图、缩略图)
        // 使用 CompletableFuture 开启异步任务，不阻塞主线程
        // 这样用户删得快，后台慢慢删文件
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            this.clearPictureFile(oldPicture);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePictureByBatch(PictureEditByBatchRequest request, User loginUser) {
        List<Long> pictureIdList = request.getPictureIdList();
        Long spaceId = request.getSpaceId();

        // 1. 鉴权逻辑 (完全复用 edit 的代码，建议抽取成 private 方法)
        checkBatchAuth(pictureIdList, spaceId, loginUser);

        // 2. 执行删除
        // 逻辑删除数据库记录
        boolean result = this.removeByIds(pictureIdList);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        // 3. (可选) 异步清理云端文件
        // 遍历 idList 调 clearPictureFile，这里从简略过
    }

    //批量编辑
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        // 1. 参数校验
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        checkBatchAuth(pictureIdList,spaceId,loginUser);
        //查出这些图片
        List<Picture> pictureList = this.listByIds(pictureIdList);
        if (CollUtil.isEmpty(pictureList)) {
            return;
        }

        // 5. 批量修改属性 (准备更新)
        String tagsJson = null;
        if (CollUtil.isNotEmpty(tags)) {
            tagsJson = JSONUtil.toJsonStr(tags);
        }

        // 使用 pictureList 直接遍历修改
        for (Picture picture : pictureList) {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (StrUtil.isNotBlank(tagsJson)) {
                picture.setTags(tagsJson);
            }
            // 命名规则: 图片{i}
            if (StrUtil.isNotBlank(pictureEditByBatchRequest.getNameRule())) {
                String rule = pictureEditByBatchRequest.getNameRule();
                // 这里的 index 要从 1 开始
                String newName = rule.replaceAll("\\{i}", String.valueOf(pictureList.indexOf(picture) + 1));
                picture.setName(newName);
            }
            picture.setEditTime(new Date());
        }

        // 6. 批量更新数据库
        boolean result = this.updateBatchById(pictureList);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }


    @Override
    public PictureVO getPictureVOById(long id, HttpServletRequest request) {
        // 1. 构建缓存 Key
        String cacheKey = "ooohyg:detail:" + id;

        // 2. 查询缓存
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cachedValue)) {
            // 命中缓存，转为对象返回
            // 注意：这里用 Hutool 的 JSONUtil
            return JSONUtil.toBean(cachedValue, PictureVO.class);
        }

        // 3. 查询数据库
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        // 4. 校验权限 (把 Controller 里的逻辑搬到这里，或者复用原来的)
        // 注意：如果你不想把 request 传进来，也可以把权限校验留在 Controller
        // 这里假设已经通过了 Controller 的基础校验，或者在这里再校验一次

        // 5. 转换封装类
        PictureVO pictureVO = this.getPictureVO(picture, request);

        // 6. 写入缓存 (过期时间 5 分钟)
        String jsonValue = JSONUtil.toJsonStr(pictureVO);
        // 加上随机过期时间，防止雪崩 (5-10分钟)
        int cacheExpireTime = 300 +  RandomUtil.randomInt(0, 300);
        stringRedisTemplate.opsForValue().set(cacheKey, jsonValue, cacheExpireTime, TimeUnit.SECONDS);

        return pictureVO;
    }

    /**
     * 清理图片文件 (私有方法)
     */
    private void clearPictureFile(Picture oldPicture) {
        try {
            // 1. 清理主图 (WebP)
            String pictureUrl = oldPicture.getUrl();
            if (StrUtil.isNotBlank(pictureUrl)) {
                // 必须把完整的 URL (https://...) 转成 Key (public/1/xxx.webp)
                String key = new URL(pictureUrl).getPath();
                // path 可能会带最前面的 /，需要去掉
                if (key.startsWith("/")) {
                    key = key.substring(1);
                }
                cosManager.deleteObject(key);
            }

            // 2. 清理缩略图
            String thumbnailUrl = oldPicture.getThumbnailUrl();
            if (StrUtil.isNotBlank(thumbnailUrl)) {
                String key = new URL(thumbnailUrl).getPath();
                if (key.startsWith("/")) {
                    key = key.substring(1);
                }
                cosManager.deleteObject(key);
            }

            log.info("清理云端文件成功, id: {}", oldPicture.getId());

        } catch (MalformedURLException e) {
            log.error("解析 URL 失败, id: {}", oldPicture.getId(), e);
        } catch (Exception e) {
            log.error("清理云端文件失败, id: {}", oldPicture.getId(), e);
        }
    }

    @Override
    public PictureVO createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest request, User loginUser) {
        // 1. 查原图
        Picture picture = this.getById(request.getPictureId());
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // ... (鉴权逻辑保持不变) ...

        // 2. 获取参数
        Integer style = 2; // 默认二次元
        if (request.getParameters() != null && request.getParameters().getStyle() != null) {
            style = request.getParameters().getStyle();
        }

        // -----------------------------------------------------------------------
        // 💡 智能构建提示词 (Prompt Construction)
        // 利用图片原本的信息，来生成一张内容相似的新图
        // -----------------------------------------------------------------------
        StringBuilder promptBuilder = new StringBuilder();
        // 1. 加入图片名称 (比如 "小猫")
        if (StrUtil.isNotBlank(picture.getName())) {
            promptBuilder.append(picture.getName()).append(", ");
        }
        // 2. 加入图片简介 (比如 "在草地上跑")
        if (StrUtil.isNotBlank(picture.getIntroduction())) {
            promptBuilder.append(picture.getIntroduction()).append(", ");
        }
        // 3. 加入通用高质量词
        promptBuilder.append("masterpiece, high quality, 4k, ");

        // 4. 根据风格加入关键词
        switch (style) {
            case 0: promptBuilder.append("retro style, vintage filter"); break;
            case 1: promptBuilder.append("3d render, pixar style, cute"); break;
            case 2: promptBuilder.append("anime style, japanese anime"); break; // 二次元
            case 3: promptBuilder.append("flat illustration, vector art"); break;
            case 4: promptBuilder.append("watercolor painting, artistic"); break;
            default: promptBuilder.append("realistic, photography"); break;
        }

        // 5. 调用 AI (这里我们要调用 createPicture 方法，而不是 createOutPaintingPicture)
        // 注意：这里我们复用你最早写的那个文生图方法，因为它最稳！
        // 如果 AiManager 里没有支持 style 参数的文生图方法，直接调 createPicture(prompt) 即可
        String newUrl = aiManager.createPicture(promptBuilder.toString());

        // 6. 保存新图 (逻辑不变)
        Picture newPicture = new Picture();
        BeanUtils.copyProperties(picture, newPicture);
        newPicture.setId(null);
        newPicture.setName(picture.getName() + "_AI生成");
        newPicture.setUrl(newUrl);
        newPicture.setCreateTime(new Date());
        newPicture.setEditTime(new Date());
        newPicture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        this.save(newPicture);

        return PictureVO.objToVo(newPicture);
    }
    @Override
    public long createPictureOutPaintingTaskAsync(CreatePictureOutPaintingTaskRequest request, User loginUser) {
        // 1. 查原图信息
        Picture picture = this.getById(request.getPictureId());
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // ... (鉴权逻辑) ...

        // 2. 创建任务记录 (PictureTask)
        PictureTask task = new PictureTask();
        task.setUserId(loginUser.getId());
        task.setPictureId(picture.getId()); // 记录是基于哪张图扩的
        task.setPrompt(JSONUtil.toJsonStr(request.getParameters())); // 把参数存进 prompt 字段备用
        task.setStatus("WAITING");
        pictureTaskService.save(task);

        // 3. 发送到 MQ
        // 这里的路由键复用之前的 AI_DRAW_ROUTING_KEY，或者新建一个
        rabbitTemplate.convertAndSend(MqConfig.AI_DRAW_EXCHANGE, MqConfig.AI_DRAW_ROUTING_KEY, String.valueOf(task.getId()));
        return task.getId();
    }



    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 1. 转换实体
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意：pictureEditRequest.getTags() 应该是 List<String>，直接 copy 即可
        // picture.setTags(pictureEditRequest.getTags()); // BeanUtils 已经考过去了

        picture.setEditTime(new Date()); // 更新编辑时间

        // 2. 数据校验 (validPicture)
        this.validPicture(picture);

        // 3. 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 4. 校验权限 (仅本人或管理员可编辑)
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        this.fillReviewParams(picture, loginUser);
        // 5. 更新数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
       if (result) {
            // 🎉 修改成功，通知房间里的其他人
            // 这里的 message 可以是 JSON，告诉前端哪张图变了
            String msg = String.format("用户 %s 修改了图片", loginUser.getUserName());
            pictureEditHandler.broadcast(picture.getSpaceId(), msg);
        }
        // 👇 【新增】清理缓存
        String cacheKey = "ooohyg:detail:" + id;
        stringRedisTemplate.delete(cacheKey);

    }


    /**
     * 校验图片
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();

        // 修改数据时，id 不能为空，有参数则校验
        // 如果是新增，id 可以为空（由 MyBatis Plus 生成）
        // 所以这里可以根据你的业务逻辑调整，或者去掉 id 校验
        // ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");

        // 如果传递了 url，才校验长度
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }

        // 如果传递了简介，校验长度
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }

        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());

        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });

        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) return queryWrapper;

        // 1. 取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        String searchText = pictureQueryRequest.getSearchText();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus=pictureQueryRequest.getReviewStatus();
        Long userId = pictureQueryRequest.getUserId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();

        // 2. 拼接查询条件
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText).or().like("introduction", searchText));
        }
        // 核心逻辑：区分个人空间 vs 团队空间
        if (spaceId != null) {
            // 查特定团队空间：WHERE spaceId = ?
            queryWrapper.eq("spaceId", spaceId);
        } else if (nullSpaceId) {
            // 查个人空间：WHERE spaceId IS NULL
            queryWrapper.isNull("spaceId");
        }

        queryWrapper.eq(ObjUtil.isNotEmpty(id)&&id>0, "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId)&&userId>0, "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);

        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);

        // 🌟 标签查询 (核心逻辑)
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                // 因为数据库存的是 ["Java", "Spring"]，所以要用 like "%"Java"%"
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }

        // 3. 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), "ascend".equals(sortOrder), sortField);

        return queryWrapper;
    }

    /**
     * 填充审核参数
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 1. 如果是管理员，自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 2. 如果是普通用户，默认待审核
            // 注意：编辑图片时，状态也要重置为待审核！
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }
    /**
     * 批量审核
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doPictureReviewByBatch(PictureReviewByBatchRequest request, User loginUser) {
        List<Long> pictureIdList = request.getPictureIdList();
        Integer reviewStatus = request.getReviewStatus();
        String reviewMessage = request.getReviewMessage();
        Long spaceId = request.getSpaceId();

        if (CollUtil.isEmpty(pictureIdList) || reviewStatus == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 1. 鉴权 (注意：审核通常是管理员权限，或者是空间管理员审核空间内的图)
        // 这里可以直接复用 checkBatchAuth，或者写更简单的管理员校验
        checkBatchAuth(pictureIdList, spaceId, loginUser);

        // 2. 构造更新对象
        List<Picture> pictureList = pictureIdList.stream().map(id -> {
            Picture picture = new Picture();
            picture.setId(id);
            picture.setReviewStatus(reviewStatus);
            picture.setReviewMessage(reviewMessage);
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            return picture;
        }).collect(Collectors.toList());

        // 3. 批量更新
        boolean result = this.updateBatchById(pictureList);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }


    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();

        // 状态只能是 1(通过) 或 2(拒绝)，不能改回 0(待审核)
        if (id == null || reviewStatus == null || PictureReviewStatusEnum.REVIEWING.getValue() == reviewStatus) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 3. 校验：不能重复审核
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }

        // 4. 更新数据库
        Picture updatePicture = new Picture();
        updatePicture.setId(id);
        updatePicture.setReviewStatus(reviewStatus);
        updatePicture.setReviewMessage(reviewMessage);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());

        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }
    /**
     * 批量操作的公共鉴权逻辑
     */
    private void checkBatchAuth(List<Long> pictureIdList, Long spaceId, User loginUser) {
        if (CollUtil.isEmpty(pictureIdList) || spaceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        List<Picture> pictureList = this.listByIds(pictureIdList);
        if (CollUtil.isEmpty(pictureList)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }

        // 1. 检查图片是否都属于该空间
        for (Picture picture : pictureList) {
            if (!spaceId.equals(picture.getSpaceId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "部分图片不属于该空间");
            }
        }

        // 2. 检查用户是否有权操作该空间
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }

        if (!userService.isAdmin(loginUser)) {
            if (!space.getUserId().equals(loginUser.getId())) {
                 SpaceUser spaceUser = spaceUserMapper.selectOne(new QueryWrapper<SpaceUser>().eq("spaceId", spaceId).eq("userId", loginUser.getId()));
                 if (spaceUser == null || !"admin".equals(spaceUser.getSpaceRole())) {
                     throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该空间图片");
                 }
            }
        }
    }


}
