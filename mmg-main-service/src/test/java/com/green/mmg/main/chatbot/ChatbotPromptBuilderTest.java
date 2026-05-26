package com.green.mmg.main.chatbot;

import com.green.mmg.main.chatbot.entity.EntryPoint;
import com.green.mmg.main.chatbot.entity.ToneMode;
import com.green.mmg.main.pet.entity.Pet;
import com.green.mmg.main.pet.entity.PetSpecies;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatbotPromptBuilder — 톤/레벨 분기 단위 테스트")
class ChatbotPromptBuilderTest {

    private final ChatbotPromptBuilder builder = new ChatbotPromptBuilder();

    private Pet pet(int level) {
        Pet p = new Pet(1L, PetSpecies.DOG, "테스트");
        // level은 gainExp로만 변경 — 100EXP / 100EXP / 150EXP 누적해야 Lv2/Lv3 도달
        for (int i = 1; i < level; i++) {
            int need = 100 + (i - 1) * 50;
            p.gainExp(need, 0);
        }
        return p;
    }

    @Test
    @DisplayName("CS entry → 고객센터 system prompt")
    void cs_prompt() {
        String result = builder.buildSystemInstruction(EntryPoint.CS, ToneMode.SERIOUS, null, null);
        assertThat(result).contains("고객센터");
        assertThat(result).contains("뭐물꼬");
    }

    @Test
    @DisplayName("MYPET Lv.1 PLAYFUL → 단순 안내 힌트 + 발랄 톤")
    void lv1_playful() {
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.PLAYFUL, pet(1), null);
        assertThat(result).contains("발랄");
        assertThat(result).contains("단순 안내");
        assertThat(result).contains("테스트");
    }

    @Test
    @DisplayName("MYPET Lv.5 GOURMET → 트렌드 힌트 + 미식가 톤")
    void lv5_gourmet() {
        Pet pet = pet(5);
        assertThat(pet.getLevel()).isEqualTo(5);
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.GOURMET, pet, null);
        assertThat(result).contains("미식가");
        assertThat(result).contains("오늘 인기 카테고리");
    }

    @Test
    @DisplayName("MYPET Lv.10 EMPATHY → 개인 맞춤 추천 힌트 + 공감 톤")
    void lv10_empathy() {
        Pet pet = pet(10);
        assertThat(pet.getLevel()).isEqualTo(10);
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.EMPATHY, pet, null);
        assertThat(result).contains("공감");
        assertThat(result).contains("개인 맞춤 추천");
    }

    @Test
    @DisplayName("extraContext 주입 → 끝에 [참고 컨텍스트] 부착")
    void extraContext_attached() {
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.PLAYFUL, pet(7),
                "오늘 인기 카테고리: 치킨/피자");
        assertThat(result).contains("[참고 컨텍스트]");
        assertThat(result).contains("치킨/피자");
    }

    @Test
    @DisplayName("null tone → PLAYFUL 기본 적용")
    void nullTone_defaultsPlayful() {
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, null, pet(1), null);
        assertThat(result).contains("발랄");
    }
}
