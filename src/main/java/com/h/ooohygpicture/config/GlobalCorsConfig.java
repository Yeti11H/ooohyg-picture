package com.h.ooohygpicture.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class GlobalCorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 1. 允许所有域名 (为了更兼容，可以用 addAllowedOriginPattern)
        config.addAllowedOriginPattern("*");
        // 2. 允许所有头
        config.addAllowedHeader("*");
        // 3. 允许所有方法 (GET, POST, etc)
        config.addAllowedMethod("*");
        // 4. 允许携带 Cookie
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
