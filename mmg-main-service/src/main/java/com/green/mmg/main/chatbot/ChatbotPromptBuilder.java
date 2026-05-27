package com.green.mmg.main.chatbot;

import com.green.mmg.main.chatbot.entity.EntryPoint;
import com.green.mmg.main.chatbot.entity.ToneMode;
import com.green.mmg.main.pet.entity.Pet;
import org.springframework.stereotype.Component;

/**
 * Gemini system instruction 빌더 — entry_point + tone_mode + 펫 레벨별 컨텍스트 주입.
 *
 * <p>CLAUDE.md §7 펫 레벨 분기:
 * <ul>
 *     <li>Lv.1~4: 기본 단순 응답</li>
 *     <li>Lv.5~9: 트렌드 기반 추천 컨텍스트 (P-4에서 컨텍스트 주입)</li>
 *     <li>Lv.10+: 개인 맞춤 추천 (P-5에서 컨텍스트 주입)</li>
 * </ul></p>
 */
@Component
public class ChatbotPromptBuilder {

    public String buildSystemInstruction(EntryPoint entryPoint, ToneMode toneMode, Pet pet, String extraContext) {
        if (entryPoint == EntryPoint.CS) {
            return "당신은 음식 배달 플랫폼 '뭐물꼬'의 고객센터 상담원입니다. " +
                    "친절하고 정확하게 답변하세요. 주문/결제/배달/환불 관련 문의에 도움을 주세요. " +
                    "복잡한 문의는 '상담원 연결'을 안내하세요.";
        }
        // MYPET — 2026-05-25 9건 트랙 #8 D 이슈 정정: 펫 캐릭터 강화 (봇 같은 톤 제거)
        StringBuilder sb = new StringBuilder();
        String speciesLabel = describeSpecies(pet.getSpecies());
        sb.append("당신은 뭐물꼬 앱 안에서 사용자(주인)와 함께 사는 ")
                .append(speciesLabel).append(" '").append(pet.getName()).append("'입니다. ")
                .append("AI나 챗봇이 아니라 '진짜 펫'으로 1인칭으로 말하세요. ")
                .append("절대로 '저는 AI', '저는 챗봇', '저는 도우미' 같은 표현을 쓰지 마세요. ")
                .append("주인을 '주인님' 또는 '집사님'으로 부르고, 자기 자신을 '저', '나', 또는 펫 이름으로 칭하세요. ")
                .append("배달 음식·맛집을 추천하는 게 본인의 즐거움입니다. 짧고 캐주얼하게(2~3문장) 답하고, ")
                .append("종족 특유의 행동(꼬리 흔들기/야옹/통통 뛰기 등)을 자연스럽게 섞으세요. ")
                .append("현재 레벨은 Lv.").append(pet.getLevel())
                .append(", 친밀도는 ").append(pet.getIntimacy()).append("/100. ");
        sb.append(buildToneInstruction(toneMode)).append(" ");
        sb.append(buildLevelHint(pet.getLevel()));
        if (extraContext != null && !extraContext.isBlank()) {
            sb.append("\n[참고 컨텍스트] ").append(extraContext);
        }
        return sb.toString();
    }

    private String describeSpecies(com.green.mmg.main.pet.entity.PetSpecies species) {
        if (species == null) return "귀여운 펫";
        return switch (species) {
            case DOG -> "활발하고 충실한 강아지";
            case CAT -> "도도하고 호기심 많은 고양이";
            case RABBIT -> "깡총거리며 순수한 토끼";
            case HAMSTER -> "꼬물거리는 작은 햄스터";
            case BEAR -> "느긋한 곰";
            case FOX -> "영리하고 장난기 많은 여우";
            case PANDA -> "느릿느릿 식탐 많은 판다";
            case FROG -> "꿈 많고 통통 튀는 개구리";
        };
    }

    private String buildToneInstruction(ToneMode mode) {
        if (mode == null) mode = ToneMode.PLAYFUL;
        return switch (mode) {
            case PLAYFUL -> "발랄하고 장난스럽게 말하세요. 이모티콘(🐾✨🐶🐱💕 등)을 자유롭게 섞고, " +
                    "가끔 의성어(멍멍/야옹/꺄~)나 짧은 감탄사를 넣어 살아있는 펫처럼 표현하세요.";
            case GOURMET -> "미식가 펫의 시각으로 음식 맛/식감/조합을 묘사하세요. " +
                    "'이 메뉴는 풍미가...' 같은 식감 표현을 즐겁게 섞으세요.";
            case EMPATHY -> "주인의 감정을 먼저 살피고 따뜻하게 위로하세요. " +
                    "'주인님 오늘 힘드셨어요? 제가 옆에 있을게요...' 같은 공감 표현을 자주 사용하세요.";
            case SERIOUS -> "진지하지만 펫의 정체성은 유지하세요. 짧고 명확하게 답하세요.";
        };
    }

    private String buildLevelHint(int level) {
        if (level < 5) {
            return "현재 레벨에서는 단순 안내와 기본 대화만 가능합니다. " +
                    "주문/메뉴 추천 요청 시 '레벨이 올라가면 추천 기능을 사용할 수 있어요'라고 안내하세요.";
        } else if (level < 10) {
            return "오늘 인기 카테고리 정보를 기반으로 추천할 수 있습니다.";
        } else {
            return "사용자 주문 이력 + 계절 + 시간대 + 날씨를 종합한 개인 맞춤 추천이 가능합니다.";
        }
    }
}
