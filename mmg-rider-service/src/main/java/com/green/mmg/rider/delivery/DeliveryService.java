package com.green.mmg.rider.delivery;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.delivery.model.ActorRole;
import com.green.mmg.rider.delivery.model.Delivery;
import com.green.mmg.rider.delivery.model.DeliveryLog;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import com.green.mmg.rider.internal.dto.RiderInternalAssignReq;
import com.green.mmg.rider.internal.dto.RiderInternalAssignRes;
import com.green.mmg.rider.internal.dto.RiderInternalStatusRes;
import com.green.mmg.rider.rider.RiderRepository;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.RiderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 배달 도메인 서비스 — Phase 5-R3-b 범위 (상태 머신 + 낙관적 락 + delivery_log INSERT).
 *
 * <h3>상태 머신 (ADR-004)</h3>
 * 7개 상태 7 합법 전이 (ASSIGNED→WAITING_ASSIGN reject 포함, 사례 #20 정정 — "8 합법" 부정확).
 * 위반 시 BusinessException(HttpStatus.BAD_REQUEST). 결정 7 (가) Service 내부 단일 검증.
 *
 * <h3>낙관적 락 (Q5-A, ADR-004 line 100-115)</h3>
 * Delivery.@Version + Service 내부 saveAndFlush + try-catch (사례 #19 결정 11 (i)) →
 * BusinessException(HttpStatus.CONFLICT) 변환 → mmg-common GlobalExceptionHandler.e.getStatus() 동적 매핑 → HTTP 409.
 * mmg-common 미수정 (영역 ✅, R1-A 정착 패턴 일관).
 *
 * <h3>delivery_log 같은 트랜잭션 INSERT</h3>
 * @Transactional 같은 트랜잭션 안에서 delivery UPDATE + delivery_log INSERT. 실패 시 둘 다 롤백.
 *
 * <h3>callerActorRole 매개변수 명시 (결정 8 (가))</h3>
 * R1-A SecurityContext 의존 X 정착 패턴 일관. Controller(R6/R7)에서 호출자 권한 분기 후 callerActorRole 전달.
 * RIDER 액터만 본인 배달 검증. ADMIN/SYSTEM은 검증 X (R7 강제 변경 / SYSTEM 자동 처리).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    /** ADR-004 line 76-82 화이트리스트 — 7 합법 전이 (사례 #20 정정 일관) */
    private static final Map<DeliveryStatus, Set<DeliveryStatus>> ALLOWED_TRANSITIONS;

    static {
        EnumMap<DeliveryStatus, Set<DeliveryStatus>> map = new EnumMap<>(DeliveryStatus.class);
        map.put(DeliveryStatus.WAITING_ASSIGN, EnumSet.of(DeliveryStatus.ASSIGNED));
        map.put(DeliveryStatus.ASSIGNED,
                EnumSet.of(DeliveryStatus.ARRIVED_AT_STORE, DeliveryStatus.WAITING_ASSIGN));
        map.put(DeliveryStatus.ARRIVED_AT_STORE, EnumSet.of(DeliveryStatus.AWAITING_PICKUP));
        map.put(DeliveryStatus.AWAITING_PICKUP, EnumSet.of(DeliveryStatus.PICKED_UP));
        map.put(DeliveryStatus.PICKED_UP, EnumSet.of(DeliveryStatus.DELIVERING));
        map.put(DeliveryStatus.DELIVERING, EnumSet.of(DeliveryStatus.DELIVERED));
        map.put(DeliveryStatus.DELIVERED, EnumSet.noneOf(DeliveryStatus.class));
        ALLOWED_TRANSITIONS = Map.copyOf(map);
    }

    private final DeliveryRepository deliveryRepository;
    private final DeliveryLogRepository deliveryLogRepository;
    private final RiderRepository riderRepository;

    /**
     * 배달 상태 전환 (화이트리스트 + 권한 + 낙관적 락 + delivery_log INSERT).
     *
     * @param deliveryNo PK
     * @param to 전환 대상 상태
     * @param callerUserNo SecurityContextHolder 추출 (dto.userNo 위조 방지)
     * @param callerActorRole RIDER/SYSTEM/ADMIN (결정 8 (가) 매개변수 명시)
     */
    @Transactional
    public void updateStatus(String deliveryNo, DeliveryStatus to,
                             long callerUserNo, ActorRole callerActorRole) {
        Delivery delivery = deliveryRepository.findById(deliveryNo)
                .orElseThrow(() -> new BusinessException("배달을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 권한: RIDER 액터만 본인 배달 검증
        if (callerActorRole == ActorRole.RIDER) {
            Rider caller = riderRepository.findByUserNo(callerUserNo)
                    .orElseThrow(() -> new BusinessException(
                            "라이더 프로필이 등록되지 않았습니다.", HttpStatus.NOT_FOUND));
            if (!Objects.equals(delivery.getRiderNo(), caller.getRiderNo())) {
                throw new BusinessException("본인 배달이 아닙니다.", HttpStatus.FORBIDDEN);
            }
        }

        // 화이트리스트 검증
        DeliveryStatus from = delivery.getStatus();
        if (!ALLOWED_TRANSITIONS.get(from).contains(to)) {
            throw new BusinessException(
                    "invalid state transition: " + from + " -> " + to,
                    HttpStatus.BAD_REQUEST);
        }

        // 상태 변경 + 단계별 시각 기록
        delivery.changeStatus(to, LocalDateTime.now());

        // saveAndFlush + try-catch — OptimisticLock 메서드 내부 즉시 발생 + 변환 (결정 11 (i))
        try {
            deliveryRepository.saveAndFlush(delivery);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(
                    "동시 변경 충돌이 발생했습니다. 새로고침 후 다시 시도하세요.",
                    HttpStatus.CONFLICT);
        }

        // log 기록 (같은 트랜잭션, callerActorRole 매개변수 직접 사용)
        deliveryLogRepository.save(new DeliveryLog(deliveryNo, from, to, callerActorRole, callerUserNo));
    }

    /**
     * 배차 요청 처리 — interfaces.md §1.1 (Main → Rider).
     * Rider ACTIVE 검증 + Delivery 생성(WAITING_ASSIGN → ASSIGNED 즉시 전환) + delivery_log INSERT.
     * actorRole = SYSTEM (자동 처리, actorUserNo = null).
     */
    @Transactional
    public RiderInternalAssignRes assignDelivery(Long riderNo, RiderInternalAssignReq req) {
        Rider rider = riderRepository.findById(riderNo)
                .orElseThrow(() -> new BusinessException("라이더를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (rider.getStatus() != RiderStatus.ACTIVE) {
            throw new BusinessException(
                    "라이더가 배차 가능 상태가 아닙니다 (현재: " + rider.getStatus() + ").",
                    HttpStatus.BAD_REQUEST);
        }

        String deliveryNo = generateDeliveryNo();
        Delivery delivery = new Delivery(
                deliveryNo, req.orderId(),
                req.storePhone(), req.customerPhone(),
                req.storeAddress(), req.storeLat(), req.storeLng(),
                req.deliveryAddress(), req.deliveryLat(), req.deliveryLng(),
                req.baseFee());
        delivery.assignRider(riderNo);
        LocalDateTime now = LocalDateTime.now();
        delivery.changeStatus(DeliveryStatus.ASSIGNED, now);

        try {
            deliveryRepository.saveAndFlush(delivery);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(
                    "동시 배차 충돌이 발생했습니다. 새로고침 후 다시 시도하세요.",
                    HttpStatus.CONFLICT);
        }

        deliveryLogRepository.save(new DeliveryLog(
                deliveryNo, null, DeliveryStatus.ASSIGNED, ActorRole.SYSTEM, null));

        return new RiderInternalAssignRes(true, deliveryNo, riderNo, now);
    }

    /**
     * 라이더 상태 조회 — interfaces.md §1.3 (Main/Admin → Rider).
     * Rider.status + 진행 중 배달 1건 조회 (ASSIGNED~DELIVERING 5 상태 중, 가장 최근 배차).
     */
    @Transactional(readOnly = true)
    public RiderInternalStatusRes getRiderInternalStatus(Long riderNo) {
        Rider rider = riderRepository.findById(riderNo)
                .orElseThrow(() -> new BusinessException("라이더를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        List<DeliveryStatus> inProgress = List.of(
                DeliveryStatus.ASSIGNED,
                DeliveryStatus.ARRIVED_AT_STORE,
                DeliveryStatus.AWAITING_PICKUP,
                DeliveryStatus.PICKED_UP,
                DeliveryStatus.DELIVERING);

        String currentDeliveryNo = deliveryRepository
                .findFirstByRiderNoAndStatusInOrderByAssignedAtDesc(riderNo, inProgress)
                .map(Delivery::getDeliveryNo)
                .orElse(null);

        return new RiderInternalStatusRes(riderNo, rider.getStatus().name(), currentDeliveryNo);
    }

    /** delivery_no 자동 생성 — 5자리 timestamp + 3자리 영문 (interfaces.md §1.1 박제 형식 예시 일관). */
    private static String generateDeliveryNo() {
        long ts = System.currentTimeMillis() % 100_000;
        String alpha = UUID.randomUUID().toString().replaceAll("[^a-zA-Z]", "")
                .substring(0, 3).toUpperCase();
        return String.format("%05d%s", ts, alpha);
    }
}
