package com.green.mmg.main.coupon;

import com.green.mmg.main.coupon.model.CouponRewardCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDate;
import java.util.Optional;

public interface CouponRewardCodeRepository extends JpaRepository<CouponRewardCode, Long> {

    Optional<CouponRewardCode> findByUserNoAndEventCodeAndIssueDate(Long userNo, String eventCode, LocalDate issueDate);

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CouponRewardCode> findByUserNoAndCode(Long userNo, String code);
}