package com.h.ooohygpicture.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineConfig {

    @Bean
    public Cache<String, String> pictureTaskCache() {
        return Caffeine.newBuilder()
                .initialCapacity(100) // 初始大小
                .maximumSize(10000)   // 最大容量 (防止内存溢出)
                .expireAfterWrite(1, TimeUnit.MINUTES) // 写入 1 分钟后过期 (一级缓存时间要短)
                .build();
    }
}
