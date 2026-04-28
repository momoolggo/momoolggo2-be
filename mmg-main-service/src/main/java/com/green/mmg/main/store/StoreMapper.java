package com.green.mmg.main.store;

import com.green.mmg.main.store.model.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper

public interface StoreMapper {
    List<StoreGetRes> findAll(StoreGetReq req);
    StoreOneGetRes findOne(long id);
    List<MenuGetRes> menuAll(long id);
    int getMaxPage(StoreGetReq req);
    List<StoreGetRes> favoriteList( StoreFavoriteReq req);
    int favoriteCount(long id);
    int checkWish(FavoriteToggleReq req);
    int deleteWish(FavoriteToggleReq req);
    int insertWish(FavoriteToggleReq req);
    List<StoreGetRes> searchStore(@Param("searchText") String searchText);
    List<StoreGetRes> findNearby(@Param("lat") double lat, @Param("lng") double lng);

    //가게 리뷰 조회
    List<Map<String, Object>> getStoreReviews(long storeId);
}
