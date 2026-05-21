package com.green.mmg.rider.rider;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.RiderProfileReq;
import com.green.mmg.rider.rider.model.RiderProfileRes;
import com.green.mmg.rider.rider.model.RiderStatus;
import com.green.mmg.rider.rider.model.VehicleType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 라이더 도메인 서비스 — Phase 5-R1 범위.
 *
 * <h3>Q-Status: JwtUser.status 신뢰 X, 매 요청 DB lookup 원칙</h3>
 * 권한 검증이 필요한 endpoint(R5 위치, R6 배달 등)는 본 클래스의 조회 메서드로
 * 매 요청 rider.status를 DB에서 읽음. JwtUser.status는 토큰 발급 시점 동결되어
 * 토글 후 stale 가능 — 신뢰하지 않음.
 *
 * <h3>SSE 자동화 트랙(2026-05-21) — 라이더 신원 승인/제재 흐름 영구 폐기</h3>
 * 가입 시점에 Rider 생성자에서 status=ACTIVE 직접 박제 (D11 auto-approve toggle 폐기).
 * RiderStatus.PENDING/SUSPENDED enum 값은 DB 정합 + DeliveryService/WorkSessionService/LocationService 거부 검증 의도로 보존.
 */
@Service
@RequiredArgsConstructor
public class RiderService {

    private final RiderRepository riderRepository;

    /**
     * 라이더 가입 프로필 등록 (ADR-001 Q1-C — auth signup 후 별도 endpoint).
     *
     * <ol>
     *   <li>중복 가입 방지 (existsByUserNo)</li>
     *   <li>입력 검증 (필수 필드 + vehicleType 화이트리스트)</li>
     *   <li>Rider INSERT — 생성자에서 status=ACTIVE 직접 박제 (SSE 자동화 트랙, 2026-05-21)</li>
     * </ol>
     *
     * @param callerUserNo SecurityContextHolder 추출 — dto.userNo 신뢰 X (위조 방지)
     */
    @Transactional
    public RiderProfileRes joinProfile(long callerUserNo, RiderProfileReq req) {
        if (riderRepository.existsByUserNo(callerUserNo)) {
            throw new BusinessException("이미 라이더로 등록된 계정입니다.", HttpStatus.CONFLICT);
        }

        validate(req);
        VehicleType vehicleType = parseVehicleType(req.vehicleType());

        Rider rider = new Rider(
                callerUserNo,
                req.licenseNo(),
                req.licenseType(),
                vehicleType,
                req.accountBank(),
                req.accountNo(),
                req.accountHolder(),
                req.phone()
        );
        rider = riderRepository.save(rider);
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

    /**
     * admin 라이더 목록 조회 (interfaces.md §3.5, Q-A1 (라++) Group 8 신설 2026-05-17).
     *
     * <p>{@code status} null이면 전체, 명시되면 필터. 학원 발표 MVP — List 반환 (Page 미사용, case-#36 자가 정정).</p>
     */
    @Transactional(readOnly = true)
    public List<RiderProfileRes> listRiders(RiderStatus status) {
        List<Rider> riders = (status == null)
                ? riderRepository.findAll()
                : riderRepository.findByStatusOrderByRiderNoDesc(status);
        return riders.stream().map(RiderProfileRes::from).toList();
    }

    /**
     * admin cascade 삭제 — user_no 매칭 rider 행 삭제 (없으면 skip).
     * ADR-001 (D) 보완: MSA 박제로 DB 자동 cascade X, application 레벨 보장.
     */
    @Transactional
    public long deleteByUserNoIfExists(Long userNo) {
        return riderRepository.deleteByUserNo(userNo);
    }

    private void validate(RiderProfileReq req) {
        requireNonBlank(req.licenseNo(), "licenseNo");
        requireNonBlank(req.licenseType(), "licenseType");
        requireNonBlank(req.vehicleType(), "vehicleType");
        // account_* nullable=true (Rider.java:50-57 + rider-schema.sql:23-25 일관). 가입 시 미입력 허용, 마이페이지에서 등록 (사용자 결정 A1' 정합성).
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

    // R5 진입(2026-05-11)으로 getInternalLocation은 LocationService로 이전.
}
