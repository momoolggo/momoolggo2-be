package com.green.mmg.rider.settlement;

import com.green.mmg.rider.delivery.DeliveryRepository;
import com.green.mmg.rider.delivery.model.Delivery;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import com.green.mmg.rider.rider.RiderRepository;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.VehicleType;
import com.green.mmg.rider.settlement.dto.AccountReq;
import com.green.mmg.rider.settlement.dto.AccountRes;
import com.green.mmg.rider.settlement.dto.SettlementRowRes;
import com.green.mmg.rider.settlement.model.Settlement;
import com.green.mmg.rider.settlement.model.SettlementStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R7 SettlementService 통합 — 실 학원 DB.
 *
 * <p>R3-c / R9 패턴 일관 ({@code @SpringBootTest + @Transactional + @Rollback + fixture INSERT +
 * native query 박제}).</p>
 *
 * <p>2건: calculate 산출 공식 end-to-end + 계좌 변경 영속.</p>
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
        rider.approve();
        return riderRepository.saveAndFlush(rider);
    }

    private Delivery seedDeliveredOf(Long riderNo, int baseFee, LocalDateTime deliveredAt) {
        String deliveryNo = "ST" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String orderId = "OR" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Delivery delivery = new Delivery(
                deliveryNo, orderId,
                "010-1111-1111", "010-2222-2222",
                "가게", 37.5665, 126.9780,        // 서울
                "손님", 37.5700, 126.9800,        // 약 400m
                baseFee);
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
    @DisplayName("calculate end-to-end: 50000원 × 2건 → payout 82030원 + DELIVERED 박제 행 합산")
    void calculate_endToEnd_realDb() {
        Rider rider = seedRider();
        LocalDate periodStart = LocalDate.of(2026, 5, 4);
        LocalDate periodEnd = LocalDate.of(2026, 5, 10);
        LocalDateTime withinPeriod = LocalDateTime.of(2026, 5, 7, 12, 0);
        seedDeliveredOf(rider.getRiderNo(), 50000, withinPeriod);
        seedDeliveredOf(rider.getRiderNo(), 50000, withinPeriod);
        em.flush();
        em.clear();

        List<SettlementRowRes> result = settlementService.calculate(periodStart, periodEnd);

        SettlementRowRes mine = result.stream()
                .filter(r -> {
                    Settlement s = settlementRepository.findById(r.settlementNo()).orElseThrow();
                    return s.getRiderNo().equals(rider.getRiderNo());
                })
                .findFirst().orElseThrow();

        assertThat(mine.deliveryCount()).isEqualTo(2);
        assertThat(mine.totalBaseFee()).isEqualTo(100000);
        assertThat(mine.commission()).isEqualTo(10000);
        assertThat(mine.tax()).isEqualTo(2970);
        assertThat(mine.insurance()).isEqualTo(5000);
        assertThat(mine.payout()).isEqualTo(82030);
        assertThat(mine.status()).isEqualTo(SettlementStatus.PENDING);
        // 거리 = 서울 좌표 약 400m × 2건 (Haversine)
        assertThat(mine.totalDistanceM()).isBetween(700, 900);

        // 멱등 재호출: 동일 주 다시 calculate → 동일 settlementNo 반환
        List<SettlementRowRes> repeat = settlementService.calculate(periodStart, periodEnd);
        SettlementRowRes mineRepeat = repeat.stream()
                .filter(r -> r.settlementNo().equals(mine.settlementNo()))
                .findFirst().orElseThrow();
        assertThat(mineRepeat.payout()).isEqualTo(82030);  // 새 INSERT X
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
