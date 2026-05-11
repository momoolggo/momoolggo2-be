package com.green.mmg.rider.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4.0 + spring-boot-starter-web에서 ObjectMapper 자동 등록 누락 대응.
 *
 * <p>R5 RedisRiderLocationStore가 ObjectMapper Bean inject — JavaTimeModule(LocalDateTime ISO-8601) +
 * WRITE_DATES_AS_TIMESTAMPS=false 설정 명시.</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
