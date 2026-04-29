package com.green.mmg.main.order;

import com.green.mmg.main.order.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Orders, Long> {

    long countByUserNo(Long userNo);

    /** 결제 전 주문만 삭제 (pay_state=1 조건부) */
    @Modifying
    @Query("DELETE FROM Orders o WHERE o.orderId = :orderId AND o.payState = 1")
    int deleteByOrderIdAndPayStateUnpaid(@Param("orderId") Long orderId);
}
