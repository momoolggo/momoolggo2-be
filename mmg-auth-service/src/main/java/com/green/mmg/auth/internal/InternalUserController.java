package com.green.mmg.auth.internal;

import com.green.mmg.auth.user.UserRepository;
import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 서비스 간 통신 (main/rider/admin → auth) 전용 Internal API.
 * Phase 4-A에서 cross-schema JOIN을 Feign으로 풀기 위해 도입.
 *
 * 보안: 현재 permitAll. Phase 4-B에서 Gateway가 외부 /internal/** 요청을 차단할 예정.
 * Phase 6에서 mTLS / service-to-service token 검토.
 *
 * Phase 3-A: MyBatis → JPA 전환 (UserRepository.findBriefByUserNo, findBriefsByUserNos).
 */
@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    /** 단건 조회 — 가게 상세에 사장 이름 등 1명에 대한 fetch */
    @Transactional(readOnly = true)
    @GetMapping("/user/{userNo}")
    public UserBriefDto getUser(@PathVariable long userNo) {
        return userRepository.findBriefByUserNo(userNo)
                .orElseThrow(() -> new ResourceNotFoundException("user not found: " + userNo));
    }

    /** Batch 조회 — N+1 회피용 (1회 호출 최대 100개 권장) */
    @Transactional(readOnly = true)
    @GetMapping("/users")
    public List<UserBriefDto> getUsers(@RequestParam("ids") List<Long> userNos) {
        if (userNos == null || userNos.isEmpty()) return List.of();
        return userRepository.findBriefsByUserNos(userNos);
    }

    /** 사장 정보 — 현재는 user와 동일 응답. Phase 5에서 OwnerInfoDto로 확장 가능 */
    @Transactional(readOnly = true)
    @GetMapping("/owner/{userNo}")
    public UserBriefDto getOwner(@PathVariable long userNo) {
        return userRepository.findBriefByUserNo(userNo)
                .orElseThrow(() -> new ResourceNotFoundException("owner not found: " + userNo));
    }
}
