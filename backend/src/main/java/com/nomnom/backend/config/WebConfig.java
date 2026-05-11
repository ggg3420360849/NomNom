package com.nomnom.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将浏览器的 /uploads/** 请求映射到硬盘的 D:/nomnom_uploads/ 文件夹
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:D:/nomnom_uploads/");
    }
}