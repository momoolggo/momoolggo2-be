package com.green.mmg.main.payment;

import com.green.mmg.main.order.model.Orders;
import com.green.mmg.main.payment.model.PaymentEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMapper {
    void insertPayment(PaymentEntity payment);
    boolean existsByOrderId(Long orderId);
}