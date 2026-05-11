package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.address.UserAddressRepository;
import com.green.mmg.main.feign.AuthFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/rider")
public class InternalRiderController {

    private final AuthFeignClient authFeignClient;
    private final UserAddressRepository userAddressRepository;

    @Transactional(readOnly = true)
    @GetMapping("/count")
    public ResultResponse<Long> getRiderCount(
            @RequestParam(required = false) String district) {

        if (district == null) {
            Long total = authFeignClient.getRiderCount().getResultData();
            return new ResultResponse<>("라이더 수 조회 완료", total);
        }

        List<Long> riderUserNos = authFeignClient.getRiderUserNos().getResultData();

        if (riderUserNos == null || riderUserNos.isEmpty()) {
            return new ResultResponse<>("구역별 라이더 수 조회 완료", 0L);
        }

        long count = userAddressRepository.countDistinctByUserNosAndDistrict(riderUserNos, district);
        return new ResultResponse<>("구역별 라이더 수 조회 완료", count);
    }

}
