package com.green.mmg.main.store;


import com.green.mmg.main.internal.dto.InternalOwnerApprovalDetailRes;
import com.green.mmg.main.internal.dto.InternalStoreListRes;
import com.green.mmg.main.store.model.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper

public interface StoreMapper {
    List<StoreGetRes> findAll(StoreGetReq req);
    StoreOneGetRes findOne(long id);
    List<MenuGetRes> menuAll(long id);
    int getMaxPage(StoreGetReq req);

    /** Phase 3-B-2: JOIN+LIMIT 잔존 (LikedStoreRepository와 공존). favoriteCount/checkWish/insertWish/deleteWish는 JPA로 이동. */
    List<StoreGetRes> favoriteList(StoreFavoriteReq req);

    List<StoreGetRes> searchStore(@Param("searchText") String searchText);
    List<MenuGetRes> menuSearchInStore(@Param("storeId") Long storeId, @Param("searchText") String searchText);
    List<StoreGetRes> findNearby(@Param("lat") double lat, @Param("lng") double lng);

    //가게 리뷰 조회
    List<Map<String, Object>> getStoreReviews(long storeId);

    //관리자 사장 가입승인 모달
    InternalOwnerApprovalDetailRes findOwnerApprovalDetail(@Param("ownerNo") long ownerNo);

    //관리자 오늘 가게 등록 수
    long countTodayStores(@Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    //관리자 가게관리 목록
    List<InternalStoreListRes> findInternalStoreList(@Param("startIdx") int startIdx,
                                                     @Param("size") int size,
                                                     @Param("date") String date);

    long countInternalStoreList(@Param("date") String date);

    //관리자 회원관리 가게 주소
    String findStoreLocationByOwnerNo(@Param("ownerNo") long ownerNo);

    //기간별 가게 수
    long countStoresByCreatedAtBetween(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

}
