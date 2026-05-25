package com.green.mmg.main.order;

import com.green.mmg.main.order.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface OrderRepository extends JpaRepository<Orders, Long> {

    long countByUserNo(Long userNo);

    /** 2026-05-25 9건 트랙 #8 부채 — 본인 주문 완료(orderState=6) 누적 횟수 (펫 페이지 '준 간식'용) */
    long countByUserNoAndOrderState(Long userNo, Integer orderState);

    /** 결제 전 주문만 삭제 (pay_state=1 조건부) */
    @Modifying
    @Query("DELETE FROM Orders o WHERE o.orderId = :orderId AND o.payState = 1")
    int deleteByOrderIdAndPayStateUnpaid(@Param("orderId") Long orderId);

    @Query("SELECT COUNT(o) FROM Orders o WHERE o.orderTime >= :start AND o.orderTime < :end")
    long countTodayOrders(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Orders o WHERE o.orderTime >= :start AND o.orderTime < :end")
    long sumTodayRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
