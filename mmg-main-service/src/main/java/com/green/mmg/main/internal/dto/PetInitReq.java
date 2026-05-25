package com.green.mmg.main.internal.dto;

import com.green.mmg.main.pet.entity.PetSpecies;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PetInitReq {
    private Long userNo;
    private PetSpecies species;
    private String name;
}
