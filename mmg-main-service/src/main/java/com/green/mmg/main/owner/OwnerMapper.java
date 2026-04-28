package com.green.mmg.main.owner;


import com.green.mmg.main.owner.model.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OwnerMapper {

    // 가게 등록
    int registerStore(OwnerStoreRegReq dto);

    // 가게 기본 정보 수정
    int updateStore(OwnerStoreUpdateReq dto);

    // 가게 운영정보 수정
    int updateStoreStatus(OwnerStoreUpdateStatusReq dto);

    // 가게 정보 조회
    OwnerStoreRes getStoreById(Long storeId);

    // 가게 삭제
    int deleteStore(Long storeId);

    // 내 가게 1개 조회 (매출 등 내부용)
    OwnerStoreRes getMyStore(long ownerNo);

    // 내 가게 목록 조회 (여러 가게 지원)
    List<OwnerStoreRes> getMyStores(long ownerNo);

    // ========== 주문 관련 ==========

    List<OwnerOrderRes> getOrders(@Param("storeId") Long storeId,
                                  @Param("state") Integer state,
                                  @Param("date") String date);

    int updateOrderState(OwnerOrderStateReq req);

    void deleteOrderDetail(Long orderId);

    void deleteOrder(Long orderId);

    // ========== 메뉴 관련 ==========

    int registerMenu(OwnerMenuRegReq dto);

    OwnerMenuRes getMenuById(Long menuId);

    int updateMenu(OwnerMenuUpdateReq dto);

    int deleteMenu(Long menuId);

    List<OwnerMenuRes> getMenusByStoreId(Long storeId);

    // ========== 카테고리 관련 ==========

    void registerDefaultMenuCategory(long userId);

    void registerStoreCategory(@Param("userId") long userId, @Param("categoryId") long categoryId);

    List<Map<String, Object>> getCategoriesByStoreId(Long storeId);
    void addCategory(@Param("storeId") Long storeId, @Param("category") String category);
    void updateCategory(@Param("categoryId") Long categoryId, @Param("category") String category);
    void deleteCategory(Long categoryId);

    // ========== 매출 관련 ==========

    OwnerSalesStatsRes getSalesStats(@Param("storeId") long storeId, @Param("period") String period);
    List<OwnerSalesRankingRes> getSalesRanking(@Param("storeId") long storeId, @Param("period") String period);
}