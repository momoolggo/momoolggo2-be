package com.green.mmg.rider.settlement.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Settlement entity 단위 테스트 (JPA / Repository / DB 의존 0).
 *
 * <p>R1-A RiderServiceTest / R2-a DeliveryTest / R2-c WorkSessionTest Mockito 패턴 일관 —
 * 학원 DB 의존성 회피 (Q-R2a6-Test (iii)). R7 SettlementService 진입 시 통합 테스트 추가.</p>
 *
 * <p>검증 본질: 명시 생성자 결과 (필수 필드 + status PENDING 고정 + confirmed/paid 필드 null +
 * 합산 필드 매핑 정확).</p>
 */
@DisplayName("Settlement entity 단위")
class SettlementTest {

    @Test
    @DisplayName("생성자: 필수 필드 + status PENDING 고정 + confirmed/paid 필드 null")
    void constructor_setsRequiredFields_andDefaults() {
        LocalDate start = LocalDate.of(2026, 5, 5);
        LocalDate end = LocalDate.of(2026, 5, 11);

        Settlement settlement = new Settlement(
                1L, start, end,
                15, 8500,
                60000, 22500,
                4000, 2730, 700, 75070
        );

        assertThat(settlement.getRiderNo()).isEqualTo(1L);
        assertThat(settlement.getPeriodStart()).isEqualTo(start);
        assertThat(settlement.getPeriodEnd()).isEqualTo(end);
        assertThat(settlement.getDeliveryCount()).isEqualTo(15);
        assertThat(settlement.getTotalDistanceM()).isEqualTo(8500);
        assertThat(settlement.getTotalBaseFee()).isEqualTo(60000);
        assertThat(settlement.getTotalExtraFee()).isEqualTo(22500);
        assertThat(settlement.getCommission()).isEqualTo(4000);
        assertThat(settlement.getTax()).isEqualTo(2730);
        assertThat(settlement.getInsurance()).isEqualTo(700);
        assertThat(settlement.getPayout()).isEqualTo(75070);
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PENDING);
        assertThat(settlement.getConfirmedByAdminNo()).isNull();
        assertThat(settlement.getConfirmedAt()).isNull();
        assertThat(settlement.getPaidAt()).isNull();
        assertThat(settlement.getSettlementNo()).isNull();
    }

    @Test
    @DisplayName("생성자: 0 가능 합산 필드 (delivery_count/total_distance_m 등) 0 set 시 동작")
    void constructor_zeroAggregates_allowed() {
        LocalDate start = LocalDate.of(2026, 5, 5);
        LocalDate end = LocalDate.of(2026, 5, 11);

        Settlement settlement = new Settlement(
                2L, start, end,
                0, 0, 0, 0, 0, 0, 0, 0
        );

        assertThat(settlement.getDeliveryCount()).isEqualTo(0);
        assertThat(settlement.getTotalDistanceM()).isEqualTo(0);
        assertThat(settlement.getTotalBaseFee()).isEqualTo(0);
        assertThat(settlement.getPayout()).isEqualTo(0);
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    @Test
    @DisplayName("생성자: period_start/period_end LocalDate 정확 매핑 (호출자 제어)")
    void constructor_periodDates_callerControlled() {
        LocalDate start1 = LocalDate.of(2026, 1, 5);
        LocalDate end1 = LocalDate.of(2026, 1, 11);
        LocalDate start2 = LocalDate.of(2026, 12, 28);
        LocalDate end2 = LocalDate.of(2027, 1, 3);

        Settlement settlement1 = new Settlement(3L, start1, end1, 1, 100, 3000, 0, 200, 100, 50, 2650);
        Settlement settlement2 = new Settlement(4L, start2, end2, 1, 100, 3000, 0, 200, 100, 50, 2650);

        assertThat(settlement1.getPeriodStart()).isEqualTo(start1);
        assertThat(settlement1.getPeriodEnd()).isEqualTo(end1);
        assertThat(settlement2.getPeriodStart()).isEqualTo(start2);
        assertThat(settlement2.getPeriodEnd()).isEqualTo(end2);
    }
}
