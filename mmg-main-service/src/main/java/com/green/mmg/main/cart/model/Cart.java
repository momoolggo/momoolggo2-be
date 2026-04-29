package com.green.mmg.main.cart.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * cart 테이블 엔티티 (my_mmg_main.cart).
 *
 * <p>BaseEntity 미상속 — created_at/updated_at 컬럼 부재 (Phase 3-B 정찰 결과).</p>
 */
@Entity
@Table(name = "cart")
@Getter
@Setter
@NoArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long cartId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;
}
