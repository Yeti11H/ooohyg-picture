package com.h.ooohygpicture.listen;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.h.ooohygpicture.exception.BusinessException;
import com.h.ooohygpicture.exception.ErrorCode;
import com.h.ooohygpicture.manager.AiManager;
import com.h.ooohygpicture.model.dto.picture.CreatePictureOutPaintingTaskRequest;
import com.h.ooohygpicture.model.dto.picture.PictureUploadRequest;
import com.h.ooohygpicture.model.entity.CreditLog;
import com.h.ooohygpicture.model.entity.Picture;
import com.h.ooohygpicture.model.entity.PictureTask;
import com.h.ooohygpicture.model.entity.User;
import com.h.ooohygpicture.model.vo.PictureVO;
import com.h.ooohygpicture.service.CreditLogService;
import com.h.ooohygpicture.service.PictureService;
import com.h.ooohygpicture.service.PictureTaskService;
import com.h.ooohygpicture.service.UserService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class PictureTaskListener {
    // 注入 Caffeine 缓存 (记得加 @Resource)
    @Resource
    private Cache<String, String> pictureTaskCache;
    @Resource
    private PictureTaskService pictureTaskService;
    @Resource
    private AiManager aiManager;
    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService; // 👈 需要注入这个来查用户
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    @Lazy
    private PictureTaskListener self;
    @Resource
    private RedissonClient redissonClient; // 👈 记得注入这个
    @Resource
    private CreditLogService creditLogService;


    @Transactional(rollbackFor = Exception.class)
    public void deductCredits(Long taskId) {
        // 1. 查任务 (任务ID不需要锁，先查出来拿到 userId)
        PictureTask task = pictureTaskService.getById(taskId);
        if (task == null) {
            // 任务都没了，没必要扣费了
            return;
        }
        Long userId = task.getUserId();

        // 2. 定义锁的名称 (锁住用户ID，而不是任务ID)
        // 这样同一个用户并发扣费会排队，不同用户互不影响
        String lockKey = "lock:credits:" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 3. 尝试抢锁 (等待 10 秒，上锁后 30 秒自动过期)
            // 为什么是 tryLock 而不是 lock？
            // 因为如果抢不到锁（说明正在扣费中），稍微等一下可能就轮到了。
            boolean isLocked = lock.tryLock(10, 30, TimeUnit.SECONDS);

            if (!isLocked) {
                // 抢不到锁，说明并发太高了，或者系统卡住了
                // 这里可以选择抛异常重试，或者直接失败
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "系统繁忙，请稍后再试");
            }

            // 4. 【关键】拿到锁后，再查一次用户余额 (Double Check)
            User loginUser = userService.getById(userId);
            if (loginUser.getCredits() < 1L) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "积分余额不足");
            }

            // 5. 扣费
            boolean success = userService.updateCredits(userId, -1L);
            if (!success) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "扣费失败");
            }

            // 6. 记录流水 (既然都加了锁，这里也可以顺便把流水记了，保证数据完整)
            CreditLog creditLog = new CreditLog();
            creditLog.setUserId(userId);
            creditLog.setAmount(-1L);
            creditLog.setType(1); // 1-画图扣费
            creditLog.setRemark("AI画图扣费-任务ID:" + taskId);
            creditLogService.save(creditLog);

        } catch (InterruptedException e) {
            log.error("扣费获取锁中断", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扣费失败");
        } finally {
            // 7. 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    // 监听队列
    @RabbitListener(queues = "ai_draw_queue")
    public void receiveMessage(String taskIdStr, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        // ================== 멱等性保证 START ==================
        String idempotentKey = "task:consumed:" + taskIdStr;
        // 尝试设置 Key，并设置 5 分钟过期。setIfAbsent 对应 setnx
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", 5, TimeUnit.MINUTES);

        // 如果设置失败，说明 5 分钟内已经有别的线程消费过了，直接丢弃消息
        if (Boolean.FALSE.equals(success)) {
            log.warn("消息重复消费，任务ID: {}", taskIdStr);
            try {
                // 直接确认消息，告诉 MQ 我处理完了 (虽然是丢弃，但也算处理完)
                channel.basicAck(deliveryTag, false);
            } catch (IOException e) {
                // ignore
            }
            return; // 👈 关键：直接返回，不执行后面的逻辑
        }
        // ================== 멱等性保证 END ==================
        log.info("收到 AI 绘图任务，ID: {}", taskIdStr);
        Long taskId = Long.parseLong(taskIdStr);



        // 1. 查任务
        // 在 setIfAbsent 成功之后，查库前：
        PictureTask task = pictureTaskService.getById(taskId);
        // 如果发现状态已经是 "SUCCEEDED"，说明以前做成功过，只是Redis锁过期了
        if (task != null && "SUCCEEDED".equals(task.getStatus())) {
            channel.basicAck(deliveryTag, false); // 补发确认
            return; // 直接返回
        }


        try {
            self.deductCredits(taskId);
            // 2. 查用户 (因为这里没有 Request，必须用 ID 查)
            User loginUser = userService.getById(task.getUserId());

            // 3. 更新状态：开始执行
            task.setStatus("RUNNING");
            pictureTaskService.updateById(task);
            stringRedisTemplate.delete("picture:task:" + taskId);
            pictureTaskCache.invalidate("picture:task:" + taskId);
           // throw new RuntimeException("测试死信");

            // ==============================================
            // 👇 这里就是你原来的 Controller 逻辑！完美复刻过来 👇
            // ==============================================

            // 🌟 核心分流逻辑
            String aiUrl;
            if (task.getPictureId() != null) {
                // === 分支 B：扩图任务 ===
                // 1. 从 task.prompt 里解析出参数 (style, ratio)
                CreatePictureOutPaintingTaskRequest.CreatePictureOutPaintingTaskParam param =
                        JSONUtil.toBean(task.getPrompt(), CreatePictureOutPaintingTaskRequest.CreatePictureOutPaintingTaskParam.class);

                // 2. 查原图
                Picture originalPicture = pictureService.getById(task.getPictureId());

                // 3. 构造提示词 (复用你刚才写好的 PromptBuilder 逻辑)
                // ... (把刚才 Service 里的 PromptBuilder 逻辑搬过来) ...
                String prompt = "这里拼接好的prompt...";

                // 4. 调用 AI
                aiUrl = aiManager.createPicture(prompt);
            }
            else{
                // === 分支 A：文生图任务 (旧逻辑) ===
                aiUrl = aiManager.createPicture(task.getPrompt());
            }
            // B. 构造上传请求 (记得加上 .png 后缀解决打不开的问题)
            PictureUploadRequest uploadRequest = new PictureUploadRequest();
            uploadRequest.setFileUrl(aiUrl);
            uploadRequest.setPicName(task.getPrompt() + ".png"); // 👈 强制加后缀

            // C. 复用你的上传逻辑 (下载、上传COS、存picture表)
            PictureVO pictureVO = pictureService.uploadPicture(aiUrl, uploadRequest, loginUser);

            // ==============================================
            // 👆 原来的逻辑结束 👆
            // ==============================================

            // 4. 任务成功，回填数据
            task.setStatus("SUCCEEDED");
            stringRedisTemplate.delete("picture:task:" + taskId);
            pictureTaskCache.invalidate("picture:task:" + taskId);
            task.setOutputUrl(aiUrl);
            task.setPictureId(pictureVO.getId()); // 关联生成的图片ID
            pictureTaskService.updateById(task);

            // 5. 手动确认消息 (告诉 MQ 我干完了，可以删消息了)
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("AI 任务执行失败", e);
            // 6. 任务失败，更新状态
            task.setStatus("FAILED");
            stringRedisTemplate.delete("picture:task:" + taskId);
            pictureTaskCache.invalidate("picture:task:" + taskId);
            pictureTaskService.updateById(task);

            // 拒绝消息 (是否重试看你需求，这里选择不重试直接丢弃，防止死循环)
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}