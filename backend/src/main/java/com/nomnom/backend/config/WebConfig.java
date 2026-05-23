package com.nomnom.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Value("${nomnom.upload-path:uploads}")
    private String uploadPath;

    public WebConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns(
                        "/api/orders/**", "/api/admin/**", "/api/common/**",
                        "/api/cart/**", "/api/collection/**",
                        "/api/messages/**", "/api/refunds/**",
                        "/api/wallet/**", "/api/evaluations/**")
                .excludePathPatterns("/api/evaluations/dish/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + Paths.get(uploadPath).toAbsolutePath().toString().replace("\\", "/") + "/");
    }
}
