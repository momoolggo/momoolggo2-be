package com.green.mmg.admin.configuration;

import feign.Client;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfiguration {

    // OkHttp 클라이언트 등록 (PATCH 메서드 지원)
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }

    @Bean
    public Client feignClient(OkHttpClient okHttpClient) {
        return new feign.okhttp.OkHttpClient(okHttpClient);
    }

    // Feign 호출 시 현재 요청의 JWT 토큰을 자동으로 헤더에 실어서 전달
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authorization = request.getHeader("Authorization");

                if (authorization != null) {
                    requestTemplate.header("Authorization", authorization);
                }
            }
        };
    }
}