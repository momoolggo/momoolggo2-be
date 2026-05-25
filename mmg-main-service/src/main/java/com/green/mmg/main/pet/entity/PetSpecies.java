package com.green.mmg.main.pet.entity;

/**
 * 펫 종족 enum.
 * 2026-05-25 9건 트랙 #8 — FE 8종 매핑 호환 위해 BEAR/FOX/PANDA/KOALA 4종 추가.
 * Pet.species는 @Enumerated(EnumType.STRING)이라 DB는 VARCHAR — DDL 변경 없음.
 */
public enum PetSpecies {
    DOG, CAT, RABBIT, HAMSTER,
    BEAR, FOX, PANDA, KOALA
}
