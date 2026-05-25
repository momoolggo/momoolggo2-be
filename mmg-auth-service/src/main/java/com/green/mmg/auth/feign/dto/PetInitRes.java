package com.green.mmg.auth.feign.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PetInitRes {
    private Long petNo;
    private Long userNo;
    private String species;
    private String name;
    private int level;
}
