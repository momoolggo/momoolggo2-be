package com.green.mmg.rider.work;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.delivery.DeliveryRepository;
import com.green.mmg.rider.delivery.model.Delivery;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import com.green.mmg.rider.rider.RiderRepository;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.RiderStatus;
import com.green.mmg.rider.work.dto.WorkSessionEndRes;
import com.green.mmg.rider.work.dto.WorkSessionStatusRes;
import com.green.mmg.rider.work.dto.WorkSessionSummaryRes;
import com.green.mmg.rider.work.dto.WorkSessionTodayRes;
import com.green.mmg.rider.work.model.WorkSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * R8 라이더 근무 세션 — ADR-008 D8-a/D9-a 결정 일관.
 *
 * <p>핵심 4 endpoint:
 * <ul>
 *   <li>{@link #toggleStatus} — ACTIVE↔EATING 토글 (D8-a)</li>
 *   <li>{@link #endWorkSession} — 업무 종료 (D9-a)</li>
 *   <li>{@link #getTodaySession} — Figma 170202 근무관리 카드</li>
 *   <li>{@link #getSummary} — 오늘/주간 합계</li>
 * </ul>
 *
 * <p>R3 DeliveryService 패턴 일관 (callerUserNo 매개변수 / BusinessException 단일 패턴 /
 * @Transactional 단위 / R6 Rider 본인 한정 권한 검증). break_seconds 측정 = Q-Break (가) 메모리 보관
 * (서버 재기동 시 초기화, MVP 단순).</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WorkSessionService {

    private final RiderRepository riderRepository;
    private final WorkSessionRepository workSessionRepository;
    private final DeliveryRepository deliveryRepository;

    /** 활성 배달 상태 (업무 종료 차단 검증 기준, REQ-RDR-005). WAITING_ASSIGN / DELIVERED 제외. */
    private static final Set<DeliveryStatus> ACTIVE_DELIVERY_STATUSES = EnumSet.of(
            DeliveryStatus.ASSIGNED,
            DeliveryStatus.ARRIVED_AT_STORE,
            DeliveryStatus.AWAITING_PICKUP,
            DeliveryStatus.PICKED_UP,
            DeliveryStatus.DELIVERING
    );

    /**
     * Q-Break (가) 메모리 — riderNo → break 시작 시각. EATING 진입 시 put, ACTIVE 복귀/end 시 remove + 누적.
     * 서버 재기동 시 초기화 (MVP 단순, Phase 6+ 별 컬럼 검토).
     */
    private final ConcurrentHashMap<Long, LocalDateTime> breakStartedAt = new ConcurrentHashMap<>();

    /**
     * ACTIVE↔EATING 토글 (D8-a).
     * <ul>
     *   <li>ACTIVE → EATING: rider.status=EATING + breakStartedAt 메모리 저장</li>
     *   <li>EATING → ACTIVE: rider.status=ACTIVE + 진행 세션 break_seconds 누적</li>
     *   <li>ACTIVE 첫 진입(진행 세션 없음): work_session 신규 생성</li>
     * </ul>
     */
    public WorkSessionStatusRes toggleStatus(Long callerUserNo, RiderStatus to) {
        if (to != RiderStatus.ACTIVE && to != RiderStatus.EATING) {
            throw new BusinessException("toggle 가능 상태는 ACTIVE/EATING뿐입니다: " + to, HttpStatus.BAD_REQUEST);
        }

        Rider rider = riderRepository.findByUserNo(callerUserNo)
                .orElseThrow(() -> new BusinessException("라이더 프로필이 등록되지 않았습니다.", HttpStatus.NOT_FOUND));

        RiderStatus from = rider.getStatus();
        if (to == from) {
            // 동일 상태 토글 = no-op (응답만 반환). 충돌로 보지 않음 (UX 자연스러움).
            return new WorkSessionStatusRes(rider.getRiderNo(), from.name());
        }

        if (to == RiderStatus.EATING) {
            if (from != RiderStatus.ACTIVE) {
                throw new BusinessException(
                        "ACTIVE 상태에서만 EATING 전환 가능: from=" + from, HttpStatus.CONFLICT);
            }
            rider.toggleEating();
            breakStartedAt.put(rider.getRiderNo(), LocalDateTime.now());
        } else {  // to == ACTIVE
            if (from != RiderStatus.EATING) {
                throw new BusinessException(
                        "EATING 상태에서만 ACTIVE 복귀 가능: from=" + from, HttpStatus.CONFLICT);
            }
            rider.resumeActive();
            // break 누적 (메모리 보관 시각 기준)
            LocalDateTime breakStart = breakStartedAt.remove(rider.getRiderNo());
            if (breakStart != null) {
                workSessionRepository.findByRiderNoAndEndedAtIsNull(rider.getRiderNo())
                        .ifPresent(session -> {
                            int breakSeconds = (int) Duration.between(breakStart, LocalDateTime.now()).getSeconds();
                            session.addBreak(breakSeconds);
                        });
            }
        }

        // ACTIVE 진입 시 진행 중 세션 없으면 신규 생성 (ADR-008 흐름)
        if (to == RiderStatus.ACTIVE) {
            if (workSessionRepository.findByRiderNoAndEndedAtIsNull(rider.getRiderNo()).isEmpty()) {
                workSessionRepository.save(new WorkSession(
                        rider.getRiderNo(), rider.getVehicleType(), LocalDateTime.now()));
            }
        }

        return new WorkSessionStatusRes(rider.getRiderNo(), rider.getStatus().name());
    }

    /**
     * 업무 종료 (D9-a). 활성 배달 있으면 거부, work_session.end + work_seconds 계산.
     * status는 그대로 유지 (D9-a: 로그인 세션 무관, signout 호출 X).
     */
    public WorkSessionEndRes endWorkSession(Long callerUserNo) {
        Rider rider = riderRepository.findByUserNo(callerUserNo)
                .orElseThrow(() -> new BusinessException("라이더 프로필이 등록되지 않았습니다.", HttpStatus.NOT_FOUND));

        Optional<Delivery> activeDelivery = deliveryRepository
                .findFirstByRiderNoAndStatusInOrderByAssignedAtDesc(rider.getRiderNo(), List.copyOf(ACTIVE_DELIVERY_STATUSES));
        if (activeDelivery.isPresent()) {
            throw new BusinessException(
                    "진행 중 배달이 있어 업무 종료 불가: " + activeDelivery.get().getDeliveryNo(),
                    HttpStatus.CONFLICT);
        }

        WorkSession session = workSessionRepository.findByRiderNoAndEndedAtIsNull(rider.getRiderNo())
                .orElseThrow(() -> new BusinessException("진행 중 근무 세션이 없습니다.", HttpStatus.CONFLICT));

        LocalDateTime now = LocalDateTime.now();
        // EATING 중 종료 = 진행 중 break도 누적
        if (rider.getStatus() == RiderStatus.EATING) {
            LocalDateTime breakStart = breakStartedAt.remove(rider.getRiderNo());
            if (breakStart != null) {
                int breakSeconds = (int) Duration.between(breakStart, now).getSeconds();
                session.addBreak(breakSeconds);
            }
        }
        session.end(now);

        return new WorkSessionEndRes(
                session.getSessionNo(), now, session.getWorkSeconds(), session.getBreakSeconds());
    }

    /** Figma 170202 근무관리 카드 — 오늘 시작된 세션 1건 (없으면 sessionNo null). */
    @Transactional(readOnly = true)
    public WorkSessionTodayRes getTodaySession(Long callerUserNo) {
        Rider rider = riderRepository.findByUserNo(callerUserNo)
                .orElseThrow(() -> new BusinessException("라이더 프로필이 등록되지 않았습니다.", HttpStatus.NOT_FOUND));

        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = from.plusDays(1);
        List<WorkSession> todaySessions = workSessionRepository
                .findByRiderNoAndStartedAtBetweenOrderByStartedAtDesc(rider.getRiderNo(), from, to);

        if (todaySessions.isEmpty()) {
            return new WorkSessionTodayRes(null, null, null, 0, 0, rider.getVehicleType().name());
        }
        WorkSession latest = todaySessions.get(0);  // W-1 fix: DESC 정렬로 최신 1건
        return new WorkSessionTodayRes(
                latest.getSessionNo(),
                latest.getStartedAt(),
                latest.getEndedAt(),
                latest.getWorkSeconds(),
                latest.getBreakSeconds(),
                latest.getVehicleType().name()
        );
    }

    /** 오늘 또는 주간 합계 (today / week). */
    @Transactional(readOnly = true)
    public WorkSessionSummaryRes getSummary(Long callerUserNo, String period) {
        Rider rider = riderRepository.findByUserNo(callerUserNo)
                .orElseThrow(() -> new BusinessException("라이더 프로필이 등록되지 않았습니다.", HttpStatus.NOT_FOUND));

        LocalDateTime from;
        LocalDateTime to = LocalDateTime.now();
        if ("week".equalsIgnoreCase(period)) {
            from = LocalDate.now().minusDays(6).atStartOfDay();
        } else if ("today".equalsIgnoreCase(period)) {
            from = LocalDate.now().atStartOfDay();
        } else {
            throw new BusinessException("period는 today/week만 허용: " + period, HttpStatus.BAD_REQUEST);
        }

        List<WorkSession> sessions = workSessionRepository
                .findByRiderNoAndStartedAtBetweenOrderByStartedAtDesc(rider.getRiderNo(), from, to);
        int totalWorkSeconds = sessions.stream().mapToInt(WorkSession::getWorkSeconds).sum();
        int totalBreakSeconds = sessions.stream().mapToInt(WorkSession::getBreakSeconds).sum();

        return new WorkSessionSummaryRes(period, sessions.size(), totalWorkSeconds, totalBreakSeconds);
    }
}
