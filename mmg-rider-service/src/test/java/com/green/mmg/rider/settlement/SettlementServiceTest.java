package com.green.mmg.rider.settlement;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.delivery.DeliveryRepository;
import com.green.mmg.rider.delivery.model.Delivery;
import com.green.mmg.rider.delivery.model.DeliveryStatus;
import com.green.mmg.rider.rider.RiderRepository;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.settlement.dto.AccountReq;
import com.green.mmg.rider.settlement.dto.AccountRes;
import com.green.mmg.rider.settlement.dto.SettlementRowRes;
import com.green.mmg.rider.settlement.model.Settlement;
import com.green.mmg.rider.settlement.model.SettlementStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SettlementService 단위 — R7 (가짜 0건 원칙).
 *
 * <p>10건: 산출 공식 3 + 계좌 3 + history 2 + calculate 2건 = ADR-007 박제 그룹별 검증.</p>
 */
@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    private static final Long CALLER_USER_NO = 42L;
    private static final Long CALLER_RIDER_NO = 5L;

    @Mock private SettlementRepository settlementRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private RiderRepository riderRepository;

    @InjectMocks private SettlementService settlementService;

    private Rider rider;

    @BeforeEach
    void setUp() {
        rider = mock(Rider.class);
        lenient().when(rider.getRiderNo()).thenReturn(CALLER_RIDER_NO);
        lenient().when(rider.getAccountBank()).thenReturn("국민");
        lenient().when(rider.getAccountNo()).thenReturn("110-123-456789");
        lenient().when(rider.getAccountHolder()).thenReturn("홍길동");
    }

    // ─── 산출 공식 (ADR-007 line 88-93) ────────────────────

    @Test
    @DisplayName("calculate: gross=100000 → commission=10000 / tax=2970 / insurance=5000 / payout=82030")
    void calculate_formula_baseCase() {
        when(riderRepository.findAll()).thenReturn(List.of(rider));
        when(settlementRepository.findByRiderNoAndPeriodStartAndPeriodEnd(any(), any(), any()))
                .thenReturn(Optional.empty());

        Delivery d = mockDelivery(50000, 0, null, null, null, null);
        when(deliveryRepository.findByRiderNoAndStatusAndDeliveredAtBetweenOrderByDeliveredAtDesc(
                eq(CALLER_RIDER_NO), eq(DeliveryStatus.DELIVERED), any(), any()))
                .thenReturn(List.of(d, d));  // 2건 × 50000 = gross 100000

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        when(settlementRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        settlementService.calculate(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 10));

        Settlement saved = captor.getValue();
        assertThat(saved.getTotalBaseFee()).isEqualTo(100000);
        assertThat(saved.getTotalExtraFee()).isEqualTo(0);
        assertThat(saved.getCommission()).isEqualTo(10000);     // 100000 * 0.10
        assertThat(saved.getTax()).isEqualTo(2970);             // (100000 - 10000) * 0.033 = 2970
        assertThat(saved.getInsurance()).isEqualTo(5000);
        assertThat(saved.getPayout()).isEqualTo(82030);         // 100000 - 10000 - 2970 - 5000
        assertThat(saved.getDeliveryCount()).isEqualTo(2);
        assertThat(saved.getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    @Test
    @DisplayName("calculate: 미운행 주(0건) → insurance=0 / payout=0")
    void calculate_noDeliveries_insuranceZero() {
        when(riderRepository.findAll()).thenReturn(List.of(rider));
        when(settlementRepository.findByRiderNoAndPeriodStartAndPeriodEnd(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(deliveryRepository.findByRiderNoAndStatusAndDeliveredAtBetweenOrderByDeliveredAtDesc(
                eq(CALLER_RIDER_NO), eq(DeliveryStatus.DELIVERED), any(), any()))
                .thenReturn(List.of());

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        when(settlementRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        settlementService.calculate(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 10));

        Settlement saved = captor.getValue();
        assertThat(saved.getDeliveryCount()).isEqualTo(0);
        assertThat(saved.getInsurance()).isEqualTo(0);
        assertThat(saved.getPayout()).isEqualTo(0);
    }

    @Test
    @DisplayName("Haversine: 서울 ↔ 부산 위경도 ≈ 325km")
    void haversine_seoulToBusan() {
        Delivery d = mockDelivery(0, 0, 37.5665, 126.9780, 35.1796, 129.0756);  // 서울/부산
        int distance = settlementService.distanceM(d);
        // 서울-부산 약 325km, ±2km 허용
        assertThat(distance).isBetween(323_000, 327_000);
    }

    // ─── 계좌 ──────────────────────────────────────

    @Test
    @DisplayName("getAccount: Rider 컬럼 노출")
    void getAccount_returnsRiderColumns() {
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));
        AccountRes res = settlementService.getAccount(CALLER_USER_NO);
        assertThat(res.accountBank()).isEqualTo("국민");
        assertThat(res.accountNo()).isEqualTo("110-123-456789");
        assertThat(res.accountHolder()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("updateAccount: rider.updateAccount 호출 + 새 값 응답")
    void updateAccount_invokesEntityMethod() {
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));
        when(rider.getAccountBank()).thenReturn("신한");  // updateAccount 호출 후 갱신 가정
        when(rider.getAccountNo()).thenReturn("123-456-789");
        when(rider.getAccountHolder()).thenReturn("이몽룡");

        AccountRes res = settlementService.updateAccount(CALLER_USER_NO,
                new AccountReq("신한", "123-456-789", "이몽룡"));

        verify(rider).updateAccount("신한", "123-456-789", "이몽룡");
        assertThat(res.accountBank()).isEqualTo("신한");
    }

    @Test
    @DisplayName("updateAccount: blank 필드 → BAD_REQUEST")
    void updateAccount_blankRejected() {
        assertThatThrownBy(() -> settlementService.updateAccount(CALLER_USER_NO,
                new AccountReq("", "123", "홍길동")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(riderRepository);
    }

    // ─── findByRiderAndPeriod ──────────────────────

    @Test
    @DisplayName("findByRiderAndPeriod: 기본 12주 자동 + DESC 정렬 반환")
    void findByRiderAndPeriod_default12Weeks() {
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));
        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        when(settlementRepository.findByRiderNoAndPeriodStartBetweenOrderByPeriodStartDesc(
                eq(CALLER_RIDER_NO), fromCaptor.capture(), any()))
                .thenReturn(List.of());

        settlementService.findByRiderAndPeriod(CALLER_USER_NO, null, null);

        assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.now().minusWeeks(12));
    }

    @Test
    @DisplayName("findByRiderAndPeriod: from>to → BAD_REQUEST")
    void findByRiderAndPeriod_fromAfterTo_badRequest() {
        when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));
        assertThatThrownBy(() -> settlementService.findByRiderAndPeriod(
                CALLER_USER_NO, LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ─── confirm + 멱등 ───────────────────────────

    @Test
    @DisplayName("confirm: PENDING → CONFIRMED 전환 + adminNo/confirmedAt 기록")
    void confirm_pendingToConfirmed() {
        Settlement s = mock(Settlement.class);
        when(s.getStatus()).thenReturn(SettlementStatus.PENDING);
        when(settlementRepository.findById(99L)).thenReturn(Optional.of(s));

        settlementService.confirm(99L, 7L);

        verify(s).confirm(eq(7L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("confirm: adminNo null → BAD_REQUEST (W-2 fix 감사 추적)")
    void confirm_adminNoNull_badRequest() {
        assertThatThrownBy(() -> settlementService.confirm(99L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(settlementRepository);
    }

    @Test
    @DisplayName("confirm: CONFIRMED 재호출 → CONFLICT")
    void confirm_alreadyConfirmed_conflict() {
        Settlement s = mock(Settlement.class);
        when(s.getStatus()).thenReturn(SettlementStatus.CONFIRMED);
        when(settlementRepository.findById(99L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> settlementService.confirm(99L, 7L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(s, never()).confirm(any(), any());
    }

    @Test
    @DisplayName("calculate 멱등: 동일 주 이미 INSERT시 save 미호출 + 기존 행 반환")
    void calculate_idempotent_skipsSave() {
        when(riderRepository.findAll()).thenReturn(List.of(rider));
        Settlement existing = mock(Settlement.class);
        when(settlementRepository.findByRiderNoAndPeriodStartAndPeriodEnd(any(), any(), any()))
                .thenReturn(Optional.of(existing));

        settlementService.calculate(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 10));

        verify(settlementRepository, never()).save(any());
        verify(deliveryRepository, never()).findByRiderNoAndStatusAndDeliveredAtBetweenOrderByDeliveredAtDesc(
                any(), any(), any(), any());
    }

    private Delivery mockDelivery(int baseFee, int extraFee,
                                   Double pLat, Double pLng, Double dLat, Double dLng) {
        Delivery d = mock(Delivery.class);
        lenient().when(d.getBaseFee()).thenReturn(baseFee);
        lenient().when(d.getExtraFee()).thenReturn(extraFee);
        lenient().when(d.getPickupLat()).thenReturn(pLat);
        lenient().when(d.getPickupLng()).thenReturn(pLng);
        lenient().when(d.getDeliveryLat()).thenReturn(dLat);
        lenient().when(d.getDeliveryLng()).thenReturn(dLng);
        return d;
    }
}
