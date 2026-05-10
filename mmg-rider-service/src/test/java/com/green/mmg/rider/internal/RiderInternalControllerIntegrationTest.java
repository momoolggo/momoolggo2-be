package com.green.mmg.rider.internal;

import com.green.mmg.rider.delivery.DeliveryLogRepository;
import com.green.mmg.rider.delivery.DeliveryRepository;
import com.green.mmg.rider.delivery.model.ActorRole;
import com.green.mmg.rider.delivery.model.Delivery;
import com.green.mmg.rider.delivery.model.DeliveryLog;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import com.green.mmg.rider.feign.MainInternalClient;
import com.green.mmg.rider.feign.dto.DeliveryStatusUpdateReq;
import com.green.mmg.rider.feign.dto.DeliveryStatusUpdateRes;
import com.green.mmg.rider.rider.RiderRepository;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.VehicleType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 5-R4-4: RiderInternalController 통합 테스트 — 실 학원 DB + Feign mock.
 *
 * <p>Phase 4-A {@code InternalUserControllerIntegrationTest} + R3-c {@code DeliveryServiceIntegrationTest}
 * 정착 패턴 일관 ({@code @SpringBootTest + @Transactional + @Rollback + fixture INSERT + em.clear}).</p>
 *
 * <p>{@code MainInternalClient}는 {@code @MockitoBean} (Spring Boot 4.0+) — 외부 호출 0.
 * R4 사용처(assign 후 best-effort 동기화)를 mock으로 호출 검증.</p>
 *
 * <p>5건: assign happy / assign PENDING 400 / status without progress / status with progress / location 404 stub.</p>
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("RiderInternalController 통합 (R4-4)")
class RiderInternalControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private RiderRepository riderRepository;
    @Autowired private DeliveryRepository deliveryRepository;
    @Autowired private DeliveryLogRepository deliveryLogRepository;
    @Autowired private EntityManager em;

    @MockitoBean private MainInternalClient mainInternalClient;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
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
        return riderRepository.saveAndFlush(rider);
    }

    /**
     * sample assign request JSON — interfaces.md §1.1 Body 박제 일관.
     * ObjectMapper 의존 회피 (mmg-rider-service implementation 스코프 격리), text block 직접 박제.
     */
    private String sampleReqJson(String orderId) {
        return String.format("""
                {
                  "orderId": "%s",
                  "storeNo": 7,
                  "storeName": "맛있는집",
                  "storeAddress": "가게 주소",
                  "storeLat": 35.123,
                  "storeLng": 128.456,
                  "storePhone": "053-111-2222",
                  "deliveryAddress": "손님 주소",
                  "deliveryLat": 35.130,
                  "deliveryLng": 128.460,
                  "customerPhone": "010-1234-5678",
                  "baseFee": 4000,
                  "extraFee": 1500
                }
                """, orderId);
    }

    @Test
    @DisplayName("POST /assign happy: 200 + delivery DB ASSIGNED + log INSERT(SYSTEM) + MainInternalClient 호출")
    void assign_happy() throws Exception {
        Rider rider = seedRider(true);
        when(mainInternalClient.updateDeliveryStatus(any(), any()))
                .thenReturn(new DeliveryStatusUpdateRes("ORD0001", 0, 1));

        String reqJson = sampleReqJson("ORD0001");

        mockMvc.perform(post("/internal/rider/" + rider.getRiderNo() + "/assign")
                        .contentType(APPLICATION_JSON).content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned").value(true))
                .andExpect(jsonPath("$.riderNo").value(rider.getRiderNo()))
                .andExpect(jsonPath("$.deliveryNo").value(matchesPattern("^[0-9]{5}[A-Z]{3}$")))
                .andExpect(jsonPath("$.assignedAt").exists());

        em.flush();
        em.clear();

        // Delivery DB 검증
        List<Delivery> deliveries = deliveryRepository.findAll().stream()
                .filter(d -> "ORD0001".equals(d.getOrderId()))
                .toList();
        assertThat(deliveries).hasSize(1);
        Delivery saved = deliveries.get(0);
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(saved.getRiderNo()).isEqualTo(rider.getRiderNo());
        assertThat(saved.getAssignedAt()).isNotNull();
        assertThat(saved.getBaseFee()).isEqualTo(4000);

        // log 검증
        List<DeliveryLog> logs = deliveryLogRepository.findAll().stream()
                .filter(l -> saved.getDeliveryNo().equals(l.getDeliveryNo()))
                .toList();
        assertThat(logs).hasSize(1);
        DeliveryLog log = logs.get(0);
        assertThat(log.getFromStatus()).isNull();
        assertThat(log.getToStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(log.getActorRole()).isEqualTo(ActorRole.SYSTEM);
        assertThat(log.getActorUserNo()).isNull();
        assertThat(log.getChangedAt()).isNotNull();

        // Main 동기화 호출 검증
        verify(mainInternalClient).updateDeliveryStatus(eq("ORD0001"), any(DeliveryStatusUpdateReq.class));
    }

    @Test
    @DisplayName("POST /assign rider PENDING: 400 BAD_REQUEST + Main 동기화 미호출")
    void assign_pendingRider_returns400() throws Exception {
        Rider rider = seedRider(false);
        String reqJson = sampleReqJson("ORD0002");

        mockMvc.perform(post("/internal/rider/" + rider.getRiderNo() + "/assign")
                        .contentType(APPLICATION_JSON).content(reqJson))
                .andExpect(status().isBadRequest());

        verify(mainInternalClient, never()).updateDeliveryStatus(any(), any());
    }

    @Test
    @DisplayName("GET /status without progress: 200 + currentDeliveryNo null + status ACTIVE")
    void status_noProgress() throws Exception {
        Rider rider = seedRider(true);

        mockMvc.perform(get("/internal/rider/" + rider.getRiderNo() + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riderNo").value(rider.getRiderNo()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentDeliveryNo").value(nullValue()));
    }

    @Test
    @DisplayName("GET /status with in-progress delivery: 200 + currentDeliveryNo 반환")
    void status_withProgress() throws Exception {
        Rider rider = seedRider(true);
        when(mainInternalClient.updateDeliveryStatus(any(), any()))
                .thenReturn(new DeliveryStatusUpdateRes("ORD0003", 0, 1));

        String reqJson = sampleReqJson("ORD0003");
        mockMvc.perform(post("/internal/rider/" + rider.getRiderNo() + "/assign")
                        .contentType(APPLICATION_JSON).content(reqJson))
                .andExpect(status().isOk());

        em.flush();
        em.clear();

        mockMvc.perform(get("/internal/rider/" + rider.getRiderNo() + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentDeliveryNo").value(matchesPattern("^[0-9]{5}[A-Z]{3}$")));
    }

    @Test
    @DisplayName("GET /location: 404 NOT_FOUND (R4 stub, R5 진입 시 Redis 채움)")
    void location_returns404() throws Exception {
        long riderNo = uniqueUserNo();

        mockMvc.perform(get("/internal/rider/" + riderNo + "/location"))
                .andExpect(status().isNotFound());
    }
}
