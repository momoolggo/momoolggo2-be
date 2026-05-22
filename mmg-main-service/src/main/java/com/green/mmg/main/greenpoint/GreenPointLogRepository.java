package com.green.mmg.main.greenpoint;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GreenPointLogRepository extends JpaRepository<GreenPointLog, Long> {
    boolean existsByOrderId(Long orderId);

    List<GreenPointLog> findByStatusInAndRetryCountLessThanOrderByGreenPointLogIdAsc(
            List<String> statuses,
            Integer retryCount,
            Pageable pageable
    );
}