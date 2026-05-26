package com.green.mmg.main.pet.dto;

import com.green.mmg.main.pet.entity.PetSpecies;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PetUpdateReq {
    private PetSpecies species;
    private String name;
}
