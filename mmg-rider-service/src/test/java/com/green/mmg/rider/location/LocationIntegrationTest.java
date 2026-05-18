package com.green.mmg.rider.location;

import com.green.mmg.common.model.JwtUser;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.rider.location.model.RiderLocation;
import com.green.mmg.rider.rider.RiderRepository;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.VehicleType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 5-R5: LocationController + LocationService 통합 — 실 학원 DB + 로컬 Redis (docker mmg-redis).
 *
 * <p>R4-4 RiderInternalControllerIntegrationTest 패턴 일관 (@SpringBootTest + @Transactional + @Rollback +
 * fixture INSERT). SecurityContext 직접 주입 (`feedback_integration_test_setup.md` 박제).</p>
 *
 * <p>Redis는 @Transactional 영향 X — 테스트 종료 시 명시 delete (afterEach + UUID 격리).</p>
 *
 * <p>5건: PUT happy / PUT unauth(PENDING) 400 / Internal GET 200 / Internal GET 404 / TTL 만료 흐름은 짧아서 별건.</p>
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("LocationController/Service 통합 (R5-3)")
class LocationIntegrationTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private RiderRepository riderRepository;
    @Autowired private RiderLocationStore riderLocationStore;
    @Autowired private EntityManager em;

    private long testUserNo;
    private long testRiderNo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        testUserNo = -1L;
        testRiderNo = -1L;
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (testRiderNo > 0) {
            // Redis 키는 트랜잭션 영향 X — 명시 삭제 (테스트 격리)
            riderLocationStore.delete(testRiderNo);
        }
    }

    private long uniqueUserNo() {
        return Math.abs(System.nanoTime() + ThreadLocalRandom.current().nextLong(1, 10_000));
    }

    private Rider seedRider(boolean active) {
        Rider rider = new Rider(
                uniqueUserNo(),
                "11-22-" + UUID.randomUUID().toString().substring(0, 6) + "-44",
                "2종보통", VehicleType.MOTORBIKE,
                "신한은행", "110-123-456789", "홍길동");
        if (active) rider.approve();
        Rider saved = riderRepository.saveAndFlush(rider);
        testUserNo = saved.getUserNo();
        testRiderNo = saved.getRiderNo();
        return saved;
    }

    private void authenticateAs(long userNo) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userNo, "RIDER", null, "tester"));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_RIDER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("PUT /api/rider/location ACTIVE happy: 200 + Redis SET 후 Internal GET 200")
    void put_happy_storesAndReadable() throws Exception {
        Rider rider = seedRider(true);
        authenticateAs(rider.getUserNo());

        String body = """
                {"lat": 35.125, "lng": 128.456}
                """;
        mockMvc.perform(put("/api/rider/location").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultMessage").value("위치 갱신 완료"));

        // Redis 직접 조회 검증
        Optional<RiderLocation> stored = riderLocationStore.get(rider.getRiderNo());
        assertThat(stored).isPresent();
        assertThat(stored.get().lat()).isEqualTo(35.125);
        assertThat(stored.get().lng()).isEqualTo(128.456);
        assertThat(stored.get().updatedAt()).isNotNull();

        // Internal GET endpoint도 같은 데이터 반환 검증
        mockMvc.perform(get("/internal/rider/" + rider.getRiderNo() + "/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riderNo").value(rider.getRiderNo()))
                .andExpect(jsonPath("$.lat").value(35.125))
                .andExpect(jsonPath("$.lng").value(128.456))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("PUT /api/rider/location PENDING: 400 BAD_REQUEST + Redis 미저장")
    void put_pending_returns400() throws Exception {
        Rider rider = seedRider(false);
        authenticateAs(rider.getUserNo());

        String body = """
                {"lat": 35.0, "lng": 128.0}
                """;
        mockMvc.perform(put("/api/rider/location").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        assertThat(riderLocationStore.get(rider.getRiderNo())).isEmpty();
    }

    @Test
    @DisplayName("PUT /api/rider/location lat 범위 위반: 400 BAD_REQUEST")
    void put_invalidLat_returns400() throws Exception {
        Rider rider = seedRider(true);
        authenticateAs(rider.getUserNo());

        String body = """
                {"lat": 91.0, "lng": 128.0}
                """;
        mockMvc.perform(put("/api/rider/location").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        assertThat(riderLocationStore.get(rider.getRiderNo())).isEmpty();
    }

    @Test
    @DisplayName("GET /internal/rider/{riderNo}/location 미송신: 404 NOT_FOUND")
    void internalGet_notSent_returns404() throws Exception {
        Rider rider = seedRider(true);
        // PUT 안 함 — Redis 부재

        mockMvc.perform(get("/internal/rider/" + rider.getRiderNo() + "/location"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT 후 명시 delete → Internal GET 404")
    void put_thenDelete_returns404() throws Exception {
        Rider rider = seedRider(true);
        authenticateAs(rider.getUserNo());

        String body = """
                {"lat": 35.0, "lng": 128.0}
                """;
        mockMvc.perform(put("/api/rider/location").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        riderLocationStore.delete(rider.getRiderNo());

        mockMvc.perform(get("/internal/rider/" + rider.getRiderNo() + "/location"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /internal/rider/locations/active: TTL 살아있는 라이더만 반환 (Group 10 신설)")
    void activeLocations_returnsTtlAlive() throws Exception {
        Rider rider = seedRider(true);
        authenticateAs(rider.getUserNo());

        // 본 라이더 위치 PUT
        String body = """
                {"lat": 35.125, "lng": 128.456}
                """;
        mockMvc.perform(put("/api/rider/location").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        // SCAN 결과 본 라이더 포함 확인 (다른 테스트의 Redis 키와 격리되도록 본 라이더만 검증)
        mockMvc.perform(get("/internal/rider/locations/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.riderNo == " + rider.getRiderNo() + ")].lat").value(35.125))
                .andExpect(jsonPath("$[?(@.riderNo == " + rider.getRiderNo() + ")].lng").value(128.456));
    }
}
