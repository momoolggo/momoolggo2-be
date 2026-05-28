package com.green.mmg.admin.delivery;

import com.green.mmg.admin.dto.feign.RiderProfileRes;
import com.green.mmg.admin.feign.RiderFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 라이더 신원 승인 + 회원 삭제 cascade.
 *
 * <p>2026-05-28 트랙 — 라이더 가입 신원 승인 흐름 복원. PENDING → ACTIVE 단순 위임.
 * (이전: SSE 자동화 트랙(2026-05-21)에서 영구 폐기 박제. 2026-05-28 트랙에서 사용자 명시 요청으로 복원.)</p>
 *
 * <p>회원 삭제 cascade는 ADR-001 (D) 박제 일관 — admin이 user 삭제 전 호출.</p>
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

    /** 라이더 승인 위임 — rider /internal/rider/{userNo}/approve. NOT_FOUND/CONFLICT는 rider Provider 박제. */
    public RiderProfileRes approve(Long userNo) {
        return riderFeignClient.approveRider(userNo);
    }

    /** 라이더 목록 — status 필터 통과 (null/전체/PENDING/ACTIVE/EATING/SUSPENDED). */
    public List<RiderProfileRes> list(String status) {
        return riderFeignClient.getRiderList(status);
    }
}
