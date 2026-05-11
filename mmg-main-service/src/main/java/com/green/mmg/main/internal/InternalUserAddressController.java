package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.address.UserAddressRepository;
import com.green.mmg.main.address.model.UserAddressRes;
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

}