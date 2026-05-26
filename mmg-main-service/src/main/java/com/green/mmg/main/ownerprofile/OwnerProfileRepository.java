package com.green.mmg.main.ownerprofile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, Long> {
    boolean existsByUserNo(Long userNo);
    Optional<OwnerProfile> findByUserNo(Long userNo);
}