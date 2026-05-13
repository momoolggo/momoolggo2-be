package com.green.mmg.main.address;

import com.green.mmg.main.address.model.AddressSearchRes;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressSearchController {

    private final AddressSearchService addressSearchService;

    // ── 주소 검색 GET /api/address/search?query=xxx
    @GetMapping("/search")
    public ResultResponse<List<AddressSearchRes>> search(@RequestParam String query) {
        return new ResultResponse<>("주소 검색 성공", addressSearchService.search(query));
    }

    // ── Reverse Geocoding GET /api/address/reverse?lat=xx&lng=xx
    @GetMapping("/reverse")
    public ResultResponse<AddressSearchRes> reverse(@RequestParam double lat,
                                                    @RequestParam double lng) {
        return new ResultResponse<>("주소 변환 성공", addressSearchService.reverseGeocode(lat, lng));
    }
}