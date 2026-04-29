package com.green.mmg.main.address.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * address 테이블 엔티티 (my_mmg_main.address — Phase 1-B-3.5에서 main으로 이전).
 *
 * <p>BaseEntity 미상속 — created_at/updated_at 컬럼 부재.
 * latitude/longitude는 DECIMAL(16,13) → Java Double 매핑 (응답 DTO 동결 — UserAddressRes Double 유지).</p>
 */
@Entity
@Table(name = "address")
@Getter
@Setter
@NoArgsConstructor
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "default_ad")
    private Integer defaultAd;

    @Column(name = "address", length = 100)
    private String address;

    @Column(name = "address_detail", length = 200)
    private String addressDetail;

    /** DB DECIMAL(16,13) → JDBC NUMERIC, Java Double (응답 DTO Double 동결) */
    @Column(name = "latitude")
    @JdbcTypeCode(SqlTypes.NUMERIC)
    private Double latitude;

    @Column(name = "longitude")
    @JdbcTypeCode(SqlTypes.NUMERIC)
    private Double longitude;
}
