package com.green.mmg.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.config.PredicateProperties;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4-B 백필: Gateway 동작 종합 동결 (Spring Cloud Gateway WebMVC 변종).
 *
 * <p>InternalBlock 정책: {@code /internal/**} 외부 차단은 <b>헤더 무관 무조건 403</b>.
 * 헤더 토큰 검증 / mTLS는 CLAUDE.md §3 명시대로 <b>Phase 6 작업</b>이며,
 * 본 테스트는 Phase 4-B 시점 동작을 잠근다 — Phase 6 진입 시 어떤 동작이 변경되는지
 * 명확한 기준점이 됨.</p>
 *
 * <p>라우트/CORS/404 동결은 별도 @Nested에서 추가 예정 (Step 4·5).</p>
 */
@SpringBootTest
class GatewayIntegrationTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Nested
    @DisplayName("InternalBlock — /internal/** 외부 차단 (Phase 6 헤더 검증/mTLS 전 동작 잠금)")
    class InternalBlock {

        private static final String EXPECTED_MESSAGE = "외부 접근 불가 — Internal API는 서비스 간 통신 전용입니다.";

        @ParameterizedTest(name = "[{index}] {0} → 403")
        @ValueSource(strings = {
                "/internal/users/123",
                "/internal/users/batch",
                "/internal/orders/abc",
                "/internal",
                "/internal/"
        })
        @DisplayName("sub-path 다양성: 모든 /internal/** 경로 → 403 + ResultResponse 메시지 동결")
        void allSubPaths_returns403WithResultResponse(String path) throws Exception {
            mockMvc.perform(get(path))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.resultMessage").value(EXPECTED_MESSAGE))
                    .andExpect(jsonPath("$.resultData").doesNotExist());
        }

        @ParameterizedTest(name = "[{index}] {0} /internal/users/123 → 403")
        @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE"})
        @DisplayName("HTTP method 다양성: GET/POST/PUT/PATCH/DELETE 모두 → 403 (method-agnostic 동결)")
        void allMethods_returns403(String method) throws Exception {
            mockMvc.perform(request(HttpMethod.valueOf(method), "/internal/users/123"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.resultMessage").value(EXPECTED_MESSAGE));
        }

        @Test
        @DisplayName("위조 헤더 주입(X-Internal/Authorization/X-Forwarded-For) → 동일 403 — 헤더 검증 로직 부재 동결 (Phase 6 도입 시 변경 기준점)")
        void forgedHeaders_returnSame403() throws Exception {
            mockMvc.perform(get("/internal/users/123")
                            .header("X-Internal", "true")
                            .header("Authorization", "Bearer fake-internal-token")
                            .header("X-Forwarded-For", "10.0.0.1"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.resultMessage").value(EXPECTED_MESSAGE));
        }

        @Test
        @DisplayName("/internal 외 경로는 InternalBlock 미적용 — 라우트도 없는 경로(/api/nonexistent)는 다른 응답(403 아님)")
        void nonInternalPath_notBlockedByInternalBlock() throws Exception {
            // 의도: InternalBlockController가 모든 경로를 잡는 게 아님을 동결.
            // /api/nonexistent는 라우트도 없고 InternalBlockController도 적용 안 됨 → 403이 아닌 응답.
            mockMvc.perform(get("/api/nonexistent"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 403) {
                            throw new AssertionError("InternalBlock가 /internal/** 외 경로에 적용됨 — 정책 위반: status=" + status);
                        }
                    });
        }
    }

    /**
     * 라우트 정의 12개 동결 — yml 정렬 변경 시 즉시 회귀 검출.
     *
     * <p>특히 review-route(/api/user/review/**)는 auth-user-route(/api/user/**)
     * 보다 *위에* 있어야 prefix 충돌 회피로 main(8080)에 라우팅됨. 순서 뒤집히면
     * review가 auth(8081)로 라우팅되어 404.</p>
     */
    @Nested
    @DisplayName("RouteDefinition — yml 라우트 12개 동결 (prefix 매칭 순서 + uri + path)")
    class RouteDefinition {

        @Autowired
        GatewayMvcProperties gatewayMvcProperties;

        @Test
        @DisplayName("12개 라우트 정의 + review-route 1번 위치 동결 (prefix 충돌 회피)")
        void totalCount_andReviewRouteFirst() {
            List<RouteProperties> routes = gatewayMvcProperties.getRoutes();

            assertThat(routes).hasSize(12);
            assertThat(routes.get(0).getId())
                    .as("review-route는 1번 위치 (auth-user-route 위) — prefix 충돌 회피")
                    .isEqualTo("review-route");
            assertThat(routes.get(1).getId())
                    .as("auth-user-route는 2번 위치 (review-route 아래)")
                    .isEqualTo("auth-user-route");
        }

        @ParameterizedTest(name = "[{0}] {1} → {2} {3}")
        @CsvSource({
                "0, review-route,        http://localhost:8080, /api/user/review/**",
                "1, auth-user-route,     http://localhost:8081, /api/user/**",
                "2, auth-policy-route,   http://localhost:8081, /api/policy/**",
                "3, main-store-route,    http://localhost:8080, /api/store/**",
                "4, main-cart-route,     http://localhost:8080, /api/cart/**",
                "5, main-order-route,    http://localhost:8080, /api/order/**",
                "6, main-payment-route,  http://localhost:8080, /api/payment/**",
                "7, main-address-route,  http://localhost:8080, /api/address/**",
                "8, main-owner-route,    http://localhost:8080, /api/owner/**",
                "9, main-uploads-route,  http://localhost:8080, /uploads/**",
                "10, rider-route,        http://localhost:8082, /api/rider/**",
                "11, admin-route,        http://localhost:8083, /api/admin/**"
        })
        @DisplayName("각 인덱스별 id/uri/Path predicate 동결 (yml 변경 즉시 검출)")
        void routeFieldsFrozen(int index, String expectedId, String expectedUri, String expectedPath) {
            RouteProperties route = gatewayMvcProperties.getRoutes().get(index);

            assertThat(route.getId()).isEqualTo(expectedId);
            assertThat(route.getUri()).hasToString(expectedUri);

            // Path predicate 추출 — name="Path", args에 path pattern 포함
            List<PredicateProperties> predicates = route.getPredicates();
            assertThat(predicates)
                    .as("라우트 [%s] predicates 1개 (Path) 정의", expectedId)
                    .hasSize(1);
            PredicateProperties pathPredicate = predicates.get(0);
            assertThat(pathPredicate.getName()).isEqualTo("Path");
            // args는 Map<String, String> — 단일 path는 _genkey_0 키로 들어감
            assertThat(pathPredicate.getArgs().values())
                    .as("라우트 [%s] Path 패턴 동결", expectedId)
                    .containsExactly(expectedPath);
        }
    }
}
