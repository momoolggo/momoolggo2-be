package com.green.mmg.rider.rider;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.config.RiderProperties;
import com.green.mmg.rider.internal.dto.RiderInternalLocationRes;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.RiderProfileReq;
import com.green.mmg.rider.rider.model.RiderProfileRes;
import com.green.mmg.rider.rider.model.VehicleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 라이더 도메인 서비스 — Phase 5-R1 범위.
 *
 * <h3>Q-Status: JwtUser.status 신뢰 X, 매 요청 DB lookup 원칙</h3>
 * 권한 검증이 필요한 endpoint(R5 위치, R6 배달 등)는 본 클래스의 조회 메서드로
 * 매 요청 rider.status를 DB에서 읽음. JwtUser.status는 토큰 발급 시점 동결되어
 * 토글 후 stale 가능 — 신뢰하지 않음.
 *
 * <h3>D11 임시 운영 (admin-service 도입 전)</h3>
 * {@link #joinProfile}에서 RiderProperties.autoApprove true이면 PENDING → ACTIVE
 * 즉시 전환. admin-service approve endpoint 도입 후 임시 블록 + RIDER_AUTO_APPROVE=false.
 * 관련 ADR: docs/adr/rider/ADR-001-service-boundary.md "임시 운영" 섹션.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiderService {

    private final RiderRepository riderRepository;
    private final RiderProperties riderProperties;

    /**
     * 라이더 가입 프로필 등록 (ADR-001 Q1-C — auth signup 후 별도 endpoint).
     *
     * <ol>
     *   <li>중복 가입 방지 (existsByUserNo)</li>
     *   <li>입력 검증 (필수 필드 + vehicleType 화이트리스트)</li>
     *   <li>Rider INSERT (status=PENDING)</li>
     *   <li>D11: autoApprove true 시 ACTIVE 전환 (TODO: admin endpoint 도입 후 제거)</li>
     * </ol>
     *
     * @param callerUserNo SecurityContextHolder 추출 — dto.userNo 신뢰 X (위조 방지)
     */
    @Transactional
    public RiderProfileRes joinProfile(long callerUserNo, RiderProfileReq req) {
        // 1. 중복 가입 방지
        if (riderRepository.existsByUserNo(callerUserNo)) {
            throw new BusinessException("이미 라이더로 등록된 계정입니다.", HttpStatus.CONFLICT);
        }

        // 2. 입력 검증 (auth/main 기존 패턴 — validation starter 미도입)
        validate(req);
        VehicleType vehicleType = parseVehicleType(req.vehicleType());

        // 3. Rider INSERT (status=PENDING)
        Rider rider = new Rider(
                callerUserNo,
                req.licenseNo(),
                req.licenseType(),
                vehicleType,
                req.accountBank(),
                req.accountNo(),
                req.accountHolder()
        );
        rider = riderRepository.save(rider);

        // === 임시: admin-service 미도입 시 자동 ACTIVE (D11 옵션 A-1) ===
        // TODO: admin-service approve endpoint 도입 후 이 블록 제거
        //       + application.yml RIDER_AUTO_APPROVE=false
        // 관련 ADR: docs/adr/rider/ADR-001-service-boundary.md "임시 운영" 섹션
        if (riderProperties.autoApprove()) {
            rider.approve();
            log.debug("D11 auto-approve applied: riderNo={}, userNo={}", rider.getRiderNo(), callerUserNo);
        }

        return RiderProfileRes.from(rider);
    }

    /**
     * 본인 프로필 조회 (GET /api/rider/me).
     * 권한: callerUserNo로 매 요청 DB lookup (Q-Status 원칙).
     */
    @Transactional(readOnly = true)
    public RiderProfileRes findProfile(long callerUserNo) {
        Rider rider = riderRepository.findByUserNo(callerUserNo)
                .orElseThrow(() -> new BusinessException(
                        "라이더 프로필이 등록되지 않았습니다.", HttpStatus.NOT_FOUND));
        return RiderProfileRes.from(rider);
    }

    private void validate(RiderProfileReq req) {
        requireNonBlank(req.licenseNo(), "licenseNo");
        requireNonBlank(req.licenseType(), "licenseType");
        requireNonBlank(req.vehicleType(), "vehicleType");
        requireNonBlank(req.accountBank(), "accountBank");
        requireNonBlank(req.accountNo(), "accountNo");
        requireNonBlank(req.accountHolder(), "accountHolder");
    }

    /** Figma 정정 1 — 배달수단 enum 변환 (R3-a 마이그레이션, valueOf IllegalArgumentException → BusinessException) */
    private static VehicleType parseVehicleType(String value) {
        try {
            return VehicleType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "vehicleType는 WALK/BICYCLE/MOTORBIKE/CAR 중 하나여야 합니다.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(field + "는 필수 입력값입니다.", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 라이더 위치 조회 — interfaces.md §1.2 (Main → Rider).
     *
     * <p>R4 시점 = R5 Redis 인프라 부재 stub. 항상 BusinessException(NOT_FOUND) — interfaces.md §1.2
     * 박제 일관 ("Response 404 — TTL 만료 또는 위치 송신 0회"). R5 진입 시 LocationService 분리 +
     * Redis GET {@code rider:loc:{riderNo}} 호출 후 dto 반환.</p>
     *
     * <p>리턴 타입은 R5 진입 후를 위한 시그니처 박제 — 현재 구현은 throw만.</p>
     */
    public RiderInternalLocationRes getInternalLocation(Long riderNo) {
        throw new BusinessException(
                "위치 송신 0회 또는 TTL 만료 (R5 Redis 인프라 도입 예정).",
                HttpStatus.NOT_FOUND);
    }
}
