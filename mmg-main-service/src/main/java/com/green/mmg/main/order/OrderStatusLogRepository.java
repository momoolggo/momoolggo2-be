package com.green.mmg.main.order;

import com.green.mmg.main.order.model.OrderStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusLogRepository extends JpaRepository<OrderStatusLog, Long> {


}
