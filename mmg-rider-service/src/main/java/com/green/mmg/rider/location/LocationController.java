package com.green.mmg.rider.location;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.rider.location.dto.LocationUpdateReq;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 5-R5: 라이더 위치 송신 — ADR-005 박제 일관.
 *
 * <p>{@code PUT /api/rider/location} — RIDER role 인증 필수 (RiderSecurityConfig).
 * 본인 위치만 송신 — callerUserNo는 SecurityContextHolder 추출 (dto.userNo 신뢰 X).</p>
 */
@RestController
@RequestMapping("/api/rider/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PutMapping
    public ResultResponse<Void> updateMyLocation(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody LocationUpdateReq req
    ) {
        locationService.publishLocation(principal.getSignedUserNo(), req);
        return new ResultResponse<>("위치 갱신 완료", null);
    }
}
