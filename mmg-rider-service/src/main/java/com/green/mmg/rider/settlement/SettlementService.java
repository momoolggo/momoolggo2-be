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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * R7 라이더 정산 — ADR-007 박제 일관.
 *
 * <ul>
 *   <li>{@link #findByRiderAndPeriod} — 라이더 본인 정산 내역 (기간 옵션)</li>
 *   <li>{@link #getAccount} / {@link #updateAccount} — 정산 계좌 (Rider entity)</li>
 *   <li>{@link #calculate} — admin 트리거 (Internal) 주간 집계</li>
 *   <li>{@link #confirm} — admin 트리거 (Internal) PENDING → CONFIRMED</li>
 *   <li>{@link #findPending} — admin 모니터</li>
 * </ul>
 *
 * <p>산출 공식 (ADR-007 line 88-93):
 * gross = totalBaseFee + totalExtraFee /
 * commission = gross * COMMISSION_RATE /
 * tax = (gross - commission) * TAX_RATE /
 * payout = gross - commission - tax - INSURANCE_PER_WEEK.</p>
 *
 * <p>거리 산출 = Haversine 직선 (ADR-007 line 54 A 채택). pickup → delivery 좌표.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SettlementService {

    /** Q-Commission-Value (가) — ADR 예시 박제. 운영 진입 시 학원 정책으로 갱신. */
    private static final double COMMISSION_RATE = 0.10;
    private static final double TAX_RATE = 0.033;
    private static final int INSURANCE_PER_WEEK = 5000;

    /** 지구 반지름 (m) — Haversine 공식 표준. */
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private final SettlementRepository settlementRepository;
    private final DeliveryRepository deliveryRepository;
    private final RiderRepository riderRepository;

    /** GET /api/rider/settlement — 라이더 본인 정산 내역 (기간 옵션, 기본 12주). */
    @Transactional(readOnly = true)
    public List<SettlementRowRes> findByRiderAndPeriod(Long callerUserNo, LocalDate from, LocalDate to) {
        Rider rider = riderByUserNo(callerUserNo);
        LocalDate today = LocalDate.now();
        LocalDate fromDate = from != null ? from : today.minusWeeks(12);
        LocalDate toDate = to != null ? to : today;
        if (toDate.isBefore(fromDate)) {
            throw new BusinessException(
                    "to는 from 이후여야 합니다 (from=" + fromDate + ", to=" + toDate + ").",
                    HttpStatus.BAD_REQUEST);
        }
        return settlementRepository
                .findByRiderNoAndPeriodStartBetweenOrderByPeriodStartDesc(
                        rider.getRiderNo(), fromDate, toDate)
                .stream().map(SettlementRowRes::from).toList();
    }

    /** GET /api/rider/settlement/account */
    @Transactional(readOnly = true)
    public AccountRes getAccount(Long callerUserNo) {
        Rider rider = riderByUserNo(callerUserNo);
        return new AccountRes(rider.getAccountBank(), rider.getAccountNo(), rider.getAccountHolder());
    }

    /** PUT /api/rider/settlement/account */
    public AccountRes updateAccount(Long callerUserNo, AccountReq req) {
        if (isBlank(req.accountBank()) || isBlank(req.accountNo()) || isBlank(req.accountHolder())) {
            throw new BusinessException(
                    "은행/계좌번호/예금주는 모두 필수입니다.", HttpStatus.BAD_REQUEST);
        }
        Rider rider = riderByUserNo(callerUserNo);
        rider.updateAccount(req.accountBank(), req.accountNo(), req.accountHolder());
        return new AccountRes(rider.getAccountBank(), rider.getAccountNo(), rider.getAccountHolder());
    }

    /**
     * Internal — 주간 정산 집계 (D10-b admin 트리거). 멱등 처리: 동일 주 이미 INSERT시 기존 행 반환.
     *
     * <p>전체 라이더 순회 → 각 라이더의 해당 기간 DELIVERED 배달 집계 → settlement INSERT.</p>
     */
    public List<SettlementRowRes> calculate(LocalDate periodStart, LocalDate periodEnd) {
        if (periodStart == null || periodEnd == null) {
            throw new BusinessException(
                    "periodStart/periodEnd는 필수입니다.", HttpStatus.BAD_REQUEST);
        }
        if (periodEnd.isBefore(periodStart)) {
            throw new BusinessException(
                    "periodEnd는 periodStart 이후여야 합니다.", HttpStatus.BAD_REQUEST);
        }
        LocalDateTime fromTs = periodStart.atStartOfDay();
        LocalDateTime toTs = periodEnd.plusDays(1).atStartOfDay();

        List<Rider> riders = riderRepository.findAll();
        List<SettlementRowRes> result = new java.util.ArrayList<>();
        for (Rider rider : riders) {
            // 멱등: 동일 주 이미 INSERT시 skip + 기존 반환
            settlementRepository.findByRiderNoAndPeriodStartAndPeriodEnd(
                    rider.getRiderNo(), periodStart, periodEnd)
                    .ifPresentOrElse(
                            existing -> result.add(SettlementRowRes.from(existing)),
                            () -> result.add(SettlementRowRes.from(
                                    calculateAndSave(rider.getRiderNo(), periodStart, periodEnd, fromTs, toTs))));
        }
        return result;
    }

    private Settlement calculateAndSave(Long riderNo, LocalDate periodStart, LocalDate periodEnd,
                                         LocalDateTime fromTs, LocalDateTime toTs) {
        List<Delivery> rows = deliveryRepository
                .findByRiderNoAndStatusAndDeliveredAtBetweenOrderByDeliveredAtDesc(
                        riderNo, DeliveryStatus.DELIVERED, fromTs, toTs);

        int deliveryCount = rows.size();
        int totalBaseFee = rows.stream().mapToInt(d -> nz(d.getBaseFee())).sum();
        int totalExtraFee = rows.stream().mapToInt(d -> nz(d.getExtraFee())).sum();
        int totalDistanceM = rows.stream().mapToInt(this::distanceM).sum();

        int gross = totalBaseFee + totalExtraFee;
        int commission = (int) Math.round(gross * COMMISSION_RATE);
        int tax = (int) Math.round((gross - commission) * TAX_RATE);
        int insurance = deliveryCount > 0 ? INSURANCE_PER_WEEK : 0;  // 미운행 주는 0
        int payout = Math.max(0, gross - commission - tax - insurance);

        Settlement s = new Settlement(
                riderNo, periodStart, periodEnd,
                deliveryCount, totalDistanceM, totalBaseFee, totalExtraFee,
                commission, tax, insurance, payout);
        return settlementRepository.save(s);
    }

    /** Internal — PENDING → CONFIRMED. admin 검토 후 호출. */
    public SettlementRowRes confirm(Long settlementNo, Long adminNo) {
        if (adminNo == null) {
            throw new BusinessException(
                    "adminNo는 필수입니다 (감사 추적).", HttpStatus.BAD_REQUEST);
        }
        Settlement s = settlementRepository.findById(settlementNo)
                .orElseThrow(() -> new BusinessException(
                        "정산을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (s.getStatus() != SettlementStatus.PENDING) {
            throw new BusinessException(
                    "PENDING 상태에서만 confirm 가능: " + s.getStatus(), HttpStatus.CONFLICT);
        }
        s.confirm(adminNo, LocalDateTime.now());
        return SettlementRowRes.from(s);
    }

    /** Internal — admin 모니터 PENDING 목록. */
    @Transactional(readOnly = true)
    public List<SettlementRowRes> findPending() {
        return settlementRepository.findByStatusOrderByPeriodStartDesc(SettlementStatus.PENDING)
                .stream().map(SettlementRowRes::from).toList();
    }

    // ─── 도구 ────────────────────────────────────────────────────────

    private Rider riderByUserNo(Long callerUserNo) {
        return riderRepository.findByUserNo(callerUserNo)
                .orElseThrow(() -> new BusinessException(
                        "라이더 프로필이 등록되지 않았습니다.", HttpStatus.NOT_FOUND));
    }

    private static int nz(Integer v) { return v != null ? v : 0; }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    /**
     * Haversine 직선 거리 (m). pickup → delivery lat/lng 사용.
     * 좌표 null 시 0 반환 (부분 데이터 결손 허용).
     */
    int distanceM(Delivery d) {
        Double lat1 = d.getPickupLat(), lng1 = d.getPickupLng();
        Double lat2 = d.getDeliveryLat(), lng2 = d.getDeliveryLng();
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) return 0;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (int) Math.round(EARTH_RADIUS_M * c);
    }
}
