package com.green.mmg.main.feign;

import com.green.mmg.main.feign.model.RiderAssignReq;
import com.green.mmg.main.feign.model.RiderAssignRes;
import com.green.mmg.main.feign.model.RiderLocationRes;
import com.green.mmg.main.feign.model.RiderStatusRes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "mmg-rider-service",
        url = "${feign.rider-service.url:http://localhost:8082}"
)
public interface RiderFeignClient {

    @PostMapping("/internal/rider/assign")
    RiderAssignRes assignRider(@RequestBody RiderAssignReq req);

    @GetMapping("/internal/rider/{riderNo}/location")
    RiderLocationRes getRiderLocation(
            @PathVariable("riderNo") Long riderNo
    );

    @GetMapping("/internal/rider/{riderNo}/status")
    RiderStatusRes getRiderStatus(
            @PathVariable("riderNo") Long riderNo
    );
}