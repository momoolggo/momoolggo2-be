package com.green.mmg.main.pet.entity;

import com.green.mmg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pets")
@Getter
@NoArgsConstructor
public class Pet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pet_no")
    private Long petNo;

    @Column(name = "user_no", nullable = false, unique = true)
    private Long userNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "species", nullable = false)
    private PetSpecies species;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "exp", nullable = false)
    private int exp;

    @Column(name = "intimacy", nullable = false)
    private int intimacy;

    public Pet(Long userNo, PetSpecies species, String name) {
        this.userNo = userNo;
        this.species = species;
        this.name = name;
        this.level = 1;
        this.exp = 0;
        this.intimacy = 0;
    }

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) return;
        this.name = newName;
    }

    public void changeSpecies(PetSpecies newSpecies) {
        if (newSpecies == null) return;
        this.species = newSpecies;
    }

    /** P-6에서 주문 완료 시 호출 — 친밀도 + EXP 상승, 100 EXP마다 레벨업. */
    public boolean gainExp(int amount, int intimacyDelta) {
        if (amount < 0) amount = 0;
        if (intimacyDelta < 0) intimacyDelta = 0;
        this.intimacy += intimacyDelta;
        this.exp += amount;
        boolean leveledUp = false;
        while (this.exp >= expRequiredForNextLevel(this.level)) {
            this.exp -= expRequiredForNextLevel(this.level);
            this.level += 1;
            leveledUp = true;
        }
        return leveledUp;
    }

    private static int expRequiredForNextLevel(int currentLevel) {
        return 100 + (currentLevel - 1) * 50;
    }
}
