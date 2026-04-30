package com.green.mmg.main.address;

import com.green.mmg.main.address.model.AddressSearchRes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

/**
 * Phase 2-Backfill-D Step D-2-B: AddressSearchService 외부 호출 + RestTemplate 싱글톤 검증.
 *
 * <p>RestTemplate은 mock 주입 — 외부 네이버 API 실제 호출 0.
 * {@code @Value} 필드는 {@link ReflectionTestUtils}로 셋팅.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddressSearchService — 외부 호출 시나리오")
class AddressSearchServiceTest {

    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private AddressSearchService addressSearchService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(addressSearchService, "clientId", "TEST_CLIENT_ID");
        ReflectionTestUtils.setField(addressSearchService, "clientSecret", "TEST_CLIENT_SECRET");
        ReflectionTestUtils.setField(addressSearchService, "searchClientId", "TEST_SEARCH_ID");
        ReflectionTestUtils.setField(addressSearchService, "searchClientSecret", "TEST_SEARCH_SECRET");
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("search — 지역검색 우선, 빈 결과 시 Geocoding fallback")
    class Search {

        @Test
        @DisplayName("happy: 지역검색 응답 → mapx/mapy ÷ 1e7 변환 + HTML 태그 strip + Geocoding 미호출")
        void localSearchSucceeds_geocodingSkipped() {
            Map<String, Object> body = Map.of("items", List.of(Map.of(
                    "roadAddress", "<b>서울시</b> 강남구 역삼동",
                    "address", "서울시 강남구 역삼동 123-4",
                    "mapx", "1270276490",   // ÷ 1e7 = 127.0276490
                    "mapy", "374989410"     //         = 37.4989410
            )));
            when(restTemplate.exchange(startsWith("https://openapi.naver.com/v1/search/local.json"),
                    eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(body));

            List<AddressSearchRes> result = addressSearchService.search("강남");

            assertThat(result).hasSize(1);
            AddressSearchRes addr = result.get(0);
            assertThat(addr.getRoadAddress()).isEqualTo("서울시 강남구 역삼동");  // <b> strip
            assertThat(addr.getJibunAddress()).isEqualTo("서울시 강남구 역삼동 123-4");
            assertThat(addr.getLng()).isEqualTo(127.0276490);
            assertThat(addr.getLat()).isEqualTo(37.4989410);

            // Geocoding 미호출 동결 (fallback 안 탐)
            verify(restTemplate, never()).exchange(startsWith("https://maps.apigw.ntruss.com/map-geocode"),
                    any(HttpMethod.class), any(HttpEntity.class), eq(Map.class));
        }

        @Test
        @DisplayName("지역검색 빈 items → Geocoding fallback 호출")
        void localEmpty_fallsBackToGeocoding() {
            // 1. 지역검색: items 비어있음
            when(restTemplate.exchange(startsWith("https://openapi.naver.com/v1/search/local.json"),
                    eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("items", List.of())));
            // 2. Geocoding: addresses 1개
            Map<String, Object> geoBody = Map.of("addresses", List.of(Map.of(
                    "roadAddress", "서울시 강남구 테헤란로",
                    "jibunAddress", "역삼동 1번지",
                    "x", "127.0",
                    "y", "37.5"
            )));
            when(restTemplate.exchange(startsWith("https://maps.apigw.ntruss.com/map-geocode"),
                    eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(geoBody));

            List<AddressSearchRes> result = addressSearchService.search("강남");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoadAddress()).isEqualTo("서울시 강남구 테헤란로");
            assertThat(result.get(0).getLat()).isEqualTo(37.5);
            assertThat(result.get(0).getLng()).isEqualTo(127.0);
        }

        @Test
        @DisplayName("지역검색 timeout/예외 → catch → 빈 리스트 → Geocoding fallback (현재 동작 동결)")
        void localException_fallsBackToGeocoding() {
            when(restTemplate.exchange(startsWith("https://openapi.naver.com/v1/search/local.json"),
                    eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new ResourceAccessException("connect timeout"));
            when(restTemplate.exchange(startsWith("https://maps.apigw.ntruss.com/map-geocode"),
                    eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("addresses", List.of())));

            List<AddressSearchRes> result = addressSearchService.search("강남");

            assertThat(result).isEmpty();
            // Geocoding fallback 호출 동결 (지역검색 예외가 search() 전체를 깨면 안 됨)
            verify(restTemplate).exchange(startsWith("https://maps.apigw.ntruss.com/map-geocode"),
                    eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("reverseGeocode — 좌표 → 주소")
    class ReverseGeocode {

        @Test
        @DisplayName("happy: roadaddr + addr 응답 → roadAddress 합성")
        void happyPath_assemblesRoadAddress() {
            Map<String, Object> roadResult = Map.of(
                    "name", "roadaddr",
                    "region", Map.of(
                            "area1", Map.of("name", "서울특별시"),
                            "area2", Map.of("name", "강남구"),
                            "area3", Map.of("name", "역삼동")),
                    "land", Map.of("name", "테헤란로", "number1", "152", "number2", "")
            );
            Map<String, Object> body = Map.of("results", List.of(roadResult));
            when(restTemplate.exchange(startsWith("https://maps.apigw.ntruss.com/map-reversegeocode"),
                    eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(body));

            AddressSearchRes result = addressSearchService.reverseGeocode(37.5, 127.0);

            assertThat(result.getRoadAddress()).isEqualTo("서울특별시 강남구 역삼동 테헤란로 152");
            assertThat(result.getLat()).isEqualTo(37.5);
            assertThat(result.getLng()).isEqualTo(127.0);
        }

        @Test
        @DisplayName("results 빈 응답 → '주소를 찾을 수 없습니다.' fallback (현재 동작 동결)")
        void emptyResults_returnsNotFoundFallback() {
            when(restTemplate.exchange(startsWith("https://maps.apigw.ntruss.com/map-reversegeocode"),
                    eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("results", List.of())));

            AddressSearchRes result = addressSearchService.reverseGeocode(37.5, 127.0);

            assertThat(result.getRoadAddress()).isEqualTo("주소를 찾을 수 없습니다.");
            assertThat(result.getJibunAddress()).isEmpty();
            assertThat(result.getLat()).isEqualTo(37.5);
            assertThat(result.getLng()).isEqualTo(127.0);
        }

        @Test
        @DisplayName("RestTemplate 예외 → catch → '주소를 찾을 수 없습니다.' fallback (현재 동작 동결)")
        void exception_returnsNotFoundFallback() {
            when(restTemplate.exchange(startsWith("https://maps.apigw.ntruss.com/map-reversegeocode"),
                    eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new ResourceAccessException("read timeout"));

            AddressSearchRes result = addressSearchService.reverseGeocode(37.5, 127.0);

            assertThat(result.getRoadAddress()).isEqualTo("주소를 찾을 수 없습니다.");
            assertThat(result.getLat()).isEqualTo(37.5);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("HTTP 헤더 동결 — 외부 API에 client-id/secret 전달")
    class Headers {

        @Test
        @DisplayName("지역검색: X-Naver-Client-Id/Secret 헤더 전달 (응답 스펙 의존성 동결)")
        void localSearch_passesNaverHeaders() {
            when(restTemplate.exchange(startsWith("https://openapi.naver.com/v1/search/local.json"),
                    eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("items", List.of())));
            when(restTemplate.exchange(startsWith("https://maps.apigw.ntruss.com/map-geocode"),
                    eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("addresses", List.of())));

            addressSearchService.search("강남");

            ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).exchange(startsWith("https://openapi.naver.com/v1/search/local.json"),
                    eq(HttpMethod.GET), captor.capture(), eq(Map.class));
            assertThat(captor.getValue().getHeaders().getFirst("X-Naver-Client-Id"))
                    .isEqualTo("TEST_SEARCH_ID");
            assertThat(captor.getValue().getHeaders().getFirst("X-Naver-Client-Secret"))
                    .isEqualTo("TEST_SEARCH_SECRET");
        }
    }
}
