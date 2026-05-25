package com.green.mmg.auth.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PetInitReq {
    private Long userNo;
    private String species;
    private String name;

    public PetInitReq(Long userNo) {
        this.userNo = userNo;
    }
}
