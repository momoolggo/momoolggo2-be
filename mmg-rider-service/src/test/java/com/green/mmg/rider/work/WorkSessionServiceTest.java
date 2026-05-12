package com.green.mmg.rider.work;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.delivery.DeliveryRepository;
import com.green.mmg.rider.delivery.model.Delivery;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import com.green.mmg.rider.rider.RiderRepository;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.RiderStatus;
import com.green.mmg.rider.rider.model.VehicleType;
import com.green.mmg.rider.work.dto.WorkSessionEndRes;
import com.green.mmg.rider.work.dto.WorkSessionStatusRes;
import com.green.mmg.rider.work.dto.WorkSessionSummaryRes;
import com.green.mmg.rider.work.dto.WorkSessionTodayRes;
import com.green.mmg.rider.work.model.WorkSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * WorkSessionService 단위 테스트 — R8-7 (가짜 테스트 0건 원칙, CLAUDE.md §6.5).
 *
 * <p>R3-b DeliveryServiceTest Mockito 패턴 일관 — 학원 DB 의존성 회피.
 * 13건 = toggleStatus 6 + endWorkSession 3 + getSummary 3 + getTodaySession 1.</p>
 */
@ExtendWith(MockitoExtension.class)
class WorkSessionServiceTest {

    private static final Long CALLER_USER_NO = 42L;
    private static final Long CALLER_RIDER_NO = 5L;

    @Mock private RiderRepository riderRepository;
    @Mock private WorkSessionRepository workSessionRepository;
    @Mock private DeliveryRepository deliveryRepository;

    @InjectMocks private WorkSessionService workSessionService;

    private Rider rider;

    @BeforeEach
    void setUp() {
        rider = mock(Rider.class);
        lenient().when(rider.getRiderNo()).thenReturn(CALLER_RIDER_NO);
        lenient().when(rider.getVehicleType()).thenReturn(VehicleType.MOTORBIKE);
    }

    // ─── toggleStatus ──────────────────────────────────────────────

    @Test
    @DisplayName("toggleStatus: ACTIVE → EATING 성공 + rider.toggleEating 호출")
    void toggleStatus_activeToEating() {
        when(rider.getStatus()).thenReturn(RiderStatus.ACTIVE);
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));

        WorkSessionStatusRes res = workSessionService.toggleStatus(CALLER_USER_NO, RiderStatus.EATING);

        verify(rider).toggleEating();
        verify(workSessionRepository, never()).save(any(WorkSession.class));
        assertThat(res.riderNo()).isEqualTo(CALLER_RIDER_NO);
    }

    @Test
    @DisplayName("toggleStatus: EATING → ACTIVE 성공 + 진행 세션 break 누적 시도")
    void toggleStatus_eatingToActive_accumulatesBreak() {
        when(rider.getStatus()).thenReturn(RiderStatus.EATING);
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));
        // EATING 진입 후 ACTIVE 복귀까지 break 메모리 보관 시뮬레이션
        // (실측: toggleEating 호출 후 같은 Service 인스턴스에서 ACTIVE 복귀 시 breakStartedAt remove)
        WorkSession progressingSession = mock(WorkSession.class);
        when(workSessionRepository.findByRiderNoAndEndedAtIsNull(CALLER_RIDER_NO))
                .thenReturn(Optional.of(progressingSession))  // break 누적 조회
                .thenReturn(Optional.of(progressingSession)); // ACTIVE 신규 생성 차단 조회

        // EATING 먼저 진입해서 breakStartedAt 메모리 set
        when(rider.getStatus()).thenReturn(RiderStatus.ACTIVE).thenReturn(RiderStatus.EATING);
        workSessionService.toggleStatus(CALLER_USER_NO, RiderStatus.EATING);

        // EATING → ACTIVE 복귀
        when(rider.getStatus()).thenReturn(RiderStatus.EATING);
        WorkSessionStatusRes res = workSessionService.toggleStatus(CALLER_USER_NO, RiderStatus.ACTIVE);

        verify(rider).resumeActive();
        verify(progressingSession).addBreak(org.mockito.ArgumentMatchers.intThat(s -> s >= 0));
        assertThat(res.riderNo()).isEqualTo(CALLER_RIDER_NO);
    }

    @Test
    @DisplayName("toggleStatus: PENDING → EATING 차단 (CONFLICT)")
    void toggleStatus_pendingToEating_blocked() {
        when(rider.getStatus()).thenReturn(RiderStatus.PENDING);
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));

        assertThatThrownBy(() -> workSessionService.toggleStatus(CALLER_USER_NO, RiderStatus.EATING))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("ACTIVE 상태에서만 EATING");
        verify(rider, never()).toggleEating();
    }

    @Test
    @DisplayName("toggleStatus: SUSPENDED → ACTIVE 차단 (CONFLICT, EATING 상태 아님)")
    void toggleStatus_suspendedToActive_blocked() {
        when(rider.getStatus()).thenReturn(RiderStatus.SUSPENDED);
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));

        assertThatThrownBy(() -> workSessionService.toggleStatus(CALLER_USER_NO, RiderStatus.ACTIVE))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("EATING 상태에서만");
        verify(rider, never()).resumeActive();
    }

    @Test
    @DisplayName("toggleStatus: ACTIVE 진입 시 진행 세션 없으면 신규 work_session 생성")
    void toggleStatus_eatingToActive_createsNewSessionIfNone() {
        // 시나리오: 처음 EATING 상태였다가 ACTIVE 복귀, 진행 세션 없음
        when(rider.getStatus()).thenReturn(RiderStatus.EATING);
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));
        when(workSessionRepository.findByRiderNoAndEndedAtIsNull(CALLER_RIDER_NO))
                .thenReturn(Optional.empty());  // break 누적 조회 (없음)

        workSessionService.toggleStatus(CALLER_USER_NO, RiderStatus.ACTIVE);

        ArgumentCaptor<WorkSession> captor = ArgumentCaptor.forClass(WorkSession.class);
        verify(workSessionRepository).save(captor.capture());
        WorkSession saved = captor.getValue();
        assertThat(saved.getRiderNo()).isEqualTo(CALLER_RIDER_NO);
        assertThat(saved.getVehicleType()).isEqualTo(VehicleType.MOTORBIKE);
        assertThat(saved.getEndedAt()).isNull();
        assertThat(saved.getWorkSeconds()).isEqualTo(0);
    }

    @Test
    @DisplayName("toggleStatus: 잘못된 to (PENDING) → BAD_REQUEST")
    void toggleStatus_invalidTarget_badRequest() {
        assertThatThrownBy(() -> workSessionService.toggleStatus(CALLER_USER_NO, RiderStatus.PENDING))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("ACTIVE/EATING");
        verifyNoInteractions(riderRepository);
    }

    // ─── endWorkSession ────────────────────────────────────────────

    @Test
    @DisplayName("endWorkSession: 정상 종료 + work_seconds 계산 (응답 반환)")
    void endWorkSession_happy() {
        when(rider.getStatus()).thenReturn(RiderStatus.ACTIVE);
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));
        when(deliveryRepository.findFirstByRiderNoAndStatusInOrderByAssignedAtDesc(
                eq(CALLER_RIDER_NO), anyList()))
                .thenReturn(Optional.empty());

        WorkSession session = mock(WorkSession.class);
        when(session.getSessionNo()).thenReturn(99L);
        when(session.getWorkSeconds()).thenReturn(11400);
        when(session.getBreakSeconds()).thenReturn(4200);
        when(workSessionRepository.findByRiderNoAndEndedAtIsNull(CALLER_RIDER_NO))
                .thenReturn(Optional.of(session));

        WorkSessionEndRes res = workSessionService.endWorkSession(CALLER_USER_NO);

        verify(session).end(any(LocalDateTime.class));
        assertThat(res.sessionNo()).isEqualTo(99L);
        assertThat(res.workSeconds()).isEqualTo(11400);
        assertThat(res.breakSeconds()).isEqualTo(4200);
        assertThat(res.endedAt()).isNotNull();
    }

    @Test
    @DisplayName("endWorkSession: 진행 중 배달 있으면 CONFLICT")
    void endWorkSession_activeDelivery_conflict() {
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));
        Delivery active = mock(Delivery.class);
        when(active.getDeliveryNo()).thenReturn("00001ABC");
        when(deliveryRepository.findFirstByRiderNoAndStatusInOrderByAssignedAtDesc(
                eq(CALLER_RIDER_NO), anyList()))
                .thenReturn(Optional.of(active));

        assertThatThrownBy(() -> workSessionService.endWorkSession(CALLER_USER_NO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("00001ABC");
        verify(workSessionRepository, never()).findByRiderNoAndEndedAtIsNull(any());
    }

    @Test
    @DisplayName("endWorkSession: 진행 세션 없으면 CONFLICT")
    void endWorkSession_noProgressingSession_conflict() {
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));
        when(deliveryRepository.findFirstByRiderNoAndStatusInOrderByAssignedAtDesc(
                eq(CALLER_RIDER_NO), anyList()))
                .thenReturn(Optional.empty());
        when(workSessionRepository.findByRiderNoAndEndedAtIsNull(CALLER_RIDER_NO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> workSessionService.endWorkSession(CALLER_USER_NO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                .hasMessageContaining("근무 세션");
    }

    // ─── getSummary ────────────────────────────────────────────────

    @Test
    @DisplayName("getSummary: today period — 세션 합계 정확")
    void getSummary_today_aggregates() {
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));
        WorkSession s1 = mock(WorkSession.class);
        WorkSession s2 = mock(WorkSession.class);
        when(s1.getWorkSeconds()).thenReturn(11400);
        when(s1.getBreakSeconds()).thenReturn(4200);
        when(s2.getWorkSeconds()).thenReturn(7200);
        when(s2.getBreakSeconds()).thenReturn(600);
        when(workSessionRepository.findByRiderNoAndStartedAtBetweenOrderByStartedAtDesc(
                eq(CALLER_RIDER_NO), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(s1, s2));

        WorkSessionSummaryRes res = workSessionService.getSummary(CALLER_USER_NO, "today");

        assertThat(res.period()).isEqualTo("today");
        assertThat(res.sessionCount()).isEqualTo(2);
        assertThat(res.totalWorkSeconds()).isEqualTo(11400 + 7200);
        assertThat(res.totalBreakSeconds()).isEqualTo(4200 + 600);
    }

    @Test
    @DisplayName("getSummary: invalid period → BAD_REQUEST")
    void getSummary_invalidPeriod_badRequest() {
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));

        assertThatThrownBy(() -> workSessionService.getSummary(CALLER_USER_NO, "month"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("today/week");
    }

    @Test
    @DisplayName("getSummary: week period — 최근 7일 from 계산 + 합계 (W-4 보강)")
    void getSummary_week_happyPath() {
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));
        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        WorkSession s = mock(WorkSession.class);
        when(s.getWorkSeconds()).thenReturn(50400);
        when(s.getBreakSeconds()).thenReturn(7200);
        when(workSessionRepository.findByRiderNoAndStartedAtBetweenOrderByStartedAtDesc(
                eq(CALLER_RIDER_NO), fromCaptor.capture(), toCaptor.capture()))
                .thenReturn(List.of(s));

        WorkSessionSummaryRes res = workSessionService.getSummary(CALLER_USER_NO, "week");

        assertThat(res.period()).isEqualTo("week");
        assertThat(res.sessionCount()).isEqualTo(1);
        assertThat(res.totalWorkSeconds()).isEqualTo(50400);
        assertThat(res.totalBreakSeconds()).isEqualTo(7200);
        // from = 오늘 - 6일 00:00 (rolling 7-day window 포함)
        java.time.LocalDate expectedFromDate = java.time.LocalDate.now().minusDays(6);
        assertThat(fromCaptor.getValue().toLocalDate()).isEqualTo(expectedFromDate);
        assertThat(fromCaptor.getValue().toLocalTime()).isEqualTo(java.time.LocalTime.MIDNIGHT);
    }

    @Test
    @DisplayName("getTodaySession: 최신 세션 1건 매핑 (DESC 정렬 후 첫번째, W-4 보강)")
    void getTodaySession_returnsLatest() {
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));
        WorkSession latest = mock(WorkSession.class);
        WorkSession older = mock(WorkSession.class);
        when(latest.getSessionNo()).thenReturn(99L);
        when(latest.getStartedAt()).thenReturn(LocalDateTime.of(2026, 5, 7, 14, 0, 0));
        when(latest.getEndedAt()).thenReturn(null);
        when(latest.getWorkSeconds()).thenReturn(3600);
        when(latest.getBreakSeconds()).thenReturn(0);
        when(latest.getVehicleType()).thenReturn(VehicleType.MOTORBIKE);
        when(workSessionRepository.findByRiderNoAndStartedAtBetweenOrderByStartedAtDesc(
                eq(CALLER_RIDER_NO), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(latest, older));  // DESC 정렬 가정: 첫번째가 최신

        WorkSessionTodayRes res = workSessionService.getTodaySession(CALLER_USER_NO);

        assertThat(res.sessionNo()).isEqualTo(99L);
        assertThat(res.workSeconds()).isEqualTo(3600);
        assertThat(res.vehicleType()).isEqualTo("MOTORBIKE");
        assertThat(res.endedAt()).isNull();
        verifyNoInteractions(older);  // older 매핑 안 함
    }
}
