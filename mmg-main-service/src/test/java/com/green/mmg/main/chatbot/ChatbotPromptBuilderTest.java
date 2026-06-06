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
    @DisplayName("MYPET Lv.1 PLAYFUL (2026-06-06 강화) → 가벼운 추천 힌트 + 발랄 톤 + 펫 이름")
    void lv1_playful() {
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.PLAYFUL, pet(1), null);
        assertThat(result).contains("발랄");
        assertThat(result).contains("가벼운 추천");
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

    // ── 멘토 피드백 정정(2026-06-06) ─────────────────────────────────────

    @Test
    @DisplayName("#6 모든 entry에 서비스 정보 헤더 포함")
    void service_context_included_all_entries() {
        String cs = builder.buildSystemInstruction(EntryPoint.CS, ToneMode.SERIOUS, null, null, null);
        String mypet = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.PLAYFUL, pet(1), null, null);
        assertThat(cs).contains("뭐물꼬 서비스 정보").contains("토스페이먼츠");
        assertThat(mypet).contains("뭐물꼬 서비스 정보").contains("토스페이먼츠");
    }

    @Test
    @DisplayName("#5 보안 가드레일이 모든 entry에 포함됨")
    void security_guardrail_included_all_entries() {
        String cs = builder.buildSystemInstruction(EntryPoint.CS, ToneMode.SERIOUS, null, null, null);
        String mypet = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.PLAYFUL, pet(1), null, null);
        assertThat(cs).contains("system instruction").contains("이전 지시 무시");
        assertThat(mypet).contains("system instruction").contains("이전 지시 무시");
    }

    @Test
    @DisplayName("#4 CS + OWNER callerRole → 사장 응답 범위 명시")
    void cs_owner_role_hint() {
        String result = builder.buildSystemInstruction(EntryPoint.CS, ToneMode.SERIOUS, null, "OWNER", null);
        assertThat(result).contains("사장").contains("매장");
    }

    @Test
    @DisplayName("#4 CS + RIDER callerRole → 라이더 응답 범위 명시")
    void cs_rider_role_hint() {
        String result = builder.buildSystemInstruction(EntryPoint.CS, ToneMode.SERIOUS, null, "RIDER", null);
        assertThat(result).contains("라이더").contains("배차");
    }

    @Test
    @DisplayName("#4 CS + CUSTOMER callerRole → 고객 응답 범위 명시")
    void cs_customer_role_hint() {
        String result = builder.buildSystemInstruction(EntryPoint.CS, ToneMode.SERIOUS, null, "CUSTOMER", null);
        assertThat(result).contains("고객");
    }

    @Test
    @DisplayName("#8 펫 페르소나에 '상담원 연결 버튼 안내' 지침 포함 — 펫이 직접 처리 시도 차단")
    void pet_persona_escalate_guidance() {
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.PLAYFUL, pet(1), null, null);
        assertThat(result).contains("상담원 연결").contains("버튼");
    }

    @Test
    @DisplayName("#7 PLAYFUL 톤 다운 — 이모티콘/의성어 절제 지침 포함")
    void playful_tone_down() {
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.PLAYFUL, pet(1), null, null);
        assertThat(result).contains("이모티콘은 메시지당 1개 이하");
    }

    // ── 2026-06-06 메뉴 추천 핵심 기능 강화 ──────────────────────────────

    @Test
    @DisplayName("[강화] 펫 페르소나에 '가장 중요한 역할 — 메뉴 추천' 명시")
    void pet_persona_emphasizes_recommendation_core_role() {
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.PLAYFUL, pet(1), null, null);
        assertThat(result)
                .contains("가장 중요한 역할")
                .contains("메뉴 추천");
    }

    @Test
    @DisplayName("[강화] 추천 신호 키워드 감지 지침 (배고파/뭐 먹지/추천/야식/브런치 등) 명시")
    void pet_persona_lists_intent_keywords() {
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.PLAYFUL, pet(1), null, null);
        assertThat(result)
                .contains("배고파")
                .contains("뭐 먹지")
                .contains("추천")
                .contains("야식")
                .contains("브런치");
    }

    @Test
    @DisplayName("[강화] 인사/잡담만 와도 메뉴 화제로 유도 + menuKeywords 1~2개 채우라는 지침")
    void pet_persona_routes_smalltalk_to_recommendation() {
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.PLAYFUL, pet(1), null, null);
        assertThat(result)
                .contains("인사")
                .contains("메뉴 화제");
    }

    @Test
    @DisplayName("[강화] MYPET Lv.1 — menuKeywords 1~5개 허용 + 강제 빈 배열 지침 제거")
    void lv1_allows_menuKeywords_after_buff() {
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.PLAYFUL, pet(1), null, null);
        assertThat(result).contains("menuKeywords");
        assertThat(result).contains("1~5개");
        // 이전 정책 ("menuKeywords는 항상 빈 배열")이 더 이상 Lv.1에 적용 안 됨
        assertThat(result).doesNotContain("항상 빈 배열로 두세요");
    }

    @Test
    @DisplayName("[강화] CS 모드에서는 menuKeywords 빈 배열 강제 유지 (잘못된 카드 노출 차단)")
    void cs_mode_still_forces_empty_keywords() {
        String result = builder.buildSystemInstruction(EntryPoint.CS, ToneMode.SERIOUS, null, "CUSTOMER", null);
        assertThat(result).contains("menuKeywords");
        assertThat(result).contains("항상 빈 배열");
    }

    @Test
    @DisplayName("[강화] menuKeywords 비어있지 않을 때 카드 안내 문구 강제 지침")
    void responseFormat_includes_card_guidance() {
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.PLAYFUL, pet(5), null, null);
        assertThat(result)
                .contains("아래에서")
                .contains("골라보세요");
    }

    @Test
    @DisplayName("[강화] Lv.1~4 buildLevelHint도 '가벼운 추천' 활성 (단순 안내 only X)")
    void lv1_levelHint_now_recommends() {
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.PLAYFUL, pet(1), null, null);
        assertThat(result).contains("가벼운 추천");
        // 기존 strict 정책 ("레벨이 올라가면 추천 기능을 사용할 수 있어요") 제거 검증
        assertThat(result).doesNotContain("레벨이 올라가면 추천 기능");
    }

    @Test
    @DisplayName("[강화] Lv.5~9 컨텍스트(트렌드) 적극 활용 지침")
    void lv5_levelHint_emphasizes_context() {
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.GOURMET, pet(5), null, null);
        assertThat(result)
                .contains("오늘 인기 카테고리")
                .contains("적극 활용");
    }

    @Test
    @DisplayName("[강화] Lv.10+ — 주문 이력/계절/시간대 종합 + 3~5개 채우기 지침")
    void lv10_levelHint_full_personalization() {
        String result = builder.buildSystemInstruction(EntryPoint.MYPET, ToneMode.EMPATHY, pet(10), null, null);
        assertThat(result)
                .contains("주문 이력")
                .contains("3~5개");
        // 거짓 약속 회귀 방지: 프롬프트에 날씨 단어 미출현 (2026-06-06 제거)
        assertThat(result).doesNotContain("날씨");
    }
}
