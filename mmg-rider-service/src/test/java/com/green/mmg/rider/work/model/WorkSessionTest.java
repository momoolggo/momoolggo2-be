package com.green.mmg.rider.work.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WorkSession entity 단위 테스트 (JPA / Repository / DB 의존 0).
 *
 * <p>R1-A RiderServiceTest / R2-a DeliveryTest Mockito 패턴 일관 — 학원 DB 의존성 회피 (Q-R2a6-Test (iii)).
 * R3 WorkSessionService 진입 시 통합 테스트 추가.</p>
 *
 * <p>검증 본질: 명시 생성자 결과 (rider_no/vehicle_type/started_at 박제 + ended_at null +
 * work_seconds/break_seconds 0 고정 — R2-a Delivery extra_fee 패턴 일관).</p>
 */
@DisplayName("WorkSession entity 단위")
class WorkSessionTest {

    @Test
    @DisplayName("생성자: 필수 필드 + ended_at null + work/break_seconds 0 고정")
    void constructor_setsRequiredFields_andDefaults() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 5, 7, 9, 0, 0);

        WorkSession session = new WorkSession(1L, "MOTORBIKE", startedAt);

        assertThat(session.getRiderNo()).isEqualTo(1L);
        assertThat(session.getVehicleType()).isEqualTo("MOTORBIKE");
        assertThat(session.getStartedAt()).isEqualTo(startedAt);
        assertThat(session.getEndedAt()).isNull();
        assertThat(session.getWorkSeconds()).isEqualTo(0);
        assertThat(session.getBreakSeconds()).isEqualTo(0);
        assertThat(session.getSessionNo()).isNull();
    }

    @Test
    @DisplayName("생성자: vehicleType 화이트리스트 4종(WALK/BICYCLE/MOTORBIKE/CAR) snapshot 보존")
    void constructor_vehicleTypeSnapshot_preserved() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 5, 7, 10, 30, 15);

        WorkSession walk = new WorkSession(2L, "WALK", startedAt);
        WorkSession bicycle = new WorkSession(3L, "BICYCLE", startedAt);
        WorkSession car = new WorkSession(4L, "CAR", startedAt);

        assertThat(walk.getVehicleType()).isEqualTo("WALK");
        assertThat(bicycle.getVehicleType()).isEqualTo("BICYCLE");
        assertThat(car.getVehicleType()).isEqualTo("CAR");
    }

    @Test
    @DisplayName("생성자: started_at 호출자 제어 (DB CURRENT_TIMESTAMP X)")
    void constructor_startedAt_callerControlled() {
        LocalDateTime past = LocalDateTime.of(2026, 5, 1, 8, 0, 0);
        LocalDateTime future = LocalDateTime.of(2026, 12, 31, 23, 59, 59);

        WorkSession sessionPast = new WorkSession(5L, "MOTORBIKE", past);
        WorkSession sessionFuture = new WorkSession(6L, "MOTORBIKE", future);

        assertThat(sessionPast.getStartedAt()).isEqualTo(past);
        assertThat(sessionFuture.getStartedAt()).isEqualTo(future);
    }
}
