package com.green.mmg.main.order;

import com.green.mmg.main.order.model.*;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Phase 3-C-3 정리 후 잔존 (4 SQL):
 * <ul>
 *   <li>복잡 영구: findOrdersByUserId(JOIN+DATE_FORMAT+서브쿼리), orderHistoryDetail(복잡 포맷),
 *       calSumOrder(store cross-table UPDATE)</li>
 *   <li>외부 도메인 (Phase 3-D 정리 예정): findDefaultAddress(address 테이블)</li>
 * </ul>
 *
 * <p>Phase 3-C-3에서 외부 호출 3개(findByOrderId, findUserNoByOrderId, updateState) 제거 →
 * PaymentService.confirmPayment가 OrderRepository + dirty checking 사용.</p>
 */
@Mapper
public interface OrderMapper {

    List<OrderHistoryDto> findOrdersByUserId(OrderHistoryReq req);
    OrderHistoryDto orderHistoryDetail(long id);
    int calSumOrder(long id);

    OrderAddressInfo findDefaultAddress(@org.apache.ibatis.annotations.Param("userNo") Long userNo);
}
