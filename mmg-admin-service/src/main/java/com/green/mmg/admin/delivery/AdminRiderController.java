package com.green.mmg.admin.delivery;

import com.green.mmg.admin.dto.feign.RiderProfileRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * admin 라이더 관리 컨트롤러 — 2026-05-28 트랙 가입 신원 승인 흐름 복원.
 *
 * <p>AdminDeliveryController는 배달 관제 + 라이더 공지 박제 일관 — 라이더 가입 승인은 책임 분리.
 * SecurityConfig {@code /api/admin/**} ADMIN role 자동 가드.</p>
 *
 * <p>승인 흐름: admin 화면에서 status=PENDING 목록 조회 → 면허증 사진 확인 → POST /approve →
 * rider PENDING → ACTIVE → 라이더 화면 차단 해제 (RiderLayout.vue:31-33 박제 일관).</p>
 */
@RestController
@RequestMapping("/api/admin/rider")
@RequiredArgsConstructor
public class AdminRiderController {

    private final RiderApprovalService riderApprovalService;

    /**
     * GET /api/admin/rider/list?status=PENDING — 라이더 목록 조회.
     * status null = 전체. PENDING/ACTIVE/EATING/SUSPENDED 필터.
     */
    @GetMapping("/list")
    public List<RiderProfileRes> list(@RequestParam(required = false) String status) {
        return riderApprovalService.list(status);
    }

    /**
     * POST /api/admin/rider/{userNo}/approve — 라이더 승인 (PENDING → ACTIVE).
     * NOT_FOUND (라이더 부재) / CONFLICT (이미 ACTIVE/EATING/SUSPENDED) 분기는 rider Provider 박제.
     */
    @PostMapping("/{userNo}/approve")
    public RiderProfileRes approve(@PathVariable Long userNo) {
        return riderApprovalService.approve(userNo);
    }
}
