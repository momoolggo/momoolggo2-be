package com.green.mmg.rider.rider;

import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.RiderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiderRepository extends JpaRepository<Rider, Long> {

    Optional<Rider> findByUserNo(Long userNo);

    boolean existsByUserNo(Long userNo);

    /** Group 8 §3.5 — admin 라이더 목록 조회 (status 필터). status null이면 전체 (findAll) — Service에서 분기. */
    List<Rider> findByStatusOrderByRiderNoDesc(RiderStatus status);
}
