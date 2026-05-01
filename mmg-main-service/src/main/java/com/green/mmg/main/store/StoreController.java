package com.green.mmg.main.store;

import com.green.mmg.main.store.model.*;
import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreController {
    private final StoreService storeService;

    @GetMapping  //가게 전체목록
    public ResultResponse<?> StoreListGet(@ModelAttribute StoreGetReq req){
        List<StoreGetRes> result = storeService.storeListGet(req);
        return new ResultResponse<>("ㅇ", result);
    }
    @GetMapping("max_page")//가게 최대페이지
    public ResultResponse<?> getMaxPage(@ModelAttribute StoreGetReq req){
        int result = storeService.getMaxPage(req);
        return new ResultResponse<>("",result);
    }


    @GetMapping("/{id}") //가게 상세정보
    public ResultResponse<?> StoreOneGet(@PathVariable long id){
        StoreOneGetRes result = storeService.storeOneGet(id);
        return new ResultResponse<>("",result);
    }

    @GetMapping("/menu/{id}") //가게 메뉴목록
    public ResultResponse<?> MenuListGet(@PathVariable long id){
        List<MenuGetRes> result= storeService.menuListGet(id);
        return new ResultResponse<>("", result);
    }
    @GetMapping("/searchstore") // 메뉴 가게 검색
    public ResultResponse<?> StoreSearchList(@RequestParam ("search_text") String searchText){
        List<StoreGetRes> result = storeService.storeSearchList(searchText);
        return new ResultResponse<>("", result);
    }

    @GetMapping("/favorite/check") //가게찜여부 확인
    public ResultResponse<?> wishCheck(@AuthenticationPrincipal UserPrincipal principal,
                                       @ModelAttribute FavoriteToggleReq req){
        boolean result = storeService.checkWish(principal.getSignedUserNo(), req);
        return new ResultResponse<>("",result);
    }

    @PostMapping("favorite")  //가게찜 토글
    public ResultResponse<?> wishToggle(@AuthenticationPrincipal UserPrincipal principal,
                                        @RequestBody FavoriteToggleReq req){
        boolean result = storeService.wishToggle(principal.getSignedUserNo(), req);
        return new ResultResponse<>("",result);
    }

    @GetMapping("/favorite") //찜한 가게 목록
    public ResultResponse<?> wishListGet(@AuthenticationPrincipal UserPrincipal principal,
                                         @ModelAttribute StoreFavoriteReq req) {
        Map<String, Object> result = storeService.getWishListResponse(principal.getSignedUserNo(), req);

        return new ResultResponse<>("찜 목록 조회 성공", result);
    }

    @GetMapping("/nearby")
    public ResultResponse<?> getNearbyStores(
            @RequestParam double lat,
            @RequestParam double lng) {
        List<StoreGetRes> result = storeService.findNearbyStores(lat, lng);
        return new ResultResponse<>("주변 가게 조회 성공", result);
    }

    //리뷰 조회
    @GetMapping("/{id}/review")
    public ResultResponse<?> getStoreReviews(@PathVariable long id) {
        List<Map<String, Object>> result = storeService.getStoreReviews(id);
        return new ResultResponse<>("리뷰 조회 성공", result);
    }
}

