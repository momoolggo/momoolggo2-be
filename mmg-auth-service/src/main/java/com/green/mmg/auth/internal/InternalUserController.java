package com.green.mmg.auth.internal;

import com.green.mmg.auth.internal.dto.InternalAdminUserRes;
import com.green.mmg.auth.internal.dto.InternalUserApprovalReq;
import com.green.mmg.auth.internal.dto.InternalUserDetailRes;
import com.green.mmg.auth.internal.dto.InternalUserSuspensionReq;
import com.green.mmg.auth.user.UserRepository;
import com.green.mmg.auth.user.model.User;
import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 서비스 간 통신 (main/rider/admin → auth) 전용 Internal API.
 * Phase 4-A에서 cross-schema JOIN을 Feign으로 풀기 위해 도입.
 *
 * 보안: 현재 permitAll. Phase 4-B에서 Gateway가 외부 /internal/** 요청을 차단할 예정.
 * Phase 6에서 mTLS / service-to-service token 검토.
 *
 * Phase 3-A: MyBatis → JPA 전환 (UserRepository.findBriefByUserNo, findBriefsByUserNos).
 *
 * 응답 형식: 모든 endpoint는 공통 래퍼 ResultResponse<T>로 통일.
 */
@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    /** 단건 조회 — 가게 상세에 사장 이름 등 1명에 대한 fetch */
    @Transactional(readOnly = true)
    @GetMapping("/user/{userNo}")
    public ResultResponse<UserBriefDto> getUser(@PathVariable long userNo) {
        UserBriefDto user = userRepository.findBriefByUserNo(userNo)
                .orElseThrow(() -> new ResourceNotFoundException("user not found: " + userNo));
        return new ResultResponse<>("유저 조회 완료", user);
    }

    /** 가게 검색 API */
    @Transactional(readOnly = true)
    @GetMapping("/owners/search")
    public ResultResponse<List<Long>> searchOwnerNos(@RequestParam(required = false) String userId,
                                                     @RequestParam(required = false) String name) {
        return new ResultResponse<>("검색 완료", userRepository.findOwnerUserNosBySearch(userId, name));
    }

    /** Batch 조회 — N+1 회피용 (1회 호출 최대 100개 권장) */
    @Transactional(readOnly = true)
    @GetMapping("/users")
    public ResultResponse<List<UserBriefDto>> getUsers(@RequestParam("ids") List<Long> userNos) {
        if (userNos == null || userNos.isEmpty()) {
            return new ResultResponse<>("조회 대상 없음", List.of());
        }
        return new ResultResponse<>("유저 배치 조회 완료", userRepository.findBriefsByUserNos(userNos));
    }

    /** 사장 정보 — 현재는 user와 동일 응답. Phase 5에서 OwnerInfoDto로 확장 가능 */
    @Transactional(readOnly = true)
    @GetMapping("/owner/{userNo}")
    public ResultResponse<UserBriefDto> getOwner(@PathVariable long userNo) {
        UserBriefDto owner = userRepository.findBriefByUserNo(userNo)
                .orElseThrow(() -> new ResourceNotFoundException("owner not found: " + userNo));
        return new ResultResponse<>("사장 정보 조회 완료", owner);
    }

    /** 라이더 수 조회 — 관리자 공지 발송 모달에서 전체 라이더 수 표시용 */
    @Transactional(readOnly = true)
    @GetMapping("/rider/count")
    public ResultResponse<Long> getRiderCount() {
        long count = userRepository.countByRole("RIDER");
        return new ResultResponse<>("라이더 수 조회 완료", count);
    }

    /** 라이더 userNo 목록 — Main에서 구역별 라이더 수 집계 시 사용 */
    @Transactional(readOnly = true)
    @GetMapping("/rider/user-nos")
    public ResultResponse<List<Long>> getRiderUserNos() {
        return new ResultResponse<>("라이더 userNo 조회 완료",
                userRepository.findAllUserNosByRole("RIDER"));
    }

    /** 유저 상세 조회 — 관리자 고객 상세 모달용 (아이디, 가입일, 친환경점수 포함) */
    @Transactional(readOnly = true)
    @GetMapping("/user/{userNo}/detail")
    public ResultResponse<InternalUserDetailRes> getUserDetail(@PathVariable long userNo) {
        User user = userRepository.findInternalUserDetailByUserNo(userNo)
                .orElseThrow(() -> new ResourceNotFoundException("user not found: " + userNo));
        return new ResultResponse<>("유저 상세 조회 완료", new InternalUserDetailRes(
                user.getUserNo(),
                user.getUserId(),
                user.getName(),
                user.getTel(),
                user.getGreen(),
                user.getCreatedAt(),
                user.getStatus()
        ));
    }
    /** 회원 목록 조회 */
    @Transactional(readOnly = true)
    @GetMapping("/users/list")
    public ResultResponse<Page<InternalAdminUserRes>> getUserList(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue =  "0") int page) {
        Page<User> users = userRepository.findAllByRole(role, PageRequest.of(page,10));
        return new ResultResponse<>("회원 목록 조회완료", users.map(InternalAdminUserRes::from));
    }

    /** 승인대기 회원 조회 */
    @Transactional(readOnly = true)
    @GetMapping("/users/pending")
    public ResultResponse<List<InternalAdminUserRes>> getPendingUsers(){
        return new ResultResponse<>("승인대기 회원 조회완료",
                userRepository.findPendingUsers()
                        .stream()
                        .map(InternalAdminUserRes::from)
                        .toList());
    }

    /** 승인 반려 상태 변경완료 */
    @Transactional
    @PatchMapping("/user/{userNo}/approval")
    public ResultResponse<Void> updateApproval( @PathVariable long userNo, @RequestBody InternalUserApprovalReq req){
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new ResourceNotFoundException("user not found: " + userNo));

            // PENDING은 ADR-001 (D) 라이더 통합 승인 보상용 (admin-service RiderApprovalService 호출). 일반 회원 승인 흐름에서는 사용 X.
            if (!"ACTIVE".equals(req.getStatus()) && !"REJECTED".equals(req.getStatus()) && !"PENDING".equals(req.getStatus())) {
                throw new BusinessException("status는 ACTIVE/REJECTED/PENDING(보상용)만 가능합니다.", HttpStatus.BAD_REQUEST);

            }

            user.setStatus(req.getStatus());
            user.setProcessMemo(req.getReason());

            return new ResultResponse<>("승인상태 변경 완료", null);
    }

    /**
     * 회원 삭제 — admin cascade 흐름 (admin-service AdminUserController.deleteUser 호출 체인).
     * MSA 박제: rider.rider는 별 schema라 DB 자동 cascade X. admin이 rider 먼저 삭제 후 본 endpoint 호출.
     */
    @Transactional
    @DeleteMapping("/user/{userNo}")
    public ResultResponse<Void> deleteUser(@PathVariable long userNo) {
        if (!userRepository.existsById(userNo)) {
            throw new ResourceNotFoundException("user not found: " + userNo);
        }
        userRepository.deleteById(userNo);
        return new ResultResponse<>("회원 삭제 완료", null);
    }

    /** 계정 정지 완료 */
    @Transactional
    @PatchMapping("/user/{userNo}/suspension")
    public ResultResponse<Void> suspendUser(@PathVariable long userNo, @RequestBody InternalUserSuspensionReq req){
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new ResourceNotFoundException("user not found: " + userNo));
        user.setStatus("SUSPENDED");

        if (req.getDays() != null && req.getDays() > 0) {
            Date until = Date.from(
                    LocalDate.now()
                            .plusDays(req.getDays())
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
            );
            user.setSuspensionUntil(until);
        } else {
            user.setSuspensionUntil(null);
        }

        user.setProcessMemo(req.getReason());

        return new ResultResponse<>("계정 정지 완료", null);
    }

    @Transactional
    @PatchMapping("/user/{userNo}/suspension/release")
    public ResultResponse<Void> releaseSuspension( @PathVariable long userNo){
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new ResourceNotFoundException("user not found: " + userNo));

        user.setStatus("ACTIVE");
        user.setSuspensionUntil(null);

        return new ResultResponse<>("계정 정지 해제 완료", null);
    }


    /** 일별 신규 가입자 수 — 관리자 대시보드 일별 통계 카드용 */
    @Transactional(readOnly = true)
    @GetMapping("/stats/new-users")
    public ResultResponse<Long> getTodayNewUsers() {
        Date start = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        return new ResultResponse<>("일별 신규 가입자 조회 완료",
                userRepository.countByCreatedAtBetween(start, end));
    }

    @Transactional(readOnly = true)
    @GetMapping("/stats/new-users/range")
    public ResultResponse<Long> getNewUsersByRange(@RequestParam String start,
                                                   @RequestParam String end) {
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        Date from = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date to = Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        return new ResultResponse<>("기간별 신규 가입자 조회 완료",
                userRepository.countByCreatedAtBetween(from, to));
    }

}