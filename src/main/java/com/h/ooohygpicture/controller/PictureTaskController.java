package com.h.ooohygpicture.controller;

// 1. 【修改】删除 Hutool 的 Cache，换成 Caffeine 的 Cache

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.h.ooohygpicture.common.BaseResponse;
import com.h.ooohygpicture.common.ResultUtils;
import com.h.ooohygpicture.config.MqConfig;
import com.h.ooohygpicture.exception.BusinessException;
import com.h.ooohygpicture.exception.ErrorCode;
import com.h.ooohygpicture.model.dto.picture.CreatePictureOutPaintingTaskRequest;
import com.h.ooohygpicture.model.entity.PictureTask;
import com.h.ooohygpicture.model.entity.User;
import com.h.ooohygpicture.model.vo.PictureVO;
import com.h.ooohygpicture.service.PictureTaskService;
import com.h.ooohygpicture.service.UserService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture_task")
public class PictureTaskController {

    @Resource
    private PictureTaskService pictureTaskService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private UserService userService;
    @Resource
    private RedissonClient redissonClient;

    // 这里的 Cache<String, String> 现在引用的是 Caffeine 的接口
    @Resource
    private Cache<String, String> pictureTaskCache;


    @PostMapping("/ai/create_async")
    public BaseResponse<Long> createAiPicture(@RequestParam String prompt, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        PictureTask task = new PictureTask();
        task.setUserId(loginUser.getId());
        task.setPrompt(prompt);
        task.setStatus("WAITING");
        pictureTaskService.save(task);

        rabbitTemplate.convertAndSend(MqConfig.AI_DRAW_EXCHANGE, MqConfig.AI_DRAW_ROUTING_KEY, String.valueOf(task.getId()));

        return ResultUtils.success(task.getId());
    }



    @GetMapping("/get")
    public BaseResponse<PictureTask> getPictureTask(@RequestParam("id") String id) {
        String cacheKey = "picture:task:" + id;

        // ================== 🚀 1. 查一级缓存 (Caffeine) ==================
        String caffeineValue = pictureTaskCache.getIfPresent(cacheKey);
        if (StrUtil.isNotBlank(caffeineValue)) {
            // 命中本地缓存！最快！
            if ("EMPTY".equals(caffeineValue)) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务不存在");
            }
            return ResultUtils.success(JSONUtil.toBean(caffeineValue, PictureTask.class));
        }

        // ================== 🚀 2. 查二级缓存 (Redis) ==================
        String redisValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(redisValue)) {
            // 命中 Redis！
            if ("EMPTY".equals(redisValue)) {
                // 回填 Caffeine (防止下次再穿透到 Redis)
                pictureTaskCache.put(cacheKey, "EMPTY");
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务不存在");
            }

            // 回填 Caffeine
            pictureTaskCache.put(cacheKey, redisValue);
            return ResultUtils.success(JSONUtil.toBean(redisValue, PictureTask.class));
        }

        // ================== 🛑 3. 查数据库 (加锁保护) ==================
        String lockKey = "lock:picture:task:" + id;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            lock.lock();

            // Double Check (Caffeine) - 抢到锁后再查一次本地
            // (虽然概率低，但为了极致严谨可以加，不想加也可以跳过)

            // Double Check (Redis) - 抢到锁后必须再查一次 Redis
            redisValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isNotBlank(redisValue)) {
                // 别的线程已经写进去了，回填本地并返回
                pictureTaskCache.put(cacheKey, redisValue);
                if ("EMPTY".equals(redisValue)) {
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务不存在");
                }
                return ResultUtils.success(JSONUtil.toBean(redisValue, PictureTask.class));
            }

            // 查 DB
            PictureTask task = pictureTaskService.getById(id);

            // 写缓存
            if (task != null) {
                String json = JSONUtil.toJsonStr(task);
                // 1. 写 Redis (5-10分钟)
                int expireTime = RandomUtil.randomInt(5, 10);
                stringRedisTemplate.opsForValue().set(cacheKey, json, expireTime, TimeUnit.MINUTES);

                // 2. 写 Caffeine
                pictureTaskCache.put(cacheKey, json);

                return ResultUtils.success(task);
            } else {
                // 空值保护
                stringRedisTemplate.opsForValue().set(cacheKey, "EMPTY", 2, TimeUnit.MINUTES);
                pictureTaskCache.put(cacheKey, "EMPTY"); // 本地也存空
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "任务不存在");
            }

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
