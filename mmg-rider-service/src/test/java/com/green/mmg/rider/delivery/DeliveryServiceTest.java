package com.green.mmg.rider.delivery;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.delivery.model.ActorRole;
import com.green.mmg.rider.delivery.model.Delivery;
import com.green.mmg.rider.delivery.model.DeliveryLog;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import com.green.mmg.rider.internal.dto.RiderInternalAssignReq;
import com.green.mmg.rider.internal.dto.RiderInternalAssignRes;
import com.green.mmg.rider.internal.dto.RiderInternalMonitorRes;
import com.green.mmg.rider.internal.dto.RiderInternalStatusRes;
import com.green.mmg.rider.rider.RiderRepository;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.RiderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

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
 * DeliveryService 단위 테스트 — Phase 5-R3-b (가짜 테스트 0건 원칙, CLAUDE.md §6.5).
 *
 * <p>23건 = 7 합법 + 12 비합법 대표 + 1 권한 + 1 낙관적 락 + 1 NOT_FOUND + 1 log INSERT.
 * R3-c 통합 3건(orders.delivery_state 매핑)은 별건.</p>
 *
 * <p>Mockito 패턴 일관 — 학원 DB 의존성 회피 (Q-DB (다)). entity 권한 케이스는 Mockito spy로 riderNo 박제
 * (Delivery setter 0 정착 일관, R4 진입 시 entity assignRider 메서드 추가 예정).</p>
 */
@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    private static final String DELIVERY_NO = "00001ABC";
    private static final String ORDER_ID = "0000001A";
    private static final long CALLER_USER_NO = 42L;
    private static final Long CALLER_RIDER_NO = 5L;

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryLogRepository deliveryLogRepository;
    @Mock private RiderRepository riderRepository;

    @InjectMocks private DeliveryService deliveryService;

    private Rider callerRider;

    @BeforeEach
    void setUp() {
        callerRider = mock(Rider.class);
        lenient().when(callerRider.getRiderNo()).thenReturn(CALLER_RIDER_NO);
    }

    private Delivery deliveryWith(DeliveryStatus status, Long riderNo) {
        Delivery delivery = spy(new Delivery(
                DELIVERY_NO, ORDER_ID,
                null, null, null, null, null, null, null, null,
                3000));
        if (status != DeliveryStatus.WAITING_ASSIGN) {
            delivery.changeStatus(status, LocalDateTime.now());
        }
        lenient().when(delivery.getRiderNo()).thenReturn(riderNo);
        return delivery;
    }

    private void stubFindAndOwn(Delivery delivery) {
        when(deliveryRepository.findById(DELIVERY_NO)).thenReturn(Optional.of(delivery));
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(callerRider));
        when(deliveryRepository.saveAndFlush(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("합법 전이 (7건, ADR-004 line 76-82 화이트리스트)")
    class LegalTransitions {

        @Test
        @DisplayName("WAITING_ASSIGN → ASSIGNED")
        void waitingAssign_to_assigned() {
            Delivery delivery = deliveryWith(DeliveryStatus.WAITING_ASSIGN, CALLER_RIDER_NO);
            stubFindAndOwn(delivery);

            deliveryService.updateStatus(DELIVERY_NO, DeliveryStatus.ASSIGNED, CALLER_USER_NO, ActorRole.RIDER);

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
            assertThat(delivery.getAssignedAt()).isNotNull();
            verify(deliveryRepository).saveAndFlush(delivery);
            verify(deliveryLogRepository).save(any(DeliveryLog.class));
        }

        @Test
        @DisplayName("ASSIGNED → ARRIVED_AT_STORE")
        void assigned_to_arrivedAtStore() {
            Delivery delivery = deliveryWith(DeliveryStatus.ASSIGNED, CALLER_RIDER_NO);
            stubFindAndOwn(delivery);

            deliveryService.updateStatus(DELIVERY_NO, DeliveryStatus.ARRIVED_AT_STORE, CALLER_USER_NO, ActorRole.RIDER);

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.ARRIVED_AT_STORE);
            assertThat(delivery.getArrivedAtStoreAt()).isNotNull();
            verify(deliveryRepository).saveAndFlush(delivery);
        }

        @Test
        @DisplayName("ASSIGNED → WAITING_ASSIGN (reject 재할당)")
        void assigned_to_waitingAssign_reject() {
            Delivery delivery = deliveryWith(DeliveryStatus.ASSIGNED, CALLER_RIDER_NO);
            stubFindAndOwn(delivery);

            deliveryService.updateStatus(DELIVERY_NO, DeliveryStatus.WAITING_ASSIGN, CALLER_USER_NO, ActorRole.RIDER);

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.WAITING_ASSIGN);
            verify(deliveryRepository).saveAndFlush(delivery);
        }

        @Test
        @DisplayName("ARRIVED_AT_STORE → AWAITING_PICKUP")
        void arrivedAtStore_to_awaitingPickup() {
            Delivery delivery = deliveryWith(DeliveryStatus.ARRIVED_AT_STORE, CALLER_RIDER_NO);
            stubFindAndOwn(delivery);

            deliveryService.updateStatus(DELIVERY_NO, DeliveryStatus.AWAITING_PICKUP, CALLER_USER_NO, ActorRole.RIDER);

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.AWAITING_PICKUP);
            verify(deliveryRepository).saveAndFlush(delivery);
        }

        @Test
        @DisplayName("AWAITING_PICKUP → PICKED_UP")
        void awaitingPickup_to_pickedUp() {
            Delivery delivery = deliveryWith(DeliveryStatus.AWAITING_PICKUP, CALLER_RIDER_NO);
            stubFindAndOwn(delivery);

            deliveryService.updateStatus(DELIVERY_NO, DeliveryStatus.PICKED_UP, CALLER_USER_NO, ActorRole.RIDER);

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PICKED_UP);
            assertThat(delivery.getPickedAt()).isNotNull();
            verify(deliveryRepository).saveAndFlush(delivery);
        }

        @Test
        @DisplayName("PICKED_UP → DELIVERING")
        void pickedUp_to_delivering() {
            Delivery delivery = deliveryWith(DeliveryStatus.PICKED_UP, CALLER_RIDER_NO);
            stubFindAndOwn(delivery);

            deliveryService.updateStatus(DELIVERY_NO, DeliveryStatus.DELIVERING, CALLER_USER_NO, ActorRole.RIDER);

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERING);
            assertThat(delivery.getDeliveringAt()).isNotNull();
            verify(deliveryRepository).saveAndFlush(delivery);
        }

        @Test
        @DisplayName("DELIVERING → DELIVERED (terminal)")
        void delivering_to_delivered() {
            Delivery delivery = deliveryWith(DeliveryStatus.DELIVERING, CALLER_RIDER_NO);
            stubFindAndOwn(delivery);

            deliveryService.updateStatus(DELIVERY_NO, DeliveryStatus.DELIVERED, CALLER_USER_NO, ActorRole.RIDER);

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
            assertThat(delivery.getDeliveredAt()).isNotNull();
            verify(deliveryRepository).saveAndFlush(delivery);
        }
    }

    @Nested
    @DisplayName("비합법 전이 12건 (ADR-004 line 161 대표 매핑)")
    class IllegalTransitions {

        private void assertIllegal(DeliveryStatus from, DeliveryStatus to) {
            Delivery delivery = deliveryWith(from, CALLER_RIDER_NO);
            when(deliveryRepository.findById(DELIVERY_NO)).thenReturn(Optional.of(delivery));
            when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(callerRider));

            assertThatThrownBy(() -> deliveryService.updateStatus(DELIVERY_NO, to, CALLER_USER_NO, ActorRole.RIDER))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("invalid state transition")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(deliveryRepository, never()).saveAndFlush(any(Delivery.class));
            verify(deliveryLogRepository, never()).save(any(DeliveryLog.class));
        }

        @Test
        @DisplayName("WAITING_ASSIGN → PICKED_UP (단계 건너뜀)")
        void waitingAssign_to_pickedUp() { assertIllegal(DeliveryStatus.WAITING_ASSIGN, DeliveryStatus.PICKED_UP); }

        @Test
        @DisplayName("WAITING_ASSIGN → DELIVERING (단계 건너뜀)")
        void waitingAssign_to_delivering() { assertIllegal(DeliveryStatus.WAITING_ASSIGN, DeliveryStatus.DELIVERING); }

        @Test
        @DisplayName("WAITING_ASSIGN → DELIVERED (terminal 직행)")
        void waitingAssign_to_delivered() { assertIllegal(DeliveryStatus.WAITING_ASSIGN, DeliveryStatus.DELIVERED); }

        @Test
        @DisplayName("ASSIGNED → AWAITING_PICKUP (단계 건너뜀)")
        void assigned_to_awaitingPickup() { assertIllegal(DeliveryStatus.ASSIGNED, DeliveryStatus.AWAITING_PICKUP); }

        @Test
        @DisplayName("ASSIGNED → PICKED_UP (단계 건너뜀)")
        void assigned_to_pickedUp() { assertIllegal(DeliveryStatus.ASSIGNED, DeliveryStatus.PICKED_UP); }

        @Test
        @DisplayName("ARRIVED_AT_STORE → DELIVERED (terminal 직행)")
        void arrivedAtStore_to_delivered() { assertIllegal(DeliveryStatus.ARRIVED_AT_STORE, DeliveryStatus.DELIVERED); }

        @Test
        @DisplayName("AWAITING_PICKUP → DELIVERED (terminal 직행)")
        void awaitingPickup_to_delivered() { assertIllegal(DeliveryStatus.AWAITING_PICKUP, DeliveryStatus.DELIVERED); }

        @Test
        @DisplayName("PICKED_UP → WAITING_ASSIGN (역방향)")
        void pickedUp_to_waitingAssign() { assertIllegal(DeliveryStatus.PICKED_UP, DeliveryStatus.WAITING_ASSIGN); }

        @Test
        @DisplayName("DELIVERING → AWAITING_PICKUP (역방향)")
        void delivering_to_awaitingPickup() { assertIllegal(DeliveryStatus.DELIVERING, DeliveryStatus.AWAITING_PICKUP); }

        @Test
        @DisplayName("DELIVERED → ASSIGNED (terminal 후)")
        void delivered_to_assigned() { assertIllegal(DeliveryStatus.DELIVERED, DeliveryStatus.ASSIGNED); }

        @Test
        @DisplayName("DELIVERED → PICKED_UP (terminal 후)")
        void delivered_to_pickedUp() { assertIllegal(DeliveryStatus.DELIVERED, DeliveryStatus.PICKED_UP); }

        @Test
        @DisplayName("DELIVERED → DELIVERING (terminal 후)")
        void delivered_to_delivering() { assertIllegal(DeliveryStatus.DELIVERED, DeliveryStatus.DELIVERING); }
    }

    @Nested
    @DisplayName("권한 검증 + 낙관적 락 + NOT_FOUND + log INSERT (4건)")
    class AuthorizationAndConcurrency {

        @Test
        @DisplayName("권한: RIDER 액터 + 다른 라이더 배달 → BusinessException FORBIDDEN + saveAndFlush/log 미호출")
        void riderActor_notOwnDelivery_throwsForbidden() {
            Long otherRiderNo = 999L;
            Delivery delivery = deliveryWith(DeliveryStatus.ASSIGNED, otherRiderNo);
            when(deliveryRepository.findById(DELIVERY_NO)).thenReturn(Optional.of(delivery));
            when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(callerRider));

            assertThatThrownBy(() -> deliveryService.updateStatus(
                    DELIVERY_NO, DeliveryStatus.ARRIVED_AT_STORE, CALLER_USER_NO, ActorRole.RIDER))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 배달이 아닙니다.")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(deliveryRepository, never()).saveAndFlush(any(Delivery.class));
            verify(deliveryLogRepository, never()).save(any(DeliveryLog.class));
        }

        @Test
        @DisplayName("낙관적 락: saveAndFlush ObjectOptimisticLockingFailureException → BusinessException CONFLICT + log 미호출")
        void saveAndFlush_optimisticLock_throwsConflict() {
            Delivery delivery = deliveryWith(DeliveryStatus.ASSIGNED, CALLER_RIDER_NO);
            when(deliveryRepository.findById(DELIVERY_NO)).thenReturn(Optional.of(delivery));
            when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(callerRider));
            when(deliveryRepository.saveAndFlush(any(Delivery.class)))
                    .thenThrow(new ObjectOptimisticLockingFailureException(Delivery.class, DELIVERY_NO));

            assertThatThrownBy(() -> deliveryService.updateStatus(
                    DELIVERY_NO, DeliveryStatus.ARRIVED_AT_STORE, CALLER_USER_NO, ActorRole.RIDER))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("동시 변경 충돌")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(deliveryRepository).saveAndFlush(delivery);
            verify(deliveryLogRepository, never()).save(any(DeliveryLog.class));
        }

        @Test
        @DisplayName("NOT_FOUND: delivery 부재 → BusinessException NOT_FOUND + saveAndFlush/log 미호출")
        void delivery_notFound_throwsNotFound() {
            when(deliveryRepository.findById(DELIVERY_NO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryService.updateStatus(
                    DELIVERY_NO, DeliveryStatus.ASSIGNED, CALLER_USER_NO, ActorRole.RIDER))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("배달을 찾을 수 없습니다.")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(deliveryRepository, never()).saveAndFlush(any(Delivery.class));
            verify(deliveryLogRepository, never()).save(any(DeliveryLog.class));
        }

        @Test
        @DisplayName("delivery_log 같은 트랜잭션 INSERT: from/to/actorRole/userNo 정확 박제 (ArgumentCaptor)")
        void deliveryLog_argumentsCaptured() {
            Delivery delivery = deliveryWith(DeliveryStatus.ASSIGNED, CALLER_RIDER_NO);
            stubFindAndOwn(delivery);

            deliveryService.updateStatus(DELIVERY_NO, DeliveryStatus.ARRIVED_AT_STORE, CALLER_USER_NO, ActorRole.RIDER);

            ArgumentCaptor<DeliveryLog> logCaptor = ArgumentCaptor.forClass(DeliveryLog.class);
            verify(deliveryLogRepository).save(logCaptor.capture());

            DeliveryLog captured = logCaptor.getValue();
            assertThat(captured.getDeliveryNo()).isEqualTo(DELIVERY_NO);
            assertThat(captured.getFromStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
            assertThat(captured.getToStatus()).isEqualTo(DeliveryStatus.ARRIVED_AT_STORE);
            assertThat(captured.getActorRole()).isEqualTo(ActorRole.RIDER);
            assertThat(captured.getActorUserNo()).isEqualTo(CALLER_USER_NO);
        }
    }

    @Nested
    @DisplayName("AssignDelivery (R4 §1.1, 5건)")
    class AssignDelivery {

        private RiderInternalAssignReq sampleReq() {
            return new RiderInternalAssignReq(
                    "ORD0001", 7L, "맛있는집",
                    "가게 주소", 35.123, 128.456,
                    "053-111-2222",
                    "손님 주소", 35.130, 128.460,
                    "010-1234-5678",
                    4000, 1500);
        }

        private Rider activeRider() {
            Rider rider = mock(Rider.class);
            lenient().when(rider.getRiderNo()).thenReturn(CALLER_RIDER_NO);
            lenient().when(rider.getStatus()).thenReturn(RiderStatus.ACTIVE);
            return rider;
        }

        @Test
        @DisplayName("happy: ACTIVE rider + Delivery 생성(WAITING_ASSIGN→ASSIGNED) + log INSERT(SYSTEM) + 응답 박제")
        void happy_assigned() {
            Rider rider = activeRider();
            when(riderRepository.findById(CALLER_RIDER_NO)).thenReturn(Optional.of(rider));
            when(deliveryRepository.saveAndFlush(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

            RiderInternalAssignRes res = deliveryService.assignDelivery(CALLER_RIDER_NO, sampleReq());

            assertThat(res.assigned()).isTrue();
            assertThat(res.riderNo()).isEqualTo(CALLER_RIDER_NO);
            assertThat(res.deliveryNo()).isNotNull();
            assertThat(res.assignedAt()).isNotNull();

            ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
            verify(deliveryRepository).saveAndFlush(deliveryCaptor.capture());
            Delivery saved = deliveryCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
            assertThat(saved.getRiderNo()).isEqualTo(CALLER_RIDER_NO);
            assertThat(saved.getOrderId()).isEqualTo("ORD0001");
            assertThat(saved.getAssignedAt()).isNotNull();
            assertThat(saved.getBaseFee()).isEqualTo(4000);

            ArgumentCaptor<DeliveryLog> logCaptor = ArgumentCaptor.forClass(DeliveryLog.class);
            verify(deliveryLogRepository).save(logCaptor.capture());
            DeliveryLog log = logCaptor.getValue();
            assertThat(log.getFromStatus()).isNull();
            assertThat(log.getToStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
            assertThat(log.getActorRole()).isEqualTo(ActorRole.SYSTEM);
            assertThat(log.getActorUserNo()).isNull();
        }

        @Test
        @DisplayName("rider 부재 → BusinessException NOT_FOUND + saveAndFlush/log 미호출")
        void riderNotFound_throwsNotFound() {
            when(riderRepository.findById(CALLER_RIDER_NO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryService.assignDelivery(CALLER_RIDER_NO, sampleReq()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("라이더를 찾을 수 없습니다.")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(deliveryRepository, never()).saveAndFlush(any(Delivery.class));
            verify(deliveryLogRepository, never()).save(any(DeliveryLog.class));
        }

        @Test
        @DisplayName("rider PENDING → BusinessException BAD_REQUEST (ACTIVE 외 상태)")
        void riderNotActive_throwsBadRequest() {
            Rider pendingRider = mock(Rider.class);
            when(pendingRider.getStatus()).thenReturn(RiderStatus.PENDING);
            when(riderRepository.findById(CALLER_RIDER_NO)).thenReturn(Optional.of(pendingRider));

            assertThatThrownBy(() -> deliveryService.assignDelivery(CALLER_RIDER_NO, sampleReq()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("배차 가능 상태가 아닙니다")
                    .hasMessageContaining("PENDING")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(deliveryRepository, never()).saveAndFlush(any(Delivery.class));
            verify(deliveryLogRepository, never()).save(any(DeliveryLog.class));
        }

        @Test
        @DisplayName("saveAndFlush 낙관적 락 충돌 → BusinessException CONFLICT + log 미호출")
        void optimisticLock_throwsConflict() {
            Rider rider = activeRider();
            when(riderRepository.findById(CALLER_RIDER_NO)).thenReturn(Optional.of(rider));
            when(deliveryRepository.saveAndFlush(any(Delivery.class)))
                    .thenThrow(new ObjectOptimisticLockingFailureException(Delivery.class, "any"));

            assertThatThrownBy(() -> deliveryService.assignDelivery(CALLER_RIDER_NO, sampleReq()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("동시 배차 충돌")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(deliveryLogRepository, never()).save(any(DeliveryLog.class));
        }

        @Test
        @DisplayName("deliveryNo 자동 생성 형식: 5자리 숫자 + 3자리 영문 (interfaces.md §1.1 박제 형식 예시 일관)")
        void deliveryNo_format() {
            Rider rider = activeRider();
            when(riderRepository.findById(CALLER_RIDER_NO)).thenReturn(Optional.of(rider));
            when(deliveryRepository.saveAndFlush(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

            RiderInternalAssignRes res = deliveryService.assignDelivery(CALLER_RIDER_NO, sampleReq());

            assertThat(res.deliveryNo()).hasSize(8).matches("^[0-9]{5}[A-Z]{3}$");
        }
    }

    @Nested
    @DisplayName("GetRiderInternalStatus (R4 §1.3, 3건)")
    class GetRiderInternalStatus {

        private Rider activeRider() {
            Rider rider = mock(Rider.class);
            lenient().when(rider.getRiderNo()).thenReturn(CALLER_RIDER_NO);
            lenient().when(rider.getStatus()).thenReturn(RiderStatus.ACTIVE);
            return rider;
        }

        @Test
        @DisplayName("진행 중 배달 있음 → currentDeliveryNo 반환")
        void withProgress_returnsCurrentDeliveryNo() {
            Delivery inProgress = deliveryWith(DeliveryStatus.PICKED_UP, CALLER_RIDER_NO);
            Rider rider = activeRider();
            when(riderRepository.findById(CALLER_RIDER_NO)).thenReturn(Optional.of(rider));
            when(deliveryRepository.findFirstByRiderNoAndStatusInOrderByAssignedAtDesc(eq(CALLER_RIDER_NO), anyList()))
                    .thenReturn(Optional.of(inProgress));

            RiderInternalStatusRes res = deliveryService.getRiderInternalStatus(CALLER_RIDER_NO);

            assertThat(res.riderNo()).isEqualTo(CALLER_RIDER_NO);
            assertThat(res.status()).isEqualTo("ACTIVE");
            assertThat(res.currentDeliveryNo()).isEqualTo(DELIVERY_NO);
        }

        @Test
        @DisplayName("진행 중 배달 없음 → currentDeliveryNo null")
        void withoutProgress_returnsNull() {
            Rider rider = activeRider();
            when(riderRepository.findById(CALLER_RIDER_NO)).thenReturn(Optional.of(rider));
            when(deliveryRepository.findFirstByRiderNoAndStatusInOrderByAssignedAtDesc(eq(CALLER_RIDER_NO), anyList()))
                    .thenReturn(Optional.empty());

            RiderInternalStatusRes res = deliveryService.getRiderInternalStatus(CALLER_RIDER_NO);

            assertThat(res.currentDeliveryNo()).isNull();
            assertThat(res.status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("rider 부재 → BusinessException NOT_FOUND")
        void riderNotFound_throwsNotFound() {
            when(riderRepository.findById(CALLER_RIDER_NO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryService.getRiderInternalStatus(CALLER_RIDER_NO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("라이더를 찾을 수 없습니다.")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(deliveryRepository, never())
                    .findFirstByRiderNoAndStatusInOrderByAssignedAtDesc(any(), anyList());
        }
    }

    @Nested
    @DisplayName("Admin 모니터 (6건)")
    class GetMonitor {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("status=null happy: summary 4그룹 + findAll page0")
        void monitor_noFilter_returnsSummaryAndAllDeliveries() {
            when(deliveryRepository.countByStatus(DeliveryStatus.WAITING_ASSIGN)).thenReturn(2L);
            when(deliveryRepository.countByStatus(DeliveryStatus.ASSIGNED)).thenReturn(1L);
            when(deliveryRepository.countByStatus(DeliveryStatus.ARRIVED_AT_STORE)).thenReturn(2L);
            when(deliveryRepository.countByStatus(DeliveryStatus.AWAITING_PICKUP)).thenReturn(0L);
            when(deliveryRepository.countByStatus(DeliveryStatus.PICKED_UP)).thenReturn(3L);
            when(deliveryRepository.countByStatus(DeliveryStatus.DELIVERING)).thenReturn(1L);
            when(deliveryRepository.countByStatus(DeliveryStatus.DELIVERED)).thenReturn(7L);

            Delivery d1 = deliveryWith(DeliveryStatus.ASSIGNED, 5L);
            Delivery d2 = deliveryWith(DeliveryStatus.DELIVERING, 6L);
            when(deliveryRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(d1, d2)));

            RiderInternalMonitorRes res = deliveryService.getMonitor(null, 0);

            assertThat(res.summary().waiting()).isEqualTo(2L);
            assertThat(res.summary().assigned()).isEqualTo(3L); // 1+2+0
            assertThat(res.summary().delivering()).isEqualTo(4L); // 3+1
            assertThat(res.summary().completed()).isEqualTo(7L);
            assertThat(res.deliveries()).hasSize(2);
            assertThat(res.deliveries().get(0).status()).isEqualTo("ASSIGNED");
            assertThat(res.deliveries().get(1).status()).isEqualTo("DELIVERING");

            verify(deliveryRepository, never())
                    .findByStatusIn(any(Collection.class), any(Pageable.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("status=assigned: ASSIGNED+ARRIVED_AT_STORE+AWAITING_PICKUP 3 enum filter")
        void monitor_assignedFilter_passesGroupSet() {
            when(deliveryRepository.countByStatus(any(DeliveryStatus.class))).thenReturn(0L);
            ArgumentCaptor<Collection<DeliveryStatus>> captor = ArgumentCaptor.forClass(Collection.class);
            when(deliveryRepository.findByStatusIn(captor.capture(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            deliveryService.getMonitor("assigned", 0);

            assertThat(captor.getValue()).containsExactlyInAnyOrder(
                    DeliveryStatus.ASSIGNED,
                    DeliveryStatus.ARRIVED_AT_STORE,
                    DeliveryStatus.AWAITING_PICKUP);
            verify(deliveryRepository, never()).findAll(any(Pageable.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("status=DELIVERING(대문자) lowercase 정규화 후 group 매칭")
        void monitor_uppercase_normalizedToLowercase() {
            when(deliveryRepository.countByStatus(any(DeliveryStatus.class))).thenReturn(0L);
            ArgumentCaptor<Collection<DeliveryStatus>> captor = ArgumentCaptor.forClass(Collection.class);
            when(deliveryRepository.findByStatusIn(captor.capture(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            deliveryService.getMonitor("DELIVERING", 0);

            assertThat(captor.getValue()).containsExactlyInAnyOrder(
                    DeliveryStatus.PICKED_UP,
                    DeliveryStatus.DELIVERING);
        }

        @Test
        @DisplayName("invalid status → BusinessException BAD_REQUEST")
        void monitor_invalidStatus_throwsBadRequest() {
            when(deliveryRepository.countByStatus(any(DeliveryStatus.class))).thenReturn(0L);

            assertThatThrownBy(() -> deliveryService.getMonitor("xxx", 0))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("page<0 → BusinessException BAD_REQUEST")
        void monitor_negativePage_throwsBadRequest() {
            assertThatThrownBy(() -> deliveryService.getMonitor(null, -1))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(deliveryRepository, never()).countByStatus(any(DeliveryStatus.class));
        }

        @Test
        @DisplayName("PageRequest size=20 + assignedAt DESC sort 검증")
        void monitor_pageRequestFixedSizeAndSort() {
            when(deliveryRepository.countByStatus(any(DeliveryStatus.class))).thenReturn(0L);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(deliveryRepository.findAll(pageableCaptor.capture()))
                    .thenReturn(new PageImpl<>(List.of()));

            deliveryService.getMonitor(null, 3);

            Pageable captured = pageableCaptor.getValue();
            assertThat(captured.getPageSize()).isEqualTo(20);
            assertThat(captured.getPageNumber()).isEqualTo(3);
            assertThat(captured.getSort().getOrderFor("assignedAt").getDirection())
                    .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
        }
    }
}
