package com.green.mmg.admin.delivery;

import com.green.mmg.admin.feign.RiderFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 회원 삭제 cascade — admin이 user 삭제 시 rider 행도 함께 정리.
 *
 * <p>SSE 자동화 트랙(2026-05-21) 정정 — 라이더 신원 승인/제재 흐름 폐기 후 본 클래스는 회원 삭제 cascade 단일 책임.
 * (이전: ADR-001 (D) 라이더 통합 승인 + 보상 패턴. 폐기 — auto-approve true로 가입 시 즉시 ACTIVE 박제.)</p>
 *
 * <p>Phase 6+ 클래스 이름 변경 후보 (RiderCascadeService 등) — 본 트랙에서는 호출처 영향 최소화 위해 그대로.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiderApprovalService {

    private final RiderFeignClient riderFeignClient;

    /**
     * admin 회원 삭제 cascade — admin이 user 삭제 전 호출.
     * userNo 매칭 rider 행 삭제 (행 없으면 0 반환, 일반 회원이라 skip).
     * 삭제 실패 시 throw → AdminUserController가 auth 삭제 미실행 (안전).
     */
    public long deleteByUserNoIfRider(Long userNo) {
        return riderFeignClient.deleteRiderByUserNo(userNo);
    }
}
