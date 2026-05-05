package com.green.mmg.rider.rider;

import com.green.mmg.rider.rider.model.Rider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RiderRepository extends JpaRepository<Rider, Long> {

    Optional<Rider> findByUserNo(Long userNo);

    boolean existsByUserNo(Long userNo);
}
