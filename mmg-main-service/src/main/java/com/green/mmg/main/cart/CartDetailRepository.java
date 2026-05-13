package com.green.mmg.main.cart;

import com.green.mmg.main.cart.model.CartDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartDetailRepository extends JpaRepository<CartDetail, Long> {

    Optional<CartDetail> findByCartIdAndMenuId(Long cartId, Long menuId);

    long countByCartId(Long cartId);

    @Modifying
    @Query("DELETE FROM CartDetail c WHERE c.cartId = :cartId")
    int deleteByCartId(@Param("cartId") Long cartId);
}
