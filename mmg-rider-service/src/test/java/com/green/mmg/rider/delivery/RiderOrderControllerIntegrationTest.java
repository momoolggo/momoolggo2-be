package com.green.mmg.rider.delivery;

import com.green.mmg.common.model.JwtUser;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.rider.delivery.model.ActorRole;
import com.green.mmg.rider.delivery.model.Delivery;
import com.green.mmg.rider.delivery.model.DeliveryCancelReason;
import com.green.mmg.rider.delivery.model.DeliveryLog;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import com.green.mmg.rider.feign.MainInternalClient;
import com.green.mmg.rider.feign.dto.DeliveryStatusUpdateReq;
import com.green.mmg.rider.feign.dto.DeliveryStatusUpdateRes;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 5-R6: RiderOrderController 통합 — 실 학원 DB + Feign mock.
 *
 * <p>R4-4 RiderInternalControllerIntegrationTest + R5-3 LocationIntegrationTest 패턴 일관.
 * SecurityContext 직접 주입(JwtUser RIDER role) + UUID 격리 + Main 동기화 verify.</p>
 *
 * <p>7건: waiting / inprogress / accept happy / reject happy(unassign) / complete happy(markDelivered) /
 * forbidden(다른 rider) / invalid transition.</p>
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("RiderOrderController 통합 (R6-3)")
class RiderOrderControllerIntegrationTest {

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
        when(mainInternalClient.updateDeliveryStatus(any(), any()))
                .thenReturn(new DeliveryStatusUpdateRes("ORD", 0, 1));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private long uniqueUserNo() {
        return Math.abs(System.nanoTime() + ThreadLocalRandom.current().nextLong(1, 10_000));
    }

    private String uniqueOrderId() {
        return "OR" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private String uniqueDeliveryNo() {
        return String.format("%05d%s",
                System.nanoTime() % 100_000,
                UUID.randomUUID().toString().replaceAll("[^a-zA-Z]", "")
                        .substring(0, 3).toUpperCase());
    }

    private Rider seedRider(boolean active) {
        Rider rider = new Rider(
                uniqueUserNo(),
                "11-22-" + UUID.randomUUID().toString().substring(0, 6) + "-44",
                "2종보통", VehicleType.MOTORBIKE,
                "신한", "110-1", "홍길동");
        if (active) rider.approve();
        return riderRepository.saveAndFlush(rider);
    }

    private Delivery seedDelivery(Long riderNo, DeliveryStatus status) {
        Delivery d = new Delivery(uniqueDeliveryNo(), uniqueOrderId(),
                "053-1", "010-1", "가게주소", 35.123, 128.456,
                "손님주소", 35.130, 128.460, 4000);
        if (riderNo != null) d.assignRider(riderNo);
        if (status != DeliveryStatus.WAITING_ASSIGN) d.changeStatus(status, LocalDateTime.now());
        return deliveryRepository.saveAndFlush(d);
    }

    private void authenticateAs(long userNo) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userNo, "RIDER", null, "tester"));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_RIDER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/rider/order/waiting ACTIVE: 200 + WAITING_ASSIGN 데이터 포함")
    void waiting_activeRider_returnsList() throws Exception {
        Rider rider = seedRider(true);
        authenticateAs(rider.getUserNo());
        Delivery seeded = seedDelivery(null, DeliveryStatus.WAITING_ASSIGN);

        em.flush();
        em.clear();

        mockMvc.perform(get("/api/rider/order/waiting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultMessage").value("대기 배달 조회 성공"))
                .andExpect(jsonPath("$.resultData[?(@.deliveryNo == '" + seeded.getDeliveryNo() + "')].status")
                        .value("WAITING_ASSIGN"));
    }

    @Test
    @DisplayName("GET /api/rider/order/waiting PENDING: 403 FORBIDDEN (reviewer C-2 정정)")
    void waiting_pendingRider_returns403() throws Exception {
        Rider rider = seedRider(false); // PENDING
        authenticateAs(rider.getUserNo());

        mockMvc.perform(get("/api/rider/order/waiting"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/rider/order/inprogress: 본인 진행 중 배달만 반환")
    void inprogress_returnsMyOnly() throws Exception {
        Rider rider = seedRider(true);
        authenticateAs(rider.getUserNo());
        Delivery mine = seedDelivery(rider.getRiderNo(), DeliveryStatus.PICKED_UP);
        Rider other = seedRider(true);
        seedDelivery(other.getRiderNo(), DeliveryStatus.PICKED_UP);

        em.flush();
        em.clear();

        mockMvc.perform(get("/api/rider/order/inprogress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultData[?(@.deliveryNo == '" + mine.getDeliveryNo() + "')].status")
                        .value("PICKED_UP"));
    }

    @Test
    @DisplayName("PUT /api/rider/order/{deliveryNo}/accept happy: ARRIVED_AT_STORE + log + Main 호출")
    void accept_happy() throws Exception {
        Rider rider = seedRider(true);
        authenticateAs(rider.getUserNo());
        Delivery d = seedDelivery(rider.getRiderNo(), DeliveryStatus.ASSIGNED);

        em.flush();

        mockMvc.perform(put("/api/rider/order/" + d.getDeliveryNo() + "/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultMessage").value("배차 수락 성공"));

        em.flush();
        em.clear();

        Delivery saved = deliveryRepository.findById(d.getDeliveryNo()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.ARRIVED_AT_STORE);
        assertThat(saved.getArrivedAtStoreAt()).isNotNull();

        List<DeliveryLog> logs = deliveryLogRepository.findAll().stream()
                .filter(l -> d.getDeliveryNo().equals(l.getDeliveryNo()))
                .toList();
        assertThat(logs).anyMatch(l -> l.getActorRole() == ActorRole.RIDER
                && l.getToStatus() == DeliveryStatus.ARRIVED_AT_STORE
                && java.util.Objects.equals(l.getActorUserNo(), rider.getUserNo()));

        verify(mainInternalClient).updateDeliveryStatus(eq(d.getOrderId()),
                any(DeliveryStatusUpdateReq.class));
    }

    @Test
    @DisplayName("PUT /reject happy: WAITING_ASSIGN + rider_no NULL + Main 호출 (riderNo snapshot 전달)")
    void reject_happy_unassignsAndNotifiesMain() throws Exception {
        Rider rider = seedRider(true);
        authenticateAs(rider.getUserNo());
        Delivery d = seedDelivery(rider.getRiderNo(), DeliveryStatus.ASSIGNED);

        em.flush();

        mockMvc.perform(put("/api/rider/order/" + d.getDeliveryNo() + "/reject"))
                .andExpect(status().isOk());

        em.flush();
        em.clear();

        Delivery saved = deliveryRepository.findById(d.getDeliveryNo()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.WAITING_ASSIGN);
        assertThat(saved.getRiderNo()).isNull();

        verify(mainInternalClient).updateDeliveryStatus(eq(d.getOrderId()),
                any(DeliveryStatusUpdateReq.class));
    }

    @Test
    @DisplayName("PUT /complete happy: DELIVERED + markDelivered + Main 호출")
    void complete_happy_marksAndNotifies() throws Exception {
        Rider rider = seedRider(true);
        authenticateAs(rider.getUserNo());
        Delivery d = seedDelivery(rider.getRiderNo(), DeliveryStatus.DELIVERING);

        em.flush();

        String body = """
                {"deliveredMethod": "DIRECT", "deliveredPhotoUrl": "/uploads/delivery/x.jpg"}
                """;
        mockMvc.perform(put("/api/rider/order/" + d.getDeliveryNo() + "/complete")
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        em.flush();
        em.clear();

        Delivery saved = deliveryRepository.findById(d.getDeliveryNo()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(saved.getDeliveredMethod()).isEqualTo("DIRECT");
        assertThat(saved.getDeliveredPhotoUrl()).isEqualTo("/uploads/delivery/x.jpg");
        assertThat(saved.getDeliveredAt()).isNotNull();

        verify(mainInternalClient).updateDeliveryStatus(eq(d.getOrderId()),
                any(DeliveryStatusUpdateReq.class));
    }

    @Test
    @DisplayName("PUT /accept 다른 rider 배달: 403 FORBIDDEN + Main 미호출")
    void accept_notOwner_returns403() throws Exception {
        Rider rider = seedRider(true);
        authenticateAs(rider.getUserNo());
        Rider other = seedRider(true);
        Delivery d = seedDelivery(other.getRiderNo(), DeliveryStatus.ASSIGNED);

        em.flush();

        mockMvc.perform(put("/api/rider/order/" + d.getDeliveryNo() + "/accept"))
                .andExpect(status().isForbidden());

        verify(mainInternalClient, never()).updateDeliveryStatus(any(), any());
    }

    @Test
    @DisplayName("PUT /pickup 화이트리스트 위반(ASSIGNED→PICKED_UP): 400 BAD_REQUEST")
    void pickup_invalidTransition_returns400() throws Exception {
        Rider rider = seedRider(true);
        authenticateAs(rider.getUserNo());
        Delivery d = seedDelivery(rider.getRiderNo(), DeliveryStatus.ASSIGNED);

        em.flush();

        mockMvc.perform(put("/api/rider/order/" + d.getDeliveryNo() + "/pickup"))
                .andExpect(status().isBadRequest());

        verify(mainInternalClient, never()).updateDeliveryStatus(any(), any());
    }

    // R6-cancel: POST /api/rider/order/{deliveryNo}/cancel (3건)

    @Test
    @DisplayName("POST /cancel ACCIDENT happy: PICKED_UP → WAITING_ASSIGN + rider_no NULL + log reason 박제 + Main 호출")
    void cancel_pickedUp_accident_happy() throws Exception {
        Rider rider = seedRider(true);
        authenticateAs(rider.getUserNo());
        Delivery d = seedDelivery(rider.getRiderNo(), DeliveryStatus.PICKED_UP);

        em.flush();

        String body = """
                {"reason": "ACCIDENT"}
                """;
        mockMvc.perform(post("/api/rider/order/" + d.getDeliveryNo() + "/cancel")
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultMessage").value("배달 반려 처리 성공"));

        em.flush();
        em.clear();

        Delivery saved = deliveryRepository.findById(d.getDeliveryNo()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.WAITING_ASSIGN);
        assertThat(saved.getRiderNo()).isNull();

        List<DeliveryLog> logs = deliveryLogRepository.findAll().stream()
                .filter(l -> d.getDeliveryNo().equals(l.getDeliveryNo()))
                .toList();
        assertThat(logs).anyMatch(l -> l.getReason() == DeliveryCancelReason.ACCIDENT
                && l.getActorRole() == ActorRole.RIDER
                && l.getToStatus() == DeliveryStatus.WAITING_ASSIGN);

        verify(mainInternalClient).updateDeliveryStatus(eq(d.getOrderId()),
                any(DeliveryStatusUpdateReq.class));
    }

    @Test
    @DisplayName("POST /cancel reason 누락: 400 BAD_REQUEST + Main 미호출 + DB 변경 X")
    void cancel_nullReason_returns400() throws Exception {
        Rider rider = seedRider(true);
        authenticateAs(rider.getUserNo());
        Delivery d = seedDelivery(rider.getRiderNo(), DeliveryStatus.PICKED_UP);

        em.flush();

        String body = """
                {"reason": null}
                """;
        mockMvc.perform(post("/api/rider/order/" + d.getDeliveryNo() + "/cancel")
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(mainInternalClient, never()).updateDeliveryStatus(any(), any());
    }

    @Test
    @DisplayName("POST /cancel WAITING_ASSIGN→WAITING_ASSIGN 위반: 400 BAD_REQUEST")
    void cancel_fromWaitingAssign_returns400() throws Exception {
        Rider rider = seedRider(true);
        authenticateAs(rider.getUserNo());
        Delivery d = seedDelivery(rider.getRiderNo(), DeliveryStatus.WAITING_ASSIGN);

        em.flush();

        String body = """
                {"reason": "OTHER"}
                """;
        mockMvc.perform(post("/api/rider/order/" + d.getDeliveryNo() + "/cancel")
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(mainInternalClient, never()).updateDeliveryStatus(any(), any());
    }
}
