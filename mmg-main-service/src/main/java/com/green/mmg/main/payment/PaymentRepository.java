package com.green.mmg.main.payment;

import com.green.mmg.main.payment.model.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findFirstByOrderIdOrderByPaymentIdDesc(Long orderId);
}
