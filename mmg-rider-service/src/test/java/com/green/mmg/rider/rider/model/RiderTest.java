package com.green.mmg.rider.rider.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rider entity 단위 테스트 (R8-1 신설).
 *
 * <p>R2 entity 단위 테스트 패턴 일관 (DeliveryTest/WorkSessionTest) — Mockito 의존 0, DB 의존 0.
 * SSE 자동화 트랙(2026-05-21) — 라이더 신원 승인/제재 흐름 영구 폐기로 approve/suspend 메서드 테스트 삭제.
 * 생성자 시점 status=ACTIVE 박제 + EATING 토글 메서드 보존.</p>
 */
@DisplayName("Rider entity 단위")
class RiderTest {

    @Test
    @DisplayName("생성자(7): 필수 필드 + status ACTIVE 직접 박제 + phone NULL (위임 생성자)")
    void constructor_setsRequiredFields_andStatusActive() {
        Rider rider = new Rider(1L, "12-34-567890-12", "1종보통",
                VehicleType.MOTORBIKE, "국민", "123456-78-901234", "홍길동");

        assertThat(rider.getUserNo()).isEqualTo(1L);
        assertThat(rider.getLicenseNo()).isEqualTo("12-34-567890-12");
        assertThat(rider.getLicenseType()).isEqualTo("1종보통");
        assertThat(rider.getVehicleType()).isEqualTo(VehicleType.MOTORBIKE);
        assertThat(rider.getAccountBank()).isEqualTo("국민");
        assertThat(rider.getAccountNo()).isEqualTo("123456-78-901234");
        assertThat(rider.getAccountHolder()).isEqualTo("홍길동");
        assertThat(rider.getStatus()).isEqualTo(RiderStatus.ACTIVE);
        assertThat(rider.getPhone()).isNull();
    }

    @Test
    @DisplayName("생성자(8): phone 박제 (정산 시연 UX 트랙 #9, 2026-05-21 옵션 A) + status ACTIVE")
    void constructor_8params_setsPhone() {
        Rider rider = new Rider(1L, "12-34-567890-12", "1종보통",
                VehicleType.MOTORBIKE, "국민", "123456-78-901234", "홍길동",
                "010-9999-8888");

        assertThat(rider.getPhone()).isEqualTo("010-9999-8888");
        assertThat(rider.getStatus()).isEqualTo(RiderStatus.ACTIVE);
        assertThat(rider.getUserNo()).isEqualTo(1L);
    }

    @Test
    @DisplayName("toggleEating(): ACTIVE → EATING")
    void toggleEating_activeToEating() {
        Rider rider = newRider();

        rider.toggleEating();

        assertThat(rider.getStatus()).isEqualTo(RiderStatus.EATING);
    }

    @Test
    @DisplayName("resumeActive(): EATING → ACTIVE")
    void resumeActive_eatingToActive() {
        Rider rider = newRider();
        rider.toggleEating();

        rider.resumeActive();

        assertThat(rider.getStatus()).isEqualTo(RiderStatus.ACTIVE);
    }

    private Rider newRider() {
        return new Rider(1L, "LICENSE", "1종보통",
                VehicleType.MOTORBIKE, "국민", "ACCOUNT", "홍길동");
    }
}
