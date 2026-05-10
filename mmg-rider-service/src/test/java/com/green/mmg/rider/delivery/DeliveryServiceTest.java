package com.green.mmg.rider.delivery;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.delivery.model.ActorRole;
import com.green.mmg.rider.delivery.model.Delivery;
import com.green.mmg.rider.delivery.model.DeliveryLog;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import com.green.mmg.rider.rider.RiderRepository;
import com.green.mmg.rider.rider.model.Rider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

}
