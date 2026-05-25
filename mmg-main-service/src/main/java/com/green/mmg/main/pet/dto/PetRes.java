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
    // 2026-05-25 9건 트랙 #8 부채 — 펫 페이지 누적 통계
    private final long totalPoints;  // green_point_log 누적 합산
    private final long totalMeals;   // 주문 완료(state=6) 누적 횟수
    // Step B — 출석 시스템 통합
    private final int streak;        // 연속 출석 일수
    private final int monthCount;    // 이번달 출석 횟수

    public PetRes(Pet pet) {
        this(pet, 0L, 0L, 0, 0);
    }

    public PetRes(Pet pet, long totalPoints, long totalMeals, int streak, int monthCount) {
        this.petNo = pet.getPetNo();
        this.userNo = pet.getUserNo();
        this.species = pet.getSpecies();
        this.name = pet.getName();
        this.level = pet.getLevel();
        this.exp = pet.getExp();
        this.intimacy = pet.getIntimacy();
        this.totalPoints = totalPoints;
        this.totalMeals = totalMeals;
        this.streak = streak;
        this.monthCount = monthCount;
    }
}
