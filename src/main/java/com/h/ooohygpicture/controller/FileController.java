package com.h.ooohygpicture.controller;

import cn.hutool.core.io.FileUtil;
import com.h.ooohygpicture.common.BaseResponse;
import com.h.ooohygpicture.common.ResultUtils;
import com.h.ooohygpicture.exception.BusinessException;
import com.h.ooohygpicture.exception.ErrorCode;
import com.h.ooohygpicture.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     * 只有管理员能用，防止被刷流量
     * @param multipartFile 前端传的文件
     */
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 1. 判空
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        // 2. 校验文件格式 (白名单)
        String originalFilename = multipartFile.getOriginalFilename();
        // 获取后缀名 (例如: .jpg -> jpg)
        // 如果没有 Hutool，可以用: String suffix = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        String suffix = FileUtil.getSuffix(originalFilename);

        // 定义允许的格式
        final List<String> ALLOW_LIST = Arrays.asList("jpeg", "jpg", "png", "webp", "gif");
        if (!ALLOW_LIST.contains(suffix.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型不支持");
        }

        // 3. 生成新文件名 (UUID)
        // 结果类似: 550e8400-e29b-41d4-a716-446655440000.jpg
        String uuid = UUID.randomUUID().toString();
        String filename = uuid + "." + suffix;

        // 4. 拼接路径 (test/uuid.jpg)
        // 这里暂时不按日期分，先跑通最简单的。后续你可以在 PictureController 里加日期。
        String filepath = String.format("/test/%s", filename);

        File file = null;
        try {
            // 5. 创建临时文件
            file = File.createTempFile(uuid, null);
            multipartFile.transferTo(file);

            // 6. 上传 (调用 Manager)
            cosManager.putObject(filepath, file);

            // 7. 返回 URL (调用 Manager 封装好的方法)
            // 结果类似: https://ooohyg-139.../test/550e...jpg
            String url = cosManager.getUrl(filepath);

            return ResultUtils.success(url);

        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 8. 删除临时文件 (防磁盘爆满)
            if (file != null) {
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }
}
