package com.green.mmg.rider.work;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.delivery.DeliveryRepository;
import com.green.mmg.rider.delivery.DeliveryService;
import com.green.mmg.rider.delivery.model.Delivery;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import com.green.mmg.rider.internal.dto.RiderInternalAssignReq;
import com.green.mmg.rider.rider.RiderRepository;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.RiderStatus;
import com.green.mmg.rider.rider.model.VehicleType;
import com.green.mmg.rider.work.dto.WorkSessionEndRes;
import com.green.mmg.rider.work.dto.WorkSessionStatusRes;
import com.green.mmg.rider.work.dto.WorkSessionTodayRes;
import com.green.mmg.rider.work.model.WorkSession;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 5-R8-7: WorkSessionService 통합 테스트 — 실 학원 DB + JPA INSERT/UPDATE 검증.
 *
 * <p>R3-c DeliveryServiceIntegrationTest 정착 패턴 일관 ({@code @SpringBootTest + @Transactional + @Rollback +
 * fixture INSERT + em.clear}, {@code feedback_integration_test_setup.md}).</p>
 *
 * <p>5건:
 * <ul>
 *   <li>EATING 배차 차단 (R4-1 D8-a 검증, DeliveryService.assignDelivery 호출 시 400)</li>
 *   <li>미완료 배달 존재 시 endWorkSession 거부 (CONFLICT)</li>
 *   <li>토글 자동 세션 생성 (ACTIVE 첫 진입 시 work_session 신규 ROW)</li>
 *   <li>로그인 유지 검증 (endWorkSession 후 status 그대로, signout 호출 X)</li>
 *   <li>break 누적 (EATING → ACTIVE 토글 후 break_seconds 0 이상)</li>
 * </ul></p>
 *
 * <p>학원 DB 의존 (Q-DB (가) 일관). @Rollback 자동 롤백.</p>
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("WorkSessionService 통합 (실 학원 DB)")
class WorkSessionServiceIntegrationTest {

    @Autowired private WorkSessionService workSessionService;
    @Autowired private DeliveryService deliveryService;
    @Autowired private WorkSessionRepository workSessionRepository;
    @Autowired private DeliveryRepository deliveryRepository;
    @Autowired private RiderRepository riderRepository;
    @Autowired private EntityManager em;

    private long uniqueUserNo() {
        return Math.abs(System.nanoTime() + ThreadLocalRandom.current().nextLong(1, 10_000));
    }

    private Rider seedRider(RiderStatus initial) {
        Rider rider = new Rider(
                uniqueUserNo(),
                "88-99-" + UUID.randomUUID().toString().substring(0, 6) + "-11",
                "2종보통",
                VehicleType.MOTORBIKE,
                "국민은행",
                "110-987-654321",
                "홍길동");
        if (initial == RiderStatus.ACTIVE || initial == RiderStatus.EATING) {
            rider.approve();
        }
        if (initial == RiderStatus.EATING) {
            rider.toggleEating();
        }
        return riderRepository.saveAndFlush(rider);
    }

    private Delivery seedDelivery(Long riderNo, DeliveryStatus status) {
        String deliveryNo = "IT" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Long orderId = System.nanoTime();
        Delivery delivery = new Delivery(
                deliveryNo, orderId,
                "010-1111-1111", "010-2222-2222",
                "가게 주소", 37.5665, 126.978,
                "손님 주소", 37.5670, 126.979,
                3000);
        delivery.assignRider(riderNo);
        delivery.changeStatus(status, LocalDateTime.now());
        return deliveryRepository.saveAndFlush(delivery);
    }

    @Test
    @DisplayName("EATING 라이더 배차 시도 → BAD_REQUEST (D8-a, R4-1 검증)")
    void eating_assign_blocked() {
        Rider eating = seedRider(RiderStatus.EATING);
        em.flush();
        em.clear();

        RiderInternalAssignReq req = new RiderInternalAssignReq(
                System.nanoTime(),
                1L, "가게이름", "가게 주소",
                37.5665, 126.978,
                "010-1111-1111",
                "손님 주소", 37.5670, 126.979,
                "010-2222-2222",
                3000, 0);

        assertThatThrownBy(() -> deliveryService.assignDelivery(eating.getRiderNo(), req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("배차 가능 상태가 아닙니다")
                .hasMessageContaining("EATING");
    }

    @Test
    @DisplayName("미완료 배달 존재 시 endWorkSession 거부 (CONFLICT)")
    void endWorkSession_activeDelivery_blocked() {
        Rider active = seedRider(RiderStatus.ACTIVE);
        // 진행 중 work_session
        WorkSession progressing = workSessionRepository.saveAndFlush(
                new WorkSession(active.getRiderNo(), VehicleType.MOTORBIKE, LocalDateTime.now().minusHours(1)));
        // 활성 배달
        Delivery delivering = seedDelivery(active.getRiderNo(), DeliveryStatus.DELIVERING);
        em.flush();
        em.clear();

        assertThatThrownBy(() -> workSessionService.endWorkSession(active.getUserNo()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(delivering.getDeliveryNo());

        // 세션 종료 X 검증
        WorkSession reloaded = workSessionRepository.findById(progressing.getSessionNo()).orElseThrow();
        assertThat(reloaded.getEndedAt()).isNull();
    }

    @Test
    @DisplayName("토글 ACTIVE 첫 진입 (EATING → ACTIVE) — 진행 세션 없으면 work_session 신규 생성")
    void toggleStatus_activeFirstEntry_createsSession() {
        Rider eating = seedRider(RiderStatus.EATING);
        em.flush();
        em.clear();

        WorkSessionStatusRes res = workSessionService.toggleStatus(eating.getUserNo(), RiderStatus.ACTIVE);
        em.flush();
        em.clear();

        assertThat(res.status()).isEqualTo("ACTIVE");
        Optional<WorkSession> created = workSessionRepository
                .findByRiderNoAndEndedAtIsNull(eating.getRiderNo());
        assertThat(created).isPresent();
        assertThat(created.get().getVehicleType()).isEqualTo(VehicleType.MOTORBIKE);
        assertThat(created.get().getEndedAt()).isNull();
    }

    @Test
    @DisplayName("로그인 유지 — endWorkSession 후 status 그대로 (D9-a, signout X)")
    void endWorkSession_doesNotSignOut() {
        Rider active = seedRider(RiderStatus.ACTIVE);
        workSessionRepository.saveAndFlush(
                new WorkSession(active.getRiderNo(), VehicleType.MOTORBIKE, LocalDateTime.now().minusHours(1)));
        em.flush();
        em.clear();

        WorkSessionEndRes res = workSessionService.endWorkSession(active.getUserNo());
        em.flush();
        em.clear();

        Rider reloaded = riderRepository.findById(active.getRiderNo()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RiderStatus.ACTIVE);  // 그대로 유지
        // today 조회 정상 동작 (인증 흐름 무관)
        WorkSessionTodayRes today = workSessionService.getTodaySession(active.getUserNo());
        assertThat(today.endedAt()).isNotNull();
        assertThat(res.workSeconds()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("EATING 라이더 endWorkSession 후 status=EATING 잔존 (D9-a, W-3 보강)")
    void endWorkSession_eatingRider_statusStays() {
        Rider eating = seedRider(RiderStatus.EATING);
        // 진행 중 세션 + break 메모리 보관 흐름 시뮬: EATING 진입 시 메모리에 들어가지만,
        // 통합 테스트에서는 ConcurrentHashMap에 직접 put 불가 → seed 직후 endWorkSession 호출.
        // breakStartedAt 메모리 없어도 endWorkSession은 정상 동작 (null 가드 일관).
        workSessionRepository.saveAndFlush(
                new WorkSession(eating.getRiderNo(), VehicleType.MOTORBIKE, LocalDateTime.now().minusHours(1)));
        em.flush();
        em.clear();

        workSessionService.endWorkSession(eating.getUserNo());
        em.flush();
        em.clear();

        Rider reloaded = riderRepository.findById(eating.getRiderNo()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RiderStatus.EATING);  // status 그대로
    }

    @Test
    @DisplayName("break 누적 — ACTIVE → EATING → ACTIVE 토글 시 break_seconds > 0")
    void toggleStatus_breakAccumulates() throws InterruptedException {
        Rider active = seedRider(RiderStatus.ACTIVE);
        // 진행 세션 fixture
        workSessionRepository.saveAndFlush(
                new WorkSession(active.getRiderNo(), VehicleType.MOTORBIKE, LocalDateTime.now().minusMinutes(30)));
        em.flush();
        em.clear();

        workSessionService.toggleStatus(active.getUserNo(), RiderStatus.EATING);
        em.flush();
        em.clear();

        // break 시간 확보 (1초 sleep — 메모리 ConcurrentHashMap 의존)
        Thread.sleep(1100);

        workSessionService.toggleStatus(active.getUserNo(), RiderStatus.ACTIVE);
        em.flush();
        em.clear();

        WorkSession reloaded = workSessionRepository
                .findByRiderNoAndEndedAtIsNull(active.getRiderNo()).orElseThrow();
        assertThat(reloaded.getBreakSeconds()).isGreaterThanOrEqualTo(1);
    }
}
