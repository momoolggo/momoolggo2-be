package com.green.mmg.main.cart;

import com.green.mmg.main.cart.model.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserNo(Long userNo);

    Optional<Cart> findFirstByUserNoOrderByCartIdDesc(Long userNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Cart> findAllByUserNoOrderByCartIdDesc(Long userNo);
}
