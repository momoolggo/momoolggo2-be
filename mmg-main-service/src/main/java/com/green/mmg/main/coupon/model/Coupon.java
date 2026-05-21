package com.green.mmg.main.coupon.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.type.descriptor.jdbc.TinyIntJdbcType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "coupon")
@Getter
@NoArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "discount_type")
    private String discountType;

    @Column(name = "discount_value")
    private Integer discountValue;

    @Column(name = "min_discount_amount")
    private Integer minDiscountAmount;

    @Column(name = "max_discount_amount")
    private Integer maxDiscountAmount;

    @Column(name = "total_count")
    private Integer totalCount;

    @Column(name = "remaining_count")
    private Integer remainingCount;

    @Column(name = "issue_start_date")
    private LocalDate issueStartDate;

    @Column(name = "issue_end_date")
    private LocalDate issueEndDate;

    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(name = "issue_type", length = 20)
    private String issueType;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

}
