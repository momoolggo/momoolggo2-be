package com.green.mmg.admin.blind.repository;

import com.green.mmg.admin.blind.entity.Blind;
import com.green.mmg.admin.common.enums.BlindStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BlindRepository extends JpaRepository<Blind, Long> {

    // 상태별 조회
    List<Blind> findByStatus(BlindStatus status);

    // 특정 리뷰 블라인드 조회
    List<Blind> findByReviewNo(Long reviewNo);

    // 소명 기간 지난 블라인드 조회 (자동 계정정지용)
    List<Blind> findByStatusAndEndsAtBefore(BlindStatus status, LocalDateTime now);

    // user_no 기준 블라인드 횟수 (영구정지 판단용)
    Long countByUserNo(Long userNo);

    // 상태별 건수
    Long countByStatus(BlindStatus status);
}