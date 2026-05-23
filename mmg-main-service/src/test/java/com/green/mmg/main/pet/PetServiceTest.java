package com.green.mmg.main.pet;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.pet.dto.PetRes;
import com.green.mmg.main.pet.dto.PetUpdateReq;
import com.green.mmg.main.pet.entity.Pet;
import com.green.mmg.main.pet.entity.PetSpecies;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PetService — 단위 테스트")
class PetServiceTest {

    @Mock private PetRepository petRepository;
    @InjectMocks private PetService petService;

    private static final Long USER_NO = 42L;

    @Nested
    @DisplayName("createInitialPetIfAbsent — 회원가입 자동 지급")
    class CreateInitial {

        @Test
        @DisplayName("happy: 펫 부재 → DOG/펫{userNo}/Lv1 저장")
        void absent_savesDefaults() {
            when(petRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());
            ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
            when(petRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            petService.createInitialPetIfAbsent(USER_NO, null, null);

            Pet saved = captor.getValue();
            assertThat(saved.getUserNo()).isEqualTo(USER_NO);
            assertThat(saved.getSpecies()).isEqualTo(PetSpecies.DOG);
            assertThat(saved.getName()).isEqualTo("펫" + USER_NO);
            assertThat(saved.getLevel()).isEqualTo(1);
            assertThat(saved.getExp()).isZero();
            assertThat(saved.getIntimacy()).isZero();
        }

        @Test
        @DisplayName("idempotent: 펫 존재 → save 호출 0건, 기존 반환")
        void exists_returnsExisting() {
            Pet existing = new Pet(USER_NO, PetSpecies.CAT, "야옹이");
            when(petRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(existing));

            Pet result = petService.createInitialPetIfAbsent(USER_NO, PetSpecies.RABBIT, "토끼");

            assertThat(result).isSameAs(existing);
            verify(petRepository, never()).save(any());
        }

        @Test
        @DisplayName("species 명시 + name 명시 → 지정값으로 저장")
        void explicit_args_used() {
            when(petRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());
            ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
            when(petRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            petService.createInitialPetIfAbsent(USER_NO, PetSpecies.RABBIT, "포포");

            assertThat(captor.getValue().getSpecies()).isEqualTo(PetSpecies.RABBIT);
            assertThat(captor.getValue().getName()).isEqualTo("포포");
        }
    }

    @Nested
    @DisplayName("getMyPet / getOrCreatePet")
    class Read {

        @Test
        @DisplayName("getMyPet — 부재 시 NOT_FOUND throw")
        void getMyPet_notFound_throws() {
            when(petRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> petService.getMyPet(USER_NO))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("getOrCreatePet — 부재 시 자동 생성 (lazy fallback)")
        void getOrCreatePet_absent_creates() {
            when(petRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());
            when(petRepository.save(any(Pet.class))).thenAnswer(inv -> inv.getArgument(0));

            Pet result = petService.getOrCreatePet(USER_NO);

            assertThat(result.getUserNo()).isEqualTo(USER_NO);
            assertThat(result.getSpecies()).isEqualTo(PetSpecies.DOG);
            verify(petRepository).save(any(Pet.class));
        }

        @Test
        @DisplayName("getMyPet happy: DTO 매핑 일관")
        void getMyPet_happy() {
            Pet pet = new Pet(USER_NO, PetSpecies.HAMSTER, "햄찌");
            when(petRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(pet));

            PetRes res = petService.getMyPet(USER_NO);

            assertThat(res.getUserNo()).isEqualTo(USER_NO);
            assertThat(res.getSpecies()).isEqualTo(PetSpecies.HAMSTER);
            assertThat(res.getName()).isEqualTo("햄찌");
            assertThat(res.getLevel()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("updatePet — 종족/이름 수정")
    class Update {

        @Test
        @DisplayName("happy: 종족+이름 모두 변경")
        void happy_updatesBoth() {
            Pet pet = new Pet(USER_NO, PetSpecies.DOG, "멍멍이");
            when(petRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(pet));
            PetUpdateReq req = new PetUpdateReq();
            req.setSpecies(PetSpecies.CAT);
            req.setName("냥냥이");

            PetRes res = petService.updatePet(USER_NO, req);

            assertThat(res.getSpecies()).isEqualTo(PetSpecies.CAT);
            assertThat(res.getName()).isEqualTo("냥냥이");
        }

        @Test
        @DisplayName("blank name → 변경 X (기존 유지)")
        void blankName_skipped() {
            Pet pet = new Pet(USER_NO, PetSpecies.DOG, "원본");
            when(petRepository.findByUserNo(USER_NO)).thenReturn(Optional.of(pet));
            PetUpdateReq req = new PetUpdateReq();
            req.setName("   ");
            req.setSpecies(PetSpecies.CAT);

            PetRes res = petService.updatePet(USER_NO, req);

            assertThat(res.getName()).isEqualTo("원본");
            assertThat(res.getSpecies()).isEqualTo(PetSpecies.CAT);
        }

        @Test
        @DisplayName("부재 시 NOT_FOUND throw")
        void notFound_throws() {
            when(petRepository.findByUserNo(USER_NO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> petService.updatePet(USER_NO, new PetUpdateReq()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Pet.gainExp — P-6 사전 검증 (레벨업 로직)")
    class GainExp {

        @Test
        @DisplayName("50 EXP → Lv1 유지 (100 미달)")
        void noLevelUp() {
            Pet pet = new Pet(USER_NO, PetSpecies.DOG, "테스트");

            boolean leveledUp = pet.gainExp(50, 5);

            assertThat(leveledUp).isFalse();
            assertThat(pet.getLevel()).isEqualTo(1);
            assertThat(pet.getExp()).isEqualTo(50);
            assertThat(pet.getIntimacy()).isEqualTo(5);
        }

        @Test
        @DisplayName("100 EXP → Lv2 + EXP 0 + 친밀도 누적")
        void singleLevelUp() {
            Pet pet = new Pet(USER_NO, PetSpecies.DOG, "테스트");

            boolean leveledUp = pet.gainExp(100, 10);

            assertThat(leveledUp).isTrue();
            assertThat(pet.getLevel()).isEqualTo(2);
            assertThat(pet.getExp()).isZero();
            assertThat(pet.getIntimacy()).isEqualTo(10);
        }

        @Test
        @DisplayName("연쇄 레벨업: 250 EXP → Lv3 + 잔여 EXP 0")
        void cascadeLevelUp() {
            Pet pet = new Pet(USER_NO, PetSpecies.DOG, "테스트");

            boolean leveledUp = pet.gainExp(250, 0);

            assertThat(leveledUp).isTrue();
            assertThat(pet.getLevel()).isEqualTo(3);
            assertThat(pet.getExp()).isZero();
        }

        @Test
        @DisplayName("음수 입력 가드: 0으로 보정")
        void negativeGuarded() {
            Pet pet = new Pet(USER_NO, PetSpecies.DOG, "테스트");

            boolean leveledUp = pet.gainExp(-50, -10);

            assertThat(leveledUp).isFalse();
            assertThat(pet.getExp()).isZero();
            assertThat(pet.getIntimacy()).isZero();
        }
    }
}
