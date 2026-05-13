package com.green.mmg.main.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * main-service 정적 리소스 핸들러.
 * 이미지 저장 책임은 main이 단독 (CLAUDE.md §5).
 * 다른 서비스(rider/admin)가 받은 이미지는 Phase 4 Internal API로 main에 전달.
 *
 * 향후(Phase 4 Gateway 라우팅): /uploads/** 는 Gateway가 main으로 라우팅 →
 * main의 이 핸들러가 실제 디스크에서 파일 서빙.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.menu-path}")   private String menuPath;
    @Value("${file.upload.store-path}")  private String storePath;
    @Value("${file.upload.review-path:}")  private String reviewPath;
    @Value("${file.upload.pet-path:}")     private String petPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/menu/**")
                .addResourceLocations("file:" + menuPath);
        registry.addResourceHandler("/uploads/store/**")
                .addResourceLocations("file:" + storePath);
        if (!reviewPath.isBlank()) {
            registry.addResourceHandler("/uploads/review/**")
                    .addResourceLocations("file:" + reviewPath);
        }
        if (!petPath.isBlank()) {
            registry.addResourceHandler("/uploads/pet/**")
                    .addResourceLocations("file:" + petPath);
        }
    }
}
