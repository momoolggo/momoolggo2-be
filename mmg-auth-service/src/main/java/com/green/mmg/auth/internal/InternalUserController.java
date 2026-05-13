package com.green.mmg.auth.internal;

import com.green.mmg.auth.internal.dto.InternalUserDetailRes;
import com.green.mmg.auth.user.UserRepository;
import com.green.mmg.auth.user.model.User;
import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
                user.getCreatedAt()
        ));
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
}