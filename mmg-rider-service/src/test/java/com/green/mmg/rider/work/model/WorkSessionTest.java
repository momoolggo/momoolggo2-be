package com.green.mmg.rider.work.model;

import com.green.mmg.rider.rider.model.VehicleType;
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

        WorkSession session = new WorkSession(1L, VehicleType.MOTORBIKE, startedAt);

        assertThat(session.getRiderNo()).isEqualTo(1L);
        assertThat(session.getVehicleType()).isEqualTo(VehicleType.MOTORBIKE);
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

        WorkSession walk = new WorkSession(2L, VehicleType.WALK, startedAt);
        WorkSession bicycle = new WorkSession(3L, VehicleType.BICYCLE, startedAt);
        WorkSession car = new WorkSession(4L, VehicleType.CAR, startedAt);

        assertThat(walk.getVehicleType()).isEqualTo(VehicleType.WALK);
        assertThat(bicycle.getVehicleType()).isEqualTo(VehicleType.BICYCLE);
        assertThat(car.getVehicleType()).isEqualTo(VehicleType.CAR);
    }

    @Test
    @DisplayName("생성자: started_at 호출자 제어 (DB CURRENT_TIMESTAMP X)")
    void constructor_startedAt_callerControlled() {
        LocalDateTime past = LocalDateTime.of(2026, 5, 1, 8, 0, 0);
        LocalDateTime future = LocalDateTime.of(2026, 12, 31, 23, 59, 59);

        WorkSession sessionPast = new WorkSession(5L, VehicleType.MOTORBIKE, past);
        WorkSession sessionFuture = new WorkSession(6L, VehicleType.MOTORBIKE, future);

        assertThat(sessionPast.getStartedAt()).isEqualTo(past);
        assertThat(sessionFuture.getStartedAt()).isEqualTo(future);
    }

    @Test
    @DisplayName("addBreak: 누적 (0 → 600 → 900)")
    void addBreak_accumulates() {
        WorkSession session = new WorkSession(1L, VehicleType.MOTORBIKE,
                LocalDateTime.of(2026, 5, 7, 9, 0, 0));

        session.addBreak(600);
        assertThat(session.getBreakSeconds()).isEqualTo(600);

        session.addBreak(300);
        assertThat(session.getBreakSeconds()).isEqualTo(900);
    }

    @Test
    @DisplayName("addBreak: 음수 거부 (IllegalArgumentException)")
    void addBreak_negativeRejected() {
        WorkSession session = new WorkSession(1L, VehicleType.MOTORBIKE,
                LocalDateTime.of(2026, 5, 7, 9, 0, 0));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> session.addBreak(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(">= 0");
    }

    @Test
    @DisplayName("end: ended_at 기록 + work_seconds 계산 (총 경과 - break)")
    void end_recordsEndedAt_andComputesWorkSeconds() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 5, 7, 9, 0, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 5, 7, 13, 20, 0);  // 4h 20m = 15,600s
        WorkSession session = new WorkSession(1L, VehicleType.MOTORBIKE, startedAt);
        session.addBreak(4200);  // 1h 10m

        session.end(endedAt);

        assertThat(session.getEndedAt()).isEqualTo(endedAt);
        assertThat(session.getWorkSeconds()).isEqualTo(15600 - 4200);  // 11,400s = 3h 10m
    }

    @Test
    @DisplayName("end: break 0이면 work_seconds = 총 경과")
    void end_noBreak_workSecondsEqualsTotal() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 5, 7, 9, 0, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 5, 7, 12, 0, 0);  // 3h = 10,800s
        WorkSession session = new WorkSession(1L, VehicleType.MOTORBIKE, startedAt);

        session.end(endedAt);

        assertThat(session.getWorkSeconds()).isEqualTo(10800);
    }

    @Test
    @DisplayName("end: break가 총 경과 초과 시 work_seconds = 0 (음수 차단)")
    void end_breakExceedsTotal_workSecondsZero() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 5, 7, 9, 0, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 5, 7, 9, 30, 0);  // 30m = 1,800s
        WorkSession session = new WorkSession(1L, VehicleType.MOTORBIKE, startedAt);
        session.addBreak(3600);  // 1h > 30m

        session.end(endedAt);

        assertThat(session.getWorkSeconds()).isEqualTo(0);
    }

    @Test
    @DisplayName("end: null endedAt 거부")
    void end_nullEndedAt_rejected() {
        WorkSession session = new WorkSession(1L, VehicleType.MOTORBIKE,
                LocalDateTime.of(2026, 5, 7, 9, 0, 0));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> session.end(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("end: endedAt < startedAt 거부")
    void end_endedBeforeStarted_rejected() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 5, 7, 9, 0, 0);
        LocalDateTime endedAt = LocalDateTime.of(2026, 5, 7, 8, 0, 0);
        WorkSession session = new WorkSession(1L, VehicleType.MOTORBIKE, startedAt);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> session.end(endedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(">=");
    }
}
