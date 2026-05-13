package com.green.mmg.rider.rider.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rider entity 단위 테스트 (R8-1 신설).
 *
 * <p>R2 entity 단위 테스트 패턴 일관 (DeliveryTest/WorkSessionTest) — Mockito 의존 0, DB 의존 0.
 * R1-A 시점 누락분 보강 + R8-1 토글 메서드 검증.</p>
 */
@DisplayName("Rider entity 단위")
class RiderTest {

    @Test
    @DisplayName("생성자: 필수 필드 + status PENDING 고정")
    void constructor_setsRequiredFields_andStatusPending() {
        Rider rider = new Rider(1L, "12-34-567890-12", "1종보통",
                VehicleType.MOTORBIKE, "국민", "123456-78-901234", "홍길동");

        assertThat(rider.getUserNo()).isEqualTo(1L);
        assertThat(rider.getLicenseNo()).isEqualTo("12-34-567890-12");
        assertThat(rider.getLicenseType()).isEqualTo("1종보통");
        assertThat(rider.getVehicleType()).isEqualTo(VehicleType.MOTORBIKE);
        assertThat(rider.getAccountBank()).isEqualTo("국민");
        assertThat(rider.getAccountNo()).isEqualTo("123456-78-901234");
        assertThat(rider.getAccountHolder()).isEqualTo("홍길동");
        assertThat(rider.getStatus()).isEqualTo(RiderStatus.PENDING);
    }

    @Test
    @DisplayName("approve(): PENDING → ACTIVE")
    void approve_pendingToActive() {
        Rider rider = newRider();
        assertThat(rider.getStatus()).isEqualTo(RiderStatus.PENDING);

        rider.approve();

        assertThat(rider.getStatus()).isEqualTo(RiderStatus.ACTIVE);
    }

    @Test
    @DisplayName("toggleEating(): ACTIVE → EATING")
    void toggleEating_activeToEating() {
        Rider rider = newRider();
        rider.approve();

        rider.toggleEating();

        assertThat(rider.getStatus()).isEqualTo(RiderStatus.EATING);
    }

    @Test
    @DisplayName("resumeActive(): EATING → ACTIVE")
    void resumeActive_eatingToActive() {
        Rider rider = newRider();
        rider.approve();
        rider.toggleEating();

        rider.resumeActive();

        assertThat(rider.getStatus()).isEqualTo(RiderStatus.ACTIVE);
    }

    private Rider newRider() {
        return new Rider(1L, "LICENSE", "1종보통",
                VehicleType.MOTORBIKE, "국민", "ACCOUNT", "홍길동");
    }
}
