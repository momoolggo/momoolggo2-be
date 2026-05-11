package com.green.mmg.rider.location;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.internal.dto.RiderInternalLocationRes;
import com.green.mmg.rider.location.dto.LocationUpdateReq;
import com.green.mmg.rider.location.model.RiderLocation;
import com.green.mmg.rider.rider.RiderRepository;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.RiderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Phase 5-R5: 라이더 위치 도메인 서비스 — ADR-005/006 박제 일관.
 *
 * <p>두 진입점:
 * <ul>
 *   <li>{@link #publishLocation} — 라이더 본인 위치 송신 (PUT /api/rider/location)</li>
 *   <li>{@link #getInternalLocation} — Main/Admin 위치 조회 (GET /internal/rider/{riderNo}/location)</li>
 * </ul></p>
 *
 * <p>{@link #PUBLISHABLE_STATUSES}: ACTIVE / EATING만 송신 가능 (PENDING/SUSPENDED 거부 — ADR-005 §위치 송신 박제).
 * Q-Status 일관: rider.status는 매 요청 DB lookup (RiderService 패턴 일관).</p>
 *
 * <p>D1 throw: Redis 다운 시 RuntimeException 그대로 → 5xx (RedisRiderLocationStore 일관).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private static final Set<RiderStatus> PUBLISHABLE_STATUSES =
            EnumSet.of(RiderStatus.ACTIVE, RiderStatus.EATING);

    private final RiderRepository riderRepository;
    private final RiderLocationStore riderLocationStore;

    /**
     * 라이더 본인 위치 송신 — ADR-005 §위치 송신 박제.
     *
     * @param callerUserNo SecurityContextHolder 추출 (dto.userNo 위조 방지, R3-b 패턴 일관)
     * @param req lat/lng (서버 시각으로 updatedAt 박제)
     */
    @Transactional(readOnly = true)
    public void publishLocation(long callerUserNo, LocationUpdateReq req) {
        validate(req);

        Rider rider = riderRepository.findByUserNo(callerUserNo)
                .orElseThrow(() -> new BusinessException(
                        "라이더 프로필이 등록되지 않았습니다.", HttpStatus.NOT_FOUND));

        if (!PUBLISHABLE_STATUSES.contains(rider.getStatus())) {
            throw new BusinessException(
                    "위치 송신 가능 상태가 아닙니다 (현재: " + rider.getStatus() + ").",
                    HttpStatus.BAD_REQUEST);
        }

        RiderLocation location = new RiderLocation(req.lat(), req.lng(), LocalDateTime.now());
        riderLocationStore.save(rider.getRiderNo(), location);
    }

    /**
     * 위치 조회 — interfaces.md §1.2 (Main/Admin → Rider).
     *
     * <p>Redis GET → 부재(NULL/만료) 시 NOT_FOUND throw — ADR-005 §위치 조회 박제 일관.
     * R4 시점 RiderService.getInternalLocation stub 통합 (R5 진입 시 본 메서드로 대체).</p>
     */
    public RiderInternalLocationRes getInternalLocation(long riderNo) {
        return riderLocationStore.get(riderNo)
                .map(loc -> new RiderInternalLocationRes(
                        riderNo, loc.lat(), loc.lng(), loc.updatedAt()))
                .orElseThrow(() -> new BusinessException(
                        "위치 송신 0회 또는 TTL 만료.", HttpStatus.NOT_FOUND));
    }

    private void validate(LocationUpdateReq req) {
        if (req.lat() == null || req.lng() == null) {
            throw new BusinessException("lat/lng는 필수입니다.", HttpStatus.BAD_REQUEST);
        }
        if (req.lat() < -90 || req.lat() > 90) {
            throw new BusinessException("lat 범위 위반 (-90~90).", HttpStatus.BAD_REQUEST);
        }
        if (req.lng() < -180 || req.lng() > 180) {
            throw new BusinessException("lng 범위 위반 (-180~180).", HttpStatus.BAD_REQUEST);
        }
    }
}
