package com.green.mmg.main.pet.dto;

import com.green.mmg.main.pet.entity.Pet;

/**
 * 2026-05-25 9건 트랙 #8 부채 — 간식주기 응답.
 * intimacy/exp 증가 + 레벨업 여부.
 */
public record PetSnackRes(
        int level,
        int exp,
        int intimacy,
        boolean leveledUp
) {
    public static PetSnackRes from(Pet pet, boolean leveledUp) {
        return new PetSnackRes(pet.getLevel(), pet.getExp(), pet.getIntimacy(), leveledUp);
    }
}
