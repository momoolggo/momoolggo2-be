package com.green.mmg.auth.address;

import com.green.mmg.auth.address.model.UserAddressReq;
import com.green.mmg.auth.address.model.UserAddressRes;
import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/address")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    // ── 주소 추가 POST /api/user/address
    @PostMapping
    public ResultResponse<Void> save(@AuthenticationPrincipal UserPrincipal principal,
                                     @RequestBody UserAddressReq req) {
        userAddressService.save(principal.getSignedUserNo(), req);
        return new ResultResponse<>("주소 추가 성공", null);
    }

    // ── 주소 목록 조회 GET /api/user/address
    @GetMapping
    public ResultResponse<List<UserAddressRes>> findAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        return new ResultResponse<>("주소 조회 성공",
                userAddressService.findAll(principal.getSignedUserNo()));
    }

    // ── 주소 수정 PUT /api/user/address
    @PutMapping
    public ResultResponse<Void> update(@AuthenticationPrincipal UserPrincipal principal,
                                       @RequestBody UserAddressReq req) {
        userAddressService.update(principal.getSignedUserNo(), req);
        return new ResultResponse<>("주소 수정 성공", null);
    }

    // ── 주소 삭제 DELETE /api/user/address/{addressId}
    @DeleteMapping("/{addressId}")
    public ResultResponse<Void> delete(@PathVariable long addressId) {
        userAddressService.delete(addressId);
        return new ResultResponse<>("주소 삭제 성공", null);
    }

    //기본배송지로 수정
    @PutMapping("/{addressId}/default")
    public ResultResponse<Void> setDefault(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long addressId) {
        userAddressService.setDefault(principal.getSignedUserNo(), addressId);
        return new ResultResponse<>("기본 배송지 변경 성공", null);
    }
}
