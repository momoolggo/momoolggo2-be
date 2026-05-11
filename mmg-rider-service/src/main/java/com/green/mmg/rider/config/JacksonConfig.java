package com.green.mmg.rider.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jackson-databind(Jackson 2, com.fasterxml) 직접 의존 시 Boot 자동설정 미포함 →
 * JavaTimeModule + WRITE_DATES_AS_TIMESTAMPS=false 명시 등록.
 *
 * <p>Spring Boot 4.0 기본 자동설정은 Jackson 3(tools.jackson) ObjectMapper 대상.
 * R5 RedisRiderLocationStore가 Jackson 2 ObjectMapper Bean inject — LocalDateTime ISO-8601
 * 직렬화를 위해 JavaTimeModule 명시 필요.</p>
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
