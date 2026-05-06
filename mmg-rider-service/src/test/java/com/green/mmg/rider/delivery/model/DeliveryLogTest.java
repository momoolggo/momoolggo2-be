package com.green.mmg.rider.delivery.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DeliveryLog entity 단위 테스트 (JPA / Repository / DB 의존 0).
 *
 * <p>R1-A RiderServiceTest Mockito 패턴 + R2-a DeliveryTest 일관 — 학원 DB 의존성 회피 (Q-DB (다)).
 * 비즈니스 메서드 0 (이력 본질) → 명시 생성자 결과 검증만.</p>
 */
@DisplayName("DeliveryLog entity 단위")
class DeliveryLogTest {

    @Test
    @DisplayName("생성자: 필수 필드 매핑 (RIDER 액터 + 모든 상태 enum 매핑)")
    void constructor_setsRequiredFields() {
        DeliveryLog log = new DeliveryLog(
                "00001ABC",
                DeliveryStatus.WAITING_ASSIGN,
                DeliveryStatus.ASSIGNED,
                "RIDER",
                42L
        );

        assertThat(log.getDeliveryNo()).isEqualTo("00001ABC");
        assertThat(log.getFromStatus()).isEqualTo(DeliveryStatus.WAITING_ASSIGN);
        assertThat(log.getToStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(log.getActorRole()).isEqualTo("RIDER");
        assertThat(log.getActorUserNo()).isEqualTo(42L);
    }

    @Test
    @DisplayName("생성자: 최초 INSERT 시 fromStatus null + actorUserNo null (SYSTEM 액터) 허용")
    void constructor_initialInsert_allowsNullFromStatusAndActorUserNo() {
        DeliveryLog log = new DeliveryLog(
                "00002ABC",
                null,
                DeliveryStatus.WAITING_ASSIGN,
                "SYSTEM",
                null
        );

        assertThat(log.getFromStatus()).isNull();
        assertThat(log.getActorUserNo()).isNull();
        assertThat(log.getToStatus()).isEqualTo(DeliveryStatus.WAITING_ASSIGN);
        assertThat(log.getActorRole()).isEqualTo("SYSTEM");
        assertThat(log.getDeliveryNo()).isEqualTo("00002ABC");
    }

    @Test
    @DisplayName("생성자: DeliveryStatus enum 재사용 — 7개 상태 전이 4건 매핑 + ADMIN 액터")
    void constructor_deliveryStatusEnumReuse_variousTransitions() {
        DeliveryLog log1 = new DeliveryLog(
                "00003ABC", DeliveryStatus.ARRIVED_AT_STORE, DeliveryStatus.AWAITING_PICKUP, "RIDER", 42L);
        DeliveryLog log2 = new DeliveryLog(
                "00003ABC", DeliveryStatus.AWAITING_PICKUP, DeliveryStatus.PICKED_UP, "RIDER", 42L);
        DeliveryLog log3 = new DeliveryLog(
                "00003ABC", DeliveryStatus.PICKED_UP, DeliveryStatus.DELIVERING, "RIDER", 42L);
        DeliveryLog log4 = new DeliveryLog(
                "00003ABC", DeliveryStatus.DELIVERING, DeliveryStatus.DELIVERED, "ADMIN", 1L);

        assertThat(log1.getFromStatus()).isEqualTo(DeliveryStatus.ARRIVED_AT_STORE);
        assertThat(log1.getToStatus()).isEqualTo(DeliveryStatus.AWAITING_PICKUP);
        assertThat(log2.getToStatus()).isEqualTo(DeliveryStatus.PICKED_UP);
        assertThat(log3.getToStatus()).isEqualTo(DeliveryStatus.DELIVERING);
        assertThat(log4.getToStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(log4.getActorRole()).isEqualTo("ADMIN");
        assertThat(log4.getActorUserNo()).isEqualTo(1L);
    }
}
