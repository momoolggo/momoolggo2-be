package com.green.mmg.main.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2-Backfill-D Step D-2-B: RestTemplateConfig Bean의 timeout 설정 동결 검증.
 *
 * <p>Spring 컨텍스트 의존 0 — Config 클래스를 직접 인스턴스화해 Bean 메서드를 호출.
 * timeout 변경(예: 3s → 10s) 시 본 테스트가 즉시 fail해 의도적 변경임을 강제.</p>
 */
@DisplayName("RestTemplateConfig — Bean timeout 동결")
class RestTemplateConfigTest {

    @Test
    @DisplayName("connect 3s / read 5s SimpleClientHttpRequestFactory 적용 (값 동결)")
    void restTemplate_hasConfiguredTimeouts() throws Exception {
        RestTemplate restTemplate = new RestTemplateConfig().restTemplate();

        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);

        SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        // timeout 값은 private 필드 — Reflection으로 동결 검증 (Spring 6 SimpleClientHttpRequestFactory 구현 동결)
        java.lang.reflect.Field connectField = SimpleClientHttpRequestFactory.class.getDeclaredField("connectTimeout");
        connectField.setAccessible(true);
        java.lang.reflect.Field readField = SimpleClientHttpRequestFactory.class.getDeclaredField("readTimeout");
        readField.setAccessible(true);

        assertThat(connectField.getInt(factory)).isEqualTo(3000);  // 3 sec
        assertThat(readField.getInt(factory)).isEqualTo(5000);     // 5 sec
    }
}
