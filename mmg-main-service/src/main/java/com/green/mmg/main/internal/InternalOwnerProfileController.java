package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.ownerprofile.OwnerProfile;
import com.green.mmg.main.ownerprofile.OwnerProfileService;
import com.green.mmg.main.ownerprofile.dto.OwnerProfileCreateReq;
import com.green.mmg.main.ownerprofile.dto.OwnerProfileDetailRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/owner-profile")
@RequiredArgsConstructor
public class InternalOwnerProfileController {

    private final OwnerProfileService ownerProfileService;

    @PostMapping
    public ResultResponse<Void> create(@RequestBody OwnerProfileCreateReq req) {
        ownerProfileService.create(req);
        return new ResultResponse<>("사장 프로필 저장 완료", null);
    }

    @GetMapping("/{userNo}")
    public ResultResponse<OwnerProfileDetailRes> getByUserNo(@PathVariable Long userNo) {
        OwnerProfile profile = ownerProfileService.getByUserNo(userNo);
        return new ResultResponse<>("사장 프로필 조회 완료", OwnerProfileDetailRes.from(profile));
    }
}