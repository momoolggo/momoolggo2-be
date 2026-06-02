package com.green.mmg.rider.settlement;

import com.green.mmg.rider.delivery.DeliveryRepository;
import com.green.mmg.rider.delivery.model.Delivery;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import com.green.mmg.rider.rider.RiderRepository;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.VehicleType;
import com.green.mmg.rider.settlement.dto.AccountReq;
import com.green.mmg.rider.settlement.dto.AccountRes;
import com.green.mmg.rider.settlement.model.Settlement;
import com.green.mmg.rider.settlement.model.SettlementStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R7 SettlementService 통합 — 실 학원 DB.
 *
 * <p>R3-c / R9 패턴 일관 ({@code @SpringBootTest + @Transactional + @Rollback + fixture INSERT +
 * native query 박제}).</p>
 *
 * <p>2건: recalculateThisWeek 산출 공식 end-to-end (SSE 자동화 트랙, 2026-05-21) + 계좌 변경 영속.</p>
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("SettlementService 통합 (실 학원 DB)")
class SettlementIntegrationTest {

    @Autowired private SettlementService settlementService;
    @Autowired private SettlementRepository settlementRepository;
    @Autowired private DeliveryRepository deliveryRepository;
    @Autowired private RiderRepository riderRepository;
    @Autowired private EntityManager em;

    private long uniqueUserNo() {
        return Math.abs(System.nanoTime() + ThreadLocalRandom.current().nextLong(1, 10_000));
    }

    private Rider seedRider() {
        Rider rider = new Rider(
                uniqueUserNo(),
                "12-34-" + UUID.randomUUID().toString().substring(0, 6) + "-12",
                "2종보통", VehicleType.MOTORBIKE,
                "국민", "110-987-654321", "홍길동");
        // 2026-05-28 트랙 — 생성자 status=PENDING 복원. 본 테스트는 ACTIVE 라이더 시나리오 박제.
        rider.approve();
        return riderRepository.saveAndFlush(rider);
    }

    private Delivery seedDeliveredOf(Long riderNo, int baseFee, LocalDateTime deliveredAt) {
        String deliveryNo = "ST" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Long orderId = System.nanoTime();
        Delivery delivery = new Delivery(
                deliveryNo, orderId,
                "010-1111-1111", "010-2222-2222",
                "가게", 37.5665, 126.9780,        // 서울
                "손님", 37.5700, 126.9800,        // 약 400m
                baseFee, 0);
        delivery.changeStatus(DeliveryStatus.DELIVERED, deliveredAt);
        Delivery saved = deliveryRepository.saveAndFlush(delivery);
        // rider_no + delivered_at 박제 (R3-c 패턴)
        em.createNativeQuery("UPDATE delivery SET rider_no = :riderNo, delivered_at = :deliveredAt WHERE delivery_no = :deliveryNo")
                .setParameter("riderNo", riderNo)
                .setParameter("deliveredAt", deliveredAt)
                .setParameter("deliveryNo", saved.getDeliveryNo())
                .executeUpdate();
        em.flush();
        em.clear();
        return deliveryRepository.findById(saved.getDeliveryNo()).orElseThrow();
    }

    @Test
    @DisplayName("recalculateThisWeek end-to-end: 50000원 × 2건 → payout 82030원 + PENDING 재집계 검증")
    void recalculateThisWeek_endToEnd_realDb() {
        Rider rider = seedRider();
        LocalDate periodStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate periodEnd = periodStart.plusDays(6);
        LocalDateTime withinPeriod = periodStart.plusDays(3).atTime(12, 0);
        seedDeliveredOf(rider.getRiderNo(), 50000, withinPeriod);
        seedDeliveredOf(rider.getRiderNo(), 50000, withinPeriod);
        em.flush();
        em.clear();

        Settlement saved = settlementService.recalculateThisWeek(rider.getRiderNo());

        assertThat(saved.getRiderNo()).isEqualTo(rider.getRiderNo());
        assertThat(saved.getPeriodStart()).isEqualTo(periodStart);
        assertThat(saved.getPeriodEnd()).isEqualTo(periodEnd);
        assertThat(saved.getDeliveryCount()).isEqualTo(2);
        assertThat(saved.getTotalBaseFee()).isEqualTo(100000);
        assertThat(saved.getCommission()).isEqualTo(10000);
        assertThat(saved.getTax()).isEqualTo(2970);
        assertThat(saved.getInsurance()).isEqualTo(5000);
        assertThat(saved.getPayout()).isEqualTo(82030);
        assertThat(saved.getStatus()).isEqualTo(SettlementStatus.PENDING);
        // 거리 = 서울 좌표 약 400m × 2건 (Haversine)
        assertThat(saved.getTotalDistanceM()).isBetween(700, 900);

        // PENDING 재집계: 배달 1건 추가 → 재호출 시 동일 settlementNo (UPDATE) + deliveryCount 반영
        seedDeliveredOf(rider.getRiderNo(), 30000, withinPeriod);
        em.flush();
        em.clear();
        Settlement repeat = settlementService.recalculateThisWeek(rider.getRiderNo());
        assertThat(repeat.getSettlementNo()).isEqualTo(saved.getSettlementNo());
        assertThat(repeat.getDeliveryCount()).isEqualTo(3);  // PENDING UPSERT (recalculate)
        // gross=50000+50000+30000=130000 / commission=13000 / tax=(130000-13000)*0.033=3861 / payout=130000-13000-3861-5000=108139
        assertThat(repeat.getTotalBaseFee()).isEqualTo(130000);
        assertThat(repeat.getPayout()).isEqualTo(108139);
    }

    @Test
    @DisplayName("updateAccount: Rider entity 영속 (자유 변경, Q-AccountChange (가))")
    void updateAccount_persists() {
        Rider rider = seedRider();
        em.flush();
        em.clear();

        AccountRes res = settlementService.updateAccount(rider.getUserNo(),
                new AccountReq("신한", "777-888-999", "이몽룡"));
        em.flush();
        em.clear();

        Rider reloaded = riderRepository.findById(rider.getRiderNo()).orElseThrow();
        assertThat(reloaded.getAccountBank()).isEqualTo("신한");
        assertThat(reloaded.getAccountNo()).isEqualTo("777-888-999");
        assertThat(reloaded.getAccountHolder()).isEqualTo("이몽룡");
        assertThat(res.accountBank()).isEqualTo("신한");
    }
}
