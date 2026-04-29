package com.green.mmg.main.store;

import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.feign.AuthFeignClient;
import com.green.mmg.main.store.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {
    private final StoreMapper storeMapper;
    private final LikedStoreRepository likedStoreRepository;  // Phase 3-B-2: 단순 CRUD
    private final AuthFeignClient authFeignClient;   // Phase 4-A: cross-schema user JOIN 대체

    public List<StoreGetRes> storeListGet(StoreGetReq req){
        return storeMapper.findAll(req);
    }

    public StoreOneGetRes storeOneGet(long id){
        StoreOneGetRes res = storeMapper.findOne(id);
        if (res != null && res.getOwnerId() != null) {
            UserBriefDto owner = authFeignClient.getOwner(res.getOwnerId());
            res.setOwnerName(owner.getName());
        }
        return res;
    }

    public int  getMaxPage (StoreGetReq req){
        return storeMapper.getMaxPage(req);
    }

    public List<MenuGetRes> menuListGet(long id){ return storeMapper.menuAll(id);}

    @Transactional
    public boolean wishToggle(FavoriteToggleReq req){
        // Phase 3-B-2: MyBatis 4 SQL → JPA Repository
        boolean liked = likedStoreRepository.existsByUserNoAndStoreId(req.getUserNo(), req.getStoreId());
        if (liked) {
            likedStoreRepository.deleteByUserNoAndStoreId(req.getUserNo(), req.getStoreId());
            return false;
        } else {
            // saveAndFlush: 같은 트랜잭션 내 후속 MyBatis SELECT(favoriteList JOIN)에 즉시 가시화
            likedStoreRepository.saveAndFlush(new com.green.mmg.main.store.model.LikedStore(
                    req.getUserNo(), req.getStoreId(), null));
            return true;
        }
    }

    public boolean checkWish(FavoriteToggleReq req){
        return likedStoreRepository.existsByUserNoAndStoreId(req.getUserNo(), req.getStoreId());
    }

    public Map<String, Object> getWishListResponse(StoreFavoriteReq req) {
        Map<String, Object> response = new HashMap<>();
        // 1. 찜 목록 리스트 (JOIN+LIMIT) — MyBatis 잔존 (하이브리드 공존)
        List<StoreGetRes> list = storeMapper.favoriteList(req);
        // 2. 전체 찜 개수 — JPA Repository
        int totalCount = (int) likedStoreRepository.countByUserNo(req.getUserNo());

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

    //가게 리뷰 조회 — Phase 4-A: user JOIN 대신 Feign batch로 userName 합성
    public List<Map<String, Object>> getStoreReviews(long storeId) {
        List<Map<String, Object>> rows = storeMapper.getStoreReviews(storeId);
        if (rows.isEmpty()) return rows;

        List<Long> userNos = rows.stream()
                .map(r -> ((Number) r.get("userNo")).longValue())
                .distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = authFeignClient.getUsers(userNos).stream()
                .collect(Collectors.toMap(UserBriefDto::getUserNo, UserBriefDto::getName));

        rows.forEach(r -> r.put("userName",
                nameMap.getOrDefault(((Number) r.get("userNo")).longValue(), "")));
        return rows;
    }

}
