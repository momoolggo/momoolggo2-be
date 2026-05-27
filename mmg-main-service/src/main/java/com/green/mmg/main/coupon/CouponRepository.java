package com.green.mmg.main.coupon;

import com.green.mmg.main.coupon.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findFirstByNameAndIssueTypeAndIsActiveTrue(String name, String issueType);
}
