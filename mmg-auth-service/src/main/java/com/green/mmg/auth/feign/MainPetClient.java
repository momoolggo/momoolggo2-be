package com.green.mmg.auth.feign;

import com.green.mmg.auth.feign.dto.PetInitReq;
import com.green.mmg.auth.feign.dto.PetInitRes;
import com.green.mmg.common.dto.ResultResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "mmg-main-pet",
        url = "${feign.main-service.url:http://localhost:8080}"
)
public interface MainPetClient {

    @PostMapping("/internal/pet/init")
    ResultResponse<PetInitRes> initPet(@RequestBody PetInitReq req);
}
