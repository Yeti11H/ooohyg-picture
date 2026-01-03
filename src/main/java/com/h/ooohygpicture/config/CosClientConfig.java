package com.h.ooohygpicture.config;


import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cos.client") // 读取 yaml 中 cos.client 开头的配置
@Data
public class CosClientConfig {

    private String accessKey;
    private String secretKey;
    private String region;
    private String bucket;
    private String host;

    @Bean
    public COSClient cosClient() {
        // 1. 初始化用户身份信息 (SecretId, SecretKey)
        COSCredentials cred = new BasicCOSCredentials(accessKey, secretKey);

        // 2. 设置 bucket 的地域
        ClientConfig clientConfig = new ClientConfig(new Region(region));

        // 🔥【关键】加上这一行！告诉 SDK 签名时要带上 pic-operations
        // 这样数据万象功能才能生效
        clientConfig.setHttpProtocol(HttpProtocol.https); // 建议加上


        // 3. 生成 cos 客户端
        return new COSClient(cred, clientConfig);
    }
}

