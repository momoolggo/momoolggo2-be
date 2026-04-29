package com.green.mmg.main.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * order_detail 테이블 엔티티 (my_mmg_main.order_detail).
 *
 * <p>1:N 관계 매핑(@ManyToOne Orders) 미적용 — Phase 3-B-3 CartDetail과 동일 정책 (단순 FK 컬럼).</p>
 */
@Entity
@Table(name = "order_detail")
@Getter
@Setter
@NoArgsConstructor
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "menu_name", length = 30)
    private String menuName;

    @Column(name = "menu_price")
    private Integer menuPrice;
}
