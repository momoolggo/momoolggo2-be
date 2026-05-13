package com.green.mmg.admin.policy.repository;

import com.green.mmg.admin.policy.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    // 타입별 조회
    List<Policy> findByType(String type);

    // 활성화된 정책 조회
    List<Policy> findByIsActive(Boolean isActive);

    // 타입별 활성화된 정책 조회
    List<Policy> findByTypeAndIsActive(String type, Boolean isActive);
}