package com.green.mmg.main.internal.dto;

import com.green.mmg.main.pet.entity.Pet;
import com.green.mmg.main.pet.entity.PetSpecies;
import lombok.Getter;

@Getter
public class PetInitRes {
    private final Long petNo;
    private final Long userNo;
    private final PetSpecies species;
    private final String name;
    private final int level;

    public PetInitRes(Pet pet) {
        this.petNo = pet.getPetNo();
        this.userNo = pet.getUserNo();
        this.species = pet.getSpecies();
        this.name = pet.getName();
        this.level = pet.getLevel();
    }
}
