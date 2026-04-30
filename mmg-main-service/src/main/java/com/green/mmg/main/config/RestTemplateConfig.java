package com.green.mmg.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Phase 2-Backfill-D Step D-2-B: 외부 API 호출용 RestTemplate 싱글톤 Bean.
 *
 * <p>이전: {@code AddressSearchService}가 매 요청마다 {@code new RestTemplate()} 생성 + timeout 미설정.<br>
 * 변경: Bean 싱글톤 주입 + connect 3s / read 5s 명시.</p>
 *
 * <p>호출처: {@link com.green.mmg.main.address.AddressSearchService} (네이버 지역검색/Geocoding/Reverse).
 * Phase 5에서 토스 결제 연동(현재 HttpURLConnection)이 RestTemplate으로 전환될 때 동일 Bean 재사용 가능 —
 * 타임아웃 값이 더 짧아야 하면 그때 별도 {@code @Qualifier} Bean 분리.</p>
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return new RestTemplate(factory);
    }
}
