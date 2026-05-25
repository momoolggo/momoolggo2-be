package com.green.mmg.main.pet.dto;

import lombok.Getter;

@Getter
public class PetRewardRes {
    private final Long petNo;
    private final int newLevel;
    private final int newExp;
    private final int newIntimacy;
    private final boolean leveledUp;
    private final int pointGranted;

    public PetRewardRes(Long petNo, int newLevel, int newExp, int newIntimacy, boolean leveledUp, int pointGranted) {
        this.petNo = petNo;
        this.newLevel = newLevel;
        this.newExp = newExp;
        this.newIntimacy = newIntimacy;
        this.leveledUp = leveledUp;
        this.pointGranted = pointGranted;
    }
}
