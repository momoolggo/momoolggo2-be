package com.green.mmg.admin.policy.repository;

import com.green.mmg.admin.policy.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
}
