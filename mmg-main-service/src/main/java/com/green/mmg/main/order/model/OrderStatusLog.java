package com.green.mmg.main.order.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_log")
@Getter
@Setter
@NoArgsConstructor

public class OrderStatusLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_no")
    private Long logNo;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "before_state")
    private Integer beforeState;

    @Column(name = "after_state")
    private Integer afterState;

    @Column(name = "changed_by_type", nullable = false, length = 20)
    private String changedByType;

    @Column(name = "changed_by_no")
    private Long changedByNo;

    @Column(length = 255)
    private String memo;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;
}
