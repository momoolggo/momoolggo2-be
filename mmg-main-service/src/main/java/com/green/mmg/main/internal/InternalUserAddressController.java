package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.address.UserAddressRepository;
import com.green.mmg.main.address.model.UserAddressRes;
import com.green.mmg.main.internal.dto.UserDefaultAddressRes;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/user")
public class InternalUserAddressController {
    private final UserAddressRepository userAddressRepository;

    @Transactional(readOnly = true)
    @GetMapping("/{userNo}/address")
    public ResultResponse<List<UserAddressRes>> getUserAddresses(@PathVariable long userNo) {
        return new ResultResponse<>("주소 조회 완료", userAddressRepository.findAllByUserNo(userNo));
    }

    @Transactional(readOnly = true)
    @GetMapping("/addresses/default")
    public ResultResponse<List<UserDefaultAddressRes>> getDefaultAddresses(@RequestParam List<Long> userNos) {

        if(userNos == null || userNos.isEmpty()) {
            return new ResultResponse<>("주소 목록 조회", List.of());
        }

        return new ResultResponse<>("주소 목록 조회", userAddressRepository.findDefaultAddressesByUserNos(userNos));
    }

}