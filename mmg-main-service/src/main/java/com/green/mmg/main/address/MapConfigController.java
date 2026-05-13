package com.green.mmg.main.address;

import com.green.mmg.common.dto.ResultResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/map")
public class MapConfigController {

    @Value("${naver.map-client-id}")
    private String mapClientId;

    // 프론트에서 지도 SDK 로드할 때 필요한 key 반환
    // GET /api/map/key
    @GetMapping("/key")
    public ResultResponse<String> getMapKey() {
        return new ResultResponse<>("success", mapClientId);
    }
}