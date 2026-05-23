package com.green.mmg.main.pet;

import com.green.mmg.main.pet.entity.GreenPointLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GreenPointLogRepository extends JpaRepository<GreenPointLog, Long> {
    boolean existsByOrderId(Long orderId);
}
