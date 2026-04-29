package com.green.mmg.main.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

/**
 * orders 테이블 엔티티 (my_mmg_main.orders).
 *
 * <p>Phase 3-C-1: PK는 manual assignment (placeOrder가 `Long.parseLong("39"+timestamp)`).
 * `@Id` no `@GeneratedValue` 패턴 — JPA `save()`가 merge 발동(SELECT 후 INSERT) 회피 위해
 * {@link Persistable} 구현. 신규 entity면 EntityManager.persist 직접 호출 효과.</p>
 *
 * <p>order_time: DB DEFAULT current_timestamp() — JPA insertable=false, updatable=false.</p>
 *
 * <p>BaseEntity 미상속: order_time = 주문일시 (의미 다름, audit 아님).</p>
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Orders implements Persistable<Long> {

    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "order_time", insertable = false, updatable = false)
    private LocalDateTime orderTime;

    @Column(name = "request")
    private String request;

    @Column(name = "rider_request")
    private String riderRequest;

    @Column(name = "address")
    private String address;

    @Column(name = "address_detail")
    private String addressDetail;

    @Column(name = "delivery_fee")
    private Integer deliveryFee;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "delivery_state")
    private Integer deliveryState;

    @Column(name = "pay_state")
    private Integer payState;

    @Column(name = "order_state")
    private Integer orderState;

    @Transient
    private boolean isNewEntity = true;

    @Override
    public Long getId() { return orderId; }

    @Override
    public boolean isNew() { return isNewEntity; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNewEntity = false; }
}
