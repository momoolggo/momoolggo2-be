package com.green.mmg.admin.delivery;

import com.green.mmg.admin.dto.feign.RiderApproveReq;
import com.green.mmg.admin.dto.feign.RiderProfileRes;
import com.green.mmg.admin.dto.feign.UserApprovalReq;
import com.green.mmg.admin.feign.AuthFeignClient;
import com.green.mmg.admin.feign.RiderFeignClient;
import com.green.mmg.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ADR-001 (D) — 라이더 통합 승인 (2026-05-19 신설, supersedes (C) 승인 분리).
 *
 * <p>admin 1회 클릭으로 auth.user.status + rider.status 동시 PENDING → ACTIVE.
 * Feign 동기 조율 + try-catch 보상 패턴. Phase 6+ Outbox/Saga 도입 전 MVP.</p>
 *
 * <p>책임 분리:
 * <ul>
 *   <li>RiderApprovalController: HTTP 진입 + ResultResponse 응답</li>
 *   <li>RiderApprovalService (본 클래스): Feign 호출 조율 + 보상 처리</li>
 *   <li>auth-service InternalUserController.updateApproval: ACTIVE/REJECTED/PENDING(보상용)</li>
 *   <li>rider-service RiderInternalController.approve: PENDING → ACTIVE 전이 검증</li>
 * </ul></p>
 *
 * <p>한정 권한 적용 — 본 작업 종료 후 admin-service 수정 권한 원위치.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiderApprovalService {

    private final AuthFeignClient authFeignClient;
    private final RiderFeignClient riderFeignClient;

    /**
     * 라이더 통합 승인 — admin /admin/rider 화면 진입 (선택적 별 경로).
     *
     * <ol>
     *   <li>riderNo → userNo 매핑 (getRiderList 활용, MVP 단순)</li>
     *   <li>auth.user.status PENDING → ACTIVE</li>
     *   <li>rider.status PENDING → ACTIVE (실패 시 auth 보상 호출)</li>
     * </ol>
     */
    public void approveRider(Long riderNo, RiderApproveReq req) {
        Long userNo = findUserNoByRiderNo(riderNo);

        // auth 승인 (실패 시 throw — admin 5xx, rider 미호출)
        authFeignClient.updateApproval(userNo, new UserApprovalReq("ACTIVE", "라이더 통합 승인 (ADR-001 D)"));

        // rider 승인 — 실패 시 auth 보상
        try {
            riderFeignClient.approveRider(riderNo, req);
        } catch (Exception e) {
            log.warn("rider 승인 실패 — auth 보상 호출 시도. riderNo={}, userNo={}, ex={}",
                    riderNo, userNo, e.getMessage());
            compensateAuth(userNo);
            throw e;
        }
    }

    /**
     * /admin/user 회원관리 승인 진입점 — ADR-001 (D) 메인 흐름 (2026-05-19 정정).
     *
     * <p>AdminUserController.updateApproval에서 auth 승인 직후 호출. userNo로 rider 매핑 시도:
     * <ul>
     *   <li>라이더면 (rider 행 존재) → rider.status PENDING → ACTIVE + 실패 시 auth 보상</li>
     *   <li>일반 회원이면 (rider 행 없음) → skip (auth만으로 끝)</li>
     * </ul></p>
     *
     * <p>본 메서드는 throw 시 admin에게 5xx 전달. auth 보상 후 propagate.</p>
     */
    public void approveByUserNoIfRider(Long userNo) {
        Long riderNo = findRiderNoByUserNo(userNo);
        if (riderNo == null) {
            return;  // 일반 회원 (CUSTOMER/OWNER) — auth.user.status 단독 처리로 끝
        }
        // 라이더 — rider.status도 PENDING → ACTIVE
        try {
            riderFeignClient.approveRider(riderNo, new RiderApproveReq(null));
        } catch (Exception e) {
            log.warn("rider 승인 실패 — auth 보상 호출 시도. userNo={}, riderNo={}, ex={}",
                    userNo, riderNo, e.getMessage());
            compensateAuth(userNo);
            throw e;
        }
    }

    /** riderNo로 userNo 매핑 (전체 라이더 list 조회 후 filter). */
    private Long findUserNoByRiderNo(Long riderNo) {
        List<RiderProfileRes> riders = riderFeignClient.getRiderList(null);
        return riders.stream()
                .filter(r -> riderNo.equals(r.riderNo()))
                .map(RiderProfileRes::userNo)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "라이더를 찾을 수 없습니다: riderNo=" + riderNo,
                        HttpStatus.NOT_FOUND));
    }

    /** userNo로 riderNo 매핑 (전체 라이더 list 조회 후 filter). null = 라이더 아님(일반 회원). */
    private Long findRiderNoByUserNo(Long userNo) {
        List<RiderProfileRes> riders = riderFeignClient.getRiderList(null);
        return riders.stream()
                .filter(r -> userNo.equals(r.userNo()))
                .map(RiderProfileRes::riderNo)
                .findFirst()
                .orElse(null);
    }

    /**
     * AdminUserController.deleteUser cascade 보완 — admin이 user 삭제 전 호출.
     * userNo 매칭 rider 행 삭제 (행 없으면 0 반환, 일반 회원이라 skip).
     * 삭제 실패 시 throw → AdminUserController가 auth 삭제 미실행 (안전).
     */
    public long deleteByUserNoIfRider(Long userNo) {
        return riderFeignClient.deleteRiderByUserNo(userNo);
    }

    /** auth 보상 호출 — PENDING 복귀. 보상도 실패 시 log.error만 (admin 수동 정정 안내). */
    private void compensateAuth(Long userNo) {
        try {
            authFeignClient.updateApproval(userNo,
                    new UserApprovalReq("PENDING", "라이더 승인 실패 보상 (ADR-001 D)"));
            log.info("auth 보상 호출 성공 (PENDING 복귀). userNo={}", userNo);
        } catch (Exception compensateErr) {
            log.error("auth 보상 호출 실패 — admin 수동 정정 필요. userNo={}", userNo, compensateErr);
        }
    }
}
