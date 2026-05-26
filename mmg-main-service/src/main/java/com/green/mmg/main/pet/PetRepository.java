package com.green.mmg.main.pet;

import com.green.mmg.main.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
    Optional<Pet> findByUserNo(Long userNo);
    boolean existsByUserNo(Long userNo);
}
