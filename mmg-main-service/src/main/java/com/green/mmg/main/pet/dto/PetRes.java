package com.green.mmg.main.pet.dto;

import com.green.mmg.main.pet.entity.Pet;
import com.green.mmg.main.pet.entity.PetSpecies;
import lombok.Getter;

@Getter
public class PetRes {
    private final Long petNo;
    private final Long userNo;
    private final PetSpecies species;
    private final String name;
    private final int level;
    private final int exp;
    private final int intimacy;

    public PetRes(Pet pet) {
        this.petNo = pet.getPetNo();
        this.userNo = pet.getUserNo();
        this.species = pet.getSpecies();
        this.name = pet.getName();
        this.level = pet.getLevel();
        this.exp = pet.getExp();
        this.intimacy = pet.getIntimacy();
    }
}
