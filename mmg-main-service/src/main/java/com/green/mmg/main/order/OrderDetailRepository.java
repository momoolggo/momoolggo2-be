package com.green.mmg.main.order;

import com.green.mmg.main.order.model.OrderDetail;
import com.green.mmg.main.order.model.OrderHistoryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

    /** OrderHistoryDto.OrderItemDto로 직접 매핑 (응답 DTO 동결) */
    @Query("""
            SELECT new com.green.mmg.main.order.model.OrderHistoryDto$OrderItemDto(od.menuName, od.quantity, od.menuPrice)
            FROM OrderDetail od
            WHERE od.orderId = :orderId
            """)
    List<OrderHistoryDto.OrderItemDto> findItemsByOrderId(@Param("orderId") Long orderId);
}
