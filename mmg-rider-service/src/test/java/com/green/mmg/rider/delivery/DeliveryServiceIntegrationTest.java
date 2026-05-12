package com.green.mmg.rider.delivery;

import com.green.mmg.rider.delivery.model.ActorRole;
import com.green.mmg.rider.delivery.model.Delivery;
import com.green.mmg.rider.delivery.model.DeliveryLog;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import com.green.mmg.rider.rider.RiderRepository;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.VehicleType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 5-R3-c: DeliveryService 통합 테스트 — 실 학원 DB + JPA INSERT/UPDATE + log INSERT 검증.
 *
 * <p>Phase 4-A {@code InternalUserControllerIntegrationTest} 정착 패턴 일관
 * ({@code @SpringBootTest + @Transactional + @Rollback + fixture INSERT + em.clear},
 * {@code feedback_integration_test_setup.md}). R3-b DeliveryServiceTest 23건은 Mockito mock 검증,
 * 본 통합 3건은 실 DB 라운드트립 검증으로 보강.</p>
 *
 * <p>orders.delivery_state 매핑 그룹 대표 3건 (ADR-004 line 86-96 매핑 표):
 * <ul>
 *   <li>WAITING_ASSIGN → ASSIGNED — delivery_state 1 (배달전) 그룹 내 전환</li>
 *   <li>AWAITING_PICKUP → PICKED_UP — delivery_state 1→2 (배달전→픽업완료) 전환점</li>
 *   <li>DELIVERING → DELIVERED — delivery_state 2→3 (픽업완료→배달완료) 전환점</li>
 * </ul></p>
 *
 * <p>Feign 의존 0 (#22 정정 — DeliveryService 내부 Feign 호출 0, ADR-004 line 144-148 박제 일관).
 * R4 진입 시 MainInternalClient 신설 + 외부 호출 통합.</p>
 *
 * <p>학원 DB 의존 (Q-DB (가) 일관). @Rollback 자동 롤백으로 데이터 변경 0 보장.</p>
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("DeliveryService 통합 (실 학원 DB)")
class DeliveryServiceIntegrationTest {

    @Autowired private DeliveryService deliveryService;
    @Autowired private DeliveryRepository deliveryRepository;
    @Autowired private DeliveryLogRepository deliveryLogRepository;
    @Autowired private RiderRepository riderRepository;
    @Autowired private EntityManager em;

    /** 학원 공유 DB user_no unique 충돌 회피용 — nano + random 조합. */
    private long uniqueUserNo() {
        return Math.abs(System.nanoTime() + ThreadLocalRandom.current().nextLong(1, 10_000));
    }

    private Rider seedRider() {
        Rider rider = new Rider(
                uniqueUserNo(),
                "11-22-" + UUID.randomUUID().toString().substring(0, 6) + "-44",
                "2종보통",
                VehicleType.MOTORBIKE,
                "신한은행",
                "110-123-456789",
                "홍길동");
        rider.approve();
        return riderRepository.saveAndFlush(rider);
    }

    @Test
    @DisplayName("R9 배달내역 — DELIVERED 본인 행 + 기간 필터 + 합계 검증 (실 학원 DB)")
    void completedDeliveries_filtersAndAggregates() {
        Rider rider = seedRider();
        // 본인 DELIVERED 2건 + 본인 진행중 1건 + 타인 DELIVERED 1건
        Delivery mine1 = seedDelivery(DeliveryStatus.DELIVERED, rider.getRiderNo());
        Delivery mine2 = seedDelivery(DeliveryStatus.DELIVERED, rider.getRiderNo());
        seedDelivery(DeliveryStatus.DELIVERING, rider.getRiderNo());
        Rider other = seedRider();
        seedDelivery(DeliveryStatus.DELIVERED, other.getRiderNo());

        // seedDelivery의 changeStatus(DELIVERED)는 status만 set — deliveredAt은 markDelivered 호출 또는
        // native query로 박제 필요. 통합 fixture 단계 직접 박제 (R3-c rider_no native query 패턴 일관).
        LocalDateTime now = LocalDateTime.now();
        em.createNativeQuery("UPDATE delivery SET delivered_at = :now WHERE rider_no = :riderNo AND status = 'DELIVERED'")
                .setParameter("now", now)
                .setParameter("riderNo", rider.getRiderNo())
                .executeUpdate();
        em.flush();
        em.clear();

        com.green.mmg.rider.delivery.dto.DeliveryHistoryRes res =
                deliveryService.getMyCompletedDeliveries(rider.getUserNo(), null, null);

        // 본인 DELIVERED 2건만 (DELIVERING 제외 + 타인 제외)
        assertThat(res.rows()).extracting(r -> r.deliveryNo())
                .containsExactlyInAnyOrder(mine1.getDeliveryNo(), mine2.getDeliveryNo());
        assertThat(res.totalCount()).isEqualTo(2);
        int expectedFee = res.rows().stream().mapToInt(r -> r.totalFee()).sum();
        assertThat(res.totalFee()).isEqualTo(expectedFee);
        assertThat(res.totalFee()).isEqualTo(3000 * 2);  // base 3000 × 2, extra 0
    }

    private Delivery seedDelivery(DeliveryStatus initialStatus, Long riderNo) {
        String deliveryNo = "IT" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String orderId = "OR" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Delivery delivery = new Delivery(
                deliveryNo, orderId,
                "010-1111-1111", "010-2222-2222",
                "가게 주소", 35.1234567890123, 128.4567890123456,
                "손님 주소", 35.1300000000001, 128.4600000000001,
                3000);
        if (initialStatus != DeliveryStatus.WAITING_ASSIGN) {
            delivery.changeStatus(initialStatus, LocalDateTime.now());
        }
        Delivery saved = deliveryRepository.saveAndFlush(delivery);
        if (riderNo != null) {
            // riderNo 박제 — entity setter 0 정착 패턴(R4 진입 시 assignRider 메서드 추가 예정)이라
            // R3-c 통합 테스트는 native query로 fixture 단계 직접 박제.
            em.createNativeQuery("UPDATE delivery SET rider_no = :riderNo WHERE delivery_no = :deliveryNo")
                    .setParameter("riderNo", riderNo)
                    .setParameter("deliveryNo", saved.getDeliveryNo())
                    .executeUpdate();
            em.flush();
            em.clear();
            return deliveryRepository.findById(saved.getDeliveryNo()).orElseThrow();
        }
        return saved;
    }

    private List<DeliveryLog> findLogsOf(String deliveryNo) {
        return deliveryLogRepository.findAll().stream()
                .filter(log -> deliveryNo.equals(log.getDeliveryNo()))
                .toList();
    }

    @Test
    @DisplayName("WAITING_ASSIGN → ASSIGNED (delivery_state 1 그룹) — 실 DB UPDATE + assigned_at + log INSERT 박제")
    void waitingAssign_to_assigned_realDb() {
        Rider rider = seedRider();
        Delivery delivery = seedDelivery(DeliveryStatus.WAITING_ASSIGN, rider.getRiderNo());

        deliveryService.updateStatus(
                delivery.getDeliveryNo(), DeliveryStatus.ASSIGNED,
                rider.getUserNo(), ActorRole.RIDER);

        em.flush();
        em.clear();

        Delivery reloaded = deliveryRepository.findById(delivery.getDeliveryNo()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(reloaded.getAssignedAt()).isNotNull();
        assertThat(reloaded.getRiderNo()).isEqualTo(rider.getRiderNo());

        List<DeliveryLog> logs = findLogsOf(delivery.getDeliveryNo());
        assertThat(logs).hasSize(1);
        DeliveryLog log = logs.get(0);
        assertThat(log.getFromStatus()).isEqualTo(DeliveryStatus.WAITING_ASSIGN);
        assertThat(log.getToStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(log.getActorRole()).isEqualTo(ActorRole.RIDER);
        assertThat(log.getActorUserNo()).isEqualTo(rider.getUserNo());
        assertThat(log.getChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("AWAITING_PICKUP → PICKED_UP (delivery_state 1→2 전환점) — 실 DB UPDATE + picked_at + log INSERT 박제")
    void awaitingPickup_to_pickedUp_realDb() {
        Rider rider = seedRider();
        Delivery delivery = seedDelivery(DeliveryStatus.AWAITING_PICKUP, rider.getRiderNo());

        deliveryService.updateStatus(
                delivery.getDeliveryNo(), DeliveryStatus.PICKED_UP,
                rider.getUserNo(), ActorRole.RIDER);

        em.flush();
        em.clear();

        Delivery reloaded = deliveryRepository.findById(delivery.getDeliveryNo()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.PICKED_UP);
        assertThat(reloaded.getPickedAt()).isNotNull();

        List<DeliveryLog> logs = findLogsOf(delivery.getDeliveryNo());
        assertThat(logs).hasSize(1);
        DeliveryLog log = logs.get(0);
        assertThat(log.getFromStatus()).isEqualTo(DeliveryStatus.AWAITING_PICKUP);
        assertThat(log.getToStatus()).isEqualTo(DeliveryStatus.PICKED_UP);
        assertThat(log.getActorRole()).isEqualTo(ActorRole.RIDER);
        assertThat(log.getActorUserNo()).isEqualTo(rider.getUserNo());
        assertThat(log.getChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("DELIVERING → DELIVERED (delivery_state 2→3 전환점, terminal) — 실 DB UPDATE + delivered_at + log INSERT 박제")
    void delivering_to_delivered_realDb() {
        Rider rider = seedRider();
        Delivery delivery = seedDelivery(DeliveryStatus.DELIVERING, rider.getRiderNo());

        deliveryService.updateStatus(
                delivery.getDeliveryNo(), DeliveryStatus.DELIVERED,
                rider.getUserNo(), ActorRole.RIDER);

        em.flush();
        em.clear();

        Delivery reloaded = deliveryRepository.findById(delivery.getDeliveryNo()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(reloaded.getDeliveredAt()).isNotNull();

        List<DeliveryLog> logs = findLogsOf(delivery.getDeliveryNo());
        assertThat(logs).hasSize(1);
        DeliveryLog log = logs.get(0);
        assertThat(log.getFromStatus()).isEqualTo(DeliveryStatus.DELIVERING);
        assertThat(log.getToStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(log.getActorRole()).isEqualTo(ActorRole.RIDER);
        assertThat(log.getActorUserNo()).isEqualTo(rider.getUserNo());
        assertThat(log.getChangedAt()).isNotNull();
    }
}
