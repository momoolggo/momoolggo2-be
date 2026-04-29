package com.green.mmg.main.order;

import com.green.mmg.main.order.model.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Phase 3-D 정리 후 최종 잔존 (3 SQL — 모두 복잡 영구):
 * <ul>
 *   <li>findOrdersByUserId: JOIN+DATE_FORMAT+서브쿼리(hasReview)</li>
 *   <li>orderHistoryDetail: JOIN+복잡 DATE_FORMAT</li>
 *   <li>calSumOrder: store cross-table UPDATE 서브쿼리 (Store 도메인 경계)</li>
 * </ul>
 *
 * <p>Phase 3-D-B에서 findDefaultAddress 제거 (UserAddressRepository.findFirstDefaultByUserNo로 위임).</p>
 */
@Mapper
public interface OrderMapper {

    List<OrderHistoryDto> findOrdersByUserId(OrderHistoryReq req);
    OrderHistoryDto orderHistoryDetail(long id);
    int calSumOrder(@Param("storeId") long storeId);
}
