package com.green.mmg.main.order;

import com.green.mmg.main.order.model.OrderStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderStatusLogRepository extends JpaRepository<OrderStatusLog, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM OrderStatusLog osl WHERE osl.orderId = :orderId")
    int deleteByOrderId(@Param("orderId") Long orderId);
}
