package com.green.mmg.auth.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * user 테이블 엔티티 (my_mmg_auth.user).
 *
 * <p>BaseEntity 상속 X — user 테이블에 created_at/updated_at 컬럼 미존재 (Phase 3-A 정찰 결과).
 * Auditing 적용은 Phase 3-B의 likedstore/store부터.</p>
 *
 * <p>⚠️ userPw(BCrypt) 필드 응답 노출 절대 금지.</p>
 */
@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_no")
    private long userNo;

    @Column(name = "user_id", length = 20, unique = true)
    private String userId;

    @Column(name = "user_pw", length = 1000)
    private String userPw;

    @Column(name = "role", columnDefinition = "ENUM('CUSTOMER','OWNER','RIDER','ADMIN')")
    private String role;

    @Column(name = "name", length = 10)
    private String name;

    @Column(name = "status", columnDefinition = "ENUM('PENDING', 'ACTIVE', 'REJECTED', 'SUSPENDED')",
    nullable = false)
    private String status;

    /** DB DATE ↔ Java String("yyyy-MM-dd") — 응답 스펙 동결 */
    @Convert(converter = StringDateConverter.class)
    @Column(name = "birth")
    private String birth;

    @Column(name = "gender")
    private int gender;

    @Column(name = "green")
    private int green;

    @Column(name = "kind")
    private int kind;

    /** rank: MariaDB 예약어 → 백틱 처리 */
    @Column(name = "`rank`", columnDefinition = "ENUM('BRONZE','SILVER','GOLD','VIP','VVIP')")
    private String rank;

    @Column(name = "tel", length = 20)
    private String tel;

    @Column(name = "business_no", length = 10)
    private String businessNo;

    @Column(name = "process_memo", length = 255)
    private String processMemo;

    @Column(name = "suspension_until")
    private Date suspensionUntil;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Date createdAt;
}
