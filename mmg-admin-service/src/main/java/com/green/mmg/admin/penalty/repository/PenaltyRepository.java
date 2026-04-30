package com.green.mmg.admin.penalty.repository;

import com.green.mmg.admin.common.enums.PenaltyTarget;
import com.green.mmg.admin.penalty.entity.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    // 특정 유저/가게의 패널티 목록 조회
    List<Penalty> findByTargetTypeAndTargetNo(PenaltyTarget targetType, Long targetNo);
}