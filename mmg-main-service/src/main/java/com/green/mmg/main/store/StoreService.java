package com.green.mmg.main.store;

import com.green.mmg.main.store.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {
    private final StoreMapper storeMapper;

    public List<StoreGetRes> storeListGet(StoreGetReq req){
        return storeMapper.findAll(req);
    }

    public StoreOneGetRes storeOneGet(long id){
        return storeMapper.findOne(id);
    }

    public int  getMaxPage (StoreGetReq req){
        return storeMapper.getMaxPage(req);
    }

    public List<MenuGetRes> menuListGet(long id){ return storeMapper.menuAll(id);}

    @Transactional
    public boolean wishToggle(FavoriteToggleReq req){
        int check =storeMapper.checkWish(req);
        System.out.println("dd"+req.getStoreId());
        System.out.println(req.getUserNo());
        if(check>0){storeMapper.deleteWish(req); return false;}
        else{storeMapper.insertWish(req); return true;}
    }

    public boolean checkWish(FavoriteToggleReq req){
        return storeMapper.checkWish(req)>0;
    }

    public Map<String, Object> getWishListResponse(StoreFavoriteReq req) {
        Map<String, Object> response = new HashMap<>();
        // 1. 찜 목록 리스트 가져오기 (LIMIT 적용됨)
        List<StoreGetRes> list = storeMapper.favoriteList(req);
        // 2. 전체 찜 개수 가져오기 (LIMIT 없음)
        int totalCount = storeMapper.favoriteCount(req.getUserNo());

        response.put("list", list);
        response.put("totalCount", totalCount);
        return response;
    }

    public List<StoreGetRes> storeSearchList(String searchText) {
        if( searchText == null || searchText.trim().isEmpty()) {
            return List.of();

        }
        return storeMapper.searchStore(searchText);
    }

    public List<StoreGetRes> findNearbyStores(double lat, double lng) {
        return storeMapper.findNearby(lat, lng);
    }

    //가게 리뷰 조회
    public List<Map<String, Object>> getStoreReviews(long storeId) {
        return storeMapper.getStoreReviews(storeId);
    }

}
