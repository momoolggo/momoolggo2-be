package com.green.mmg.main.store;

import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.feign.AuthFeignClient;
import com.green.mmg.main.store.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {
    private final StoreMapper storeMapper;
    private final LikedStoreRepository likedStoreRepository;  // Phase 3-B-2: 단순 CRUD
    private final AuthFeignClient authFeignClient;   // Phase 4-A: cross-schema user JOIN 대체

    @Transactional(readOnly = true)
    public List<StoreGetRes> storeListGet(StoreGetReq req){
        return storeMapper.findAll(req);
    }

    @Transactional(readOnly = true)
    public StoreOneGetRes storeOneGet(long id){
        StoreOneGetRes res = storeMapper.findOne(id);
        if (res != null && res.getOwnerId() != null) {
            UserBriefDto owner = authFeignClient.getOwner(res.getOwnerId());
            if (owner == null) {
                throw new BusinessException("사장 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
            }
            res.setOwnerName(owner.getName());
        }
        return res;
    }

    @Transactional(readOnly = true)
    public int  getMaxPage (StoreGetReq req){
        return storeMapper.getMaxPage(req);
    }

    @Transactional(readOnly = true)
    public List<MenuGetRes> menuListGet(long id){ return storeMapper.menuAll(id);}

    @Transactional
    public boolean wishToggle(long callerUserNo, FavoriteToggleReq req){
        // Phase 3-Backfill-A-2: dto.userNo 위조 방지 (옵션 B — 불일치 시 FORBIDDEN throw)
        if (!Objects.equals(req.getUserNo(), callerUserNo)) {
            throw new BusinessException("자신의 계정으로만 찜할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
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

    @Transactional(readOnly = true)
    public boolean checkWish(long callerUserNo, FavoriteToggleReq req){
        if (!Objects.equals(req.getUserNo(), callerUserNo)) {
            throw new BusinessException("자신의 계정으로만 조회할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        return likedStoreRepository.existsByUserNoAndStoreId(req.getUserNo(), req.getStoreId());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getWishListResponse(long callerUserNo, StoreFavoriteReq req) {
        if (!Objects.equals(req.getUserNo(), callerUserNo)) {
            throw new BusinessException("본인 찜 목록만 조회 가능합니다.", HttpStatus.FORBIDDEN);
        }
        Map<String, Object> response = new HashMap<>();
        // 1. 찜 목록 리스트 (JOIN+LIMIT) — MyBatis 잔존 (하이브리드 공존)
        List<StoreGetRes> list = storeMapper.favoriteList(req);
        // 2. 전체 찜 개수 — JPA Repository
        int totalCount = (int) likedStoreRepository.countByUserNo(req.getUserNo());

        response.put("list", list);
        response.put("totalCount", totalCount);
        return response;
    }

    @Transactional(readOnly = true)
    public List<StoreGetRes> storeSearchList(String searchText) {
        if( searchText == null || searchText.trim().isEmpty()) {
            return List.of();

        }
        return storeMapper.searchStore(searchText);
    }


    public List<MenuGetRes> menuSearchInStore(long storeId, String searchText) {
        if(  searchText == null || searchText.trim().isEmpty()) {
            return List.of();
        }
        return storeMapper.menuSearchInStore(storeId,searchText);
    }

   @Transactional(readOnly = true)
    public List<StoreGetRes> findNearbyStores(double lat, double lng) {
        return storeMapper.findNearby(lat, lng);
    }

    //가게 리뷰 조회 — Phase 4-A: user JOIN 대신 Feign batch로 userName 합성
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStoreReviews(long storeId) {
        List<Map<String, Object>> rows = storeMapper.getStoreReviews(storeId);
        if (rows.isEmpty()) return rows;

        List<Long> userNos = rows.stream()
                .map(r -> ((Number) r.get("userNo")).longValue())
                .distinct().collect(Collectors.toList());
        // Phase 3-Backfill-A-4: Feign batch null 처리 (storeOneGet 패턴 전파)
        // null 응답 시 빈 Map → 누락된 userNo는 review의 userName을 빈 문자열로 fallback
        List<UserBriefDto> users = authFeignClient.getUsers(userNos);
        Map<Long, String> nameMap = (users == null ? List.<UserBriefDto>of() : users).stream()
                .collect(Collectors.toMap(UserBriefDto::getUserNo, UserBriefDto::getName));

        rows.forEach(r -> r.put("userName",
                nameMap.getOrDefault(((Number) r.get("userNo")).longValue(), "")));
        return rows;
    }

}
