package com.green.mmg.main.cart.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * cart_detail 테이블 엔티티 (my_mmg_main.cart_detail).
 *
 * <p>Cart와 1:N 관계지만 Phase 3-B는 단순 Foreign-key 컬럼만 (cartId Long).
 * @ManyToOne 매핑은 Phase 3-C 이후 검토 (현재는 N+1/Lazy 위험 회피).</p>
 */
@Entity
@Table(name = "cart_detail")
@Getter
@Setter
@NoArgsConstructor
public class CartDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long cartItemId;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}
