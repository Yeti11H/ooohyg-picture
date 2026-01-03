package com.h.ooohygpicture.manager;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.utils.Constants;
import com.h.ooohygpicture.exception.BusinessException;
import com.h.ooohygpicture.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AiManager {
    // ✅ 修改后：
    @Value("${aliyun.ai.api-key}")
    private String apiKey;

    public String createPicture(String prompt) {
        Constants.apiKey = this.apiKey;
        try {
            // 1. 先定义一个 Map 来存放额外参数
            Map<String, Object> extraParams = new HashMap<>();
            extraParams.put("format", "png"); // 👈 显式指定生成 PNG 格式

            ImageSynthesis is = new ImageSynthesis();
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .model(ImageSynthesis.Models.WANX_V1)
                    .prompt(prompt)
                    .n(1)
                    .size("1024*1024")
                    .parameters(extraParams) // 👈 在这里把 Map 塞进去
                    .build();

            ImageSynthesisResult result = is.call(param);
            // 获取第一张图的 URL
            return result.getOutput().getResults().get(0).get("url");
        } catch (Exception e) {
            log.error("AI 绘图失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 绘图失败");
        }
    }
    /**
     * AI 风格重绘 (修正版)
     * @param imageUrl 原图
     * @param styleIndex 风格索引 (0~4)
     */
    public String createOutPaintingPicture(String imageUrl, Integer styleIndex) {
        Constants.apiKey = this.apiKey;
        try {
            Map<String, Object> extraParams = new HashMap<>();
            extraParams.put("base_image_url", imageUrl);
            extraParams.put("style_index", styleIndex != null ? styleIndex : 2);

            ImageSynthesis is = new ImageSynthesis();
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .model("wanx-v1")
                    .n(1)
                    .size("1024*1024")
                    .parameters(extraParams)
                    // 🚀🚀🚀 就是这一行！必须加上！🚀🚀🚀
                    .prompt("style repaint")
                    .header("X-DashScope-Async", "enable")
                    .build();

            ImageSynthesisResult response = is.call(param);

            // 获取任务 ID
            String taskId = response.getOutput().getTaskId();
            log.info("AI 任务提交成功, taskId: {}", taskId);

            // 轮询查结果
            int maxRetry = 20;
            for (int i = 0; i < maxRetry; i++) {
                Thread.sleep(1000);
                ImageSynthesisResult taskResult = is.fetch(taskId, null);
                String status = taskResult.getOutput().getTaskStatus();

                if ("SUCCEEDED".equals(status)) {
                    return taskResult.getOutput().getResults().get(0).get("url");
                } else if ("FAILED".equals(status) || "CANCELED".equals(status)) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 任务失败: " + taskResult.getOutput().getMessage());
                }
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 任务超时");

        } catch (Exception e) {
            log.error("AI 处理失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 服务异常: " + e.getMessage());
        }
    }
}