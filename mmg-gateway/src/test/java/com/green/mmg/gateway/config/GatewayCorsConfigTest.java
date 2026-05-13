package com.green.mmg.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 4-B 백필: GatewayCorsConfig.parseAllowedOrigins 단위 테스트.
 *
 * <p>env CORS_ALLOWED_ORIGINS 다양한 형태(공백/빈 항목)에서 origin 매칭에 안전한
 * 결과를 내는지 동결. 콤마 뒤 공백, 연속 콤마, 빈 입력 모두 검증.</p>
 */
@DisplayName("GatewayCorsConfig.parseAllowedOrigins — env 공백/빈 항목 안전 처리")
class GatewayCorsConfigTest {

    @Test
    @DisplayName("env 다양한 형태(공백/연속 콤마/단일 origin) → trim 후 빈 항목 제거된 List 반환")
    void parseAllowedOrigins_trimAndFilterEmpty() {
        // 단일 origin (공백 없음)
        assertThat(GatewayCorsConfig.parseAllowedOrigins("http://localhost:5173"))
                .containsExactly("http://localhost:5173");

        // 콤마 뒤 공백 — trim 효과 검증 (시한폭탄 핵심)
        assertThat(GatewayCorsConfig.parseAllowedOrigins("http://a.com, http://b.com"))
                .as("콤마 뒤 공백 trim — origin 매칭 실패 차단")
                .containsExactly("http://a.com", "http://b.com");

        // 다중 공백 + 콤마 앞뒤 공백
        assertThat(GatewayCorsConfig.parseAllowedOrigins("  http://a.com  ,  http://b.com  "))
                .containsExactly("http://a.com", "http://b.com");

        // 연속 콤마 / 빈 항목 → filter 효과
        assertThat(GatewayCorsConfig.parseAllowedOrigins("http://a.com,,http://b.com"))
                .as("연속 콤마로 생기는 빈 항목 제거")
                .containsExactly("http://a.com", "http://b.com");

        // 빈 문자열 입력 → 빈 List
        assertThat(GatewayCorsConfig.parseAllowedOrigins(""))
                .as("빈 입력 → 빈 List (NPE 차단)")
                .isEmpty();

        // 공백만 있는 입력 → 빈 List
        assertThat(GatewayCorsConfig.parseAllowedOrigins("   "))
                .as("공백만 입력 → 빈 List")
                .isEmpty();
    }
}
