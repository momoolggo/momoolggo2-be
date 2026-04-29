package com.green.mmg.main.order;

import com.green.mmg.main.order.model.*;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Phase 3-C-1 잔존:
 * <ul>
 *   <li>복잡 SQL (영구): findOrdersByUserId(JOIN+DATE_FORMAT+서브쿼리), orderHistoryDetail(복잡 포맷),
 *       calSumOrder(store cross-table UPDATE)</li>
 *   <li>외부 도메인: findDefaultAddress(address 테이블) — Phase 3-D Address Repository 신설 시 정리</li>
 *   <li>외부 호출 (Phase 3-C-3에서 정리): findByOrderId, findUserNoByOrderId, updateState</li>
 * </ul>
 *
 * <p>제거됨: insertOrder, insertOrderDetail, deleteOrder, maxHistoryPage, findItemsByOrderId
 * (5 SQL → JPA Repository 이전).</p>
 */
@Mapper
public interface OrderMapper {

    // ── 복잡 SQL 영구 잔존
    List<OrderHistoryDto> findOrdersByUserId(OrderHistoryReq req);
    OrderHistoryDto orderHistoryDetail(long id);
    int calSumOrder(long id);

    // ── 외부 도메인 (address) — Phase 3-D 정리 예정
    OrderAddressInfo findDefaultAddress(@org.apache.ibatis.annotations.Param("userNo") Long userNo);

    // ── 외부 호출 (PaymentService) — Phase 3-C-3 정리 예정
    long findUserNoByOrderId(long orderId);
    Orders findByOrderId(Long orderId);
    int updateState(OrderState orderstate);
}
