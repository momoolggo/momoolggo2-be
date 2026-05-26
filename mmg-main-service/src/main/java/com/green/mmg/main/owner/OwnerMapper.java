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

    // ========== Phase 2-Backfill-D-bis: 권한 검증 헬퍼 (4개) ==========
    // 각 ID에서 store.owner_id를 조회 → Service의 verify*Owner가 callerOwnerNo와 비교

    /** store_id로 owner_id 조회 (없으면 null) */
    Long findStoreOwnerByStoreId(@Param("storeId") long storeId);

    /** order_id → orders.store_id → store.owner_id (없으면 null) */
    Long findStoreOwnerByOrderId(@Param("orderId") long orderId);

    /** menu_id → menu.category_id → menu_category.store_id → store.owner_id (없으면 null) */
    Long findStoreOwnerByMenuId(@Param("menuId") long menuId);

    /** category_id → menu_category.store_id → store.owner_id (없으면 null) */
    Long findStoreOwnerByCategoryId(@Param("categoryId") long categoryId);

    // ========== Phase 5+ 작업 A Group 4: 라이더 배차 정보 조회 ==========
    // Q-A9.c MyBatis 박제 일관 (Phase 3-D "Store/Owner MyBatis 영구 유지").
    // 분류 B 자율: RiderAssignReq 직접 매핑 (StoreInfoDto 신설 X — DTO 중복 회피, case-#34 영역 분리 별 결).
    // case-#34-후속 일관: orders.delivery_lat/lng + customer_phone + extra_fee 부재 → NULL/0 패스 (Q-A9.e (나)).

    /**
     * 점주 수락 시점 라이더 배차 정보 조회 — interfaces.md §1.1 박제 14 필드 매핑.
     * riderNo = NULL (라이더 풀 모델, Q-A9.a (β+δ)).
     */
    com.green.mmg.main.feign.model.RiderAssignReq findStoreInfoByOrderId(@Param("orderId") long orderId);
}