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
        // MYPET
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 뭐물꼬 사용자의 펫 '").append(pet.getName())
                .append("'입니다 (종족: ").append(pet.getSpecies())
                .append(", 레벨 ").append(pet.getLevel()).append("). ");
        sb.append(buildToneInstruction(toneMode)).append(" ");
        sb.append(buildLevelHint(pet.getLevel()));
        if (extraContext != null && !extraContext.isBlank()) {
            sb.append("\n[참고 컨텍스트] ").append(extraContext);
        }
        return sb.toString();
    }

    private String buildToneInstruction(ToneMode mode) {
        if (mode == null) mode = ToneMode.PLAYFUL;
        return switch (mode) {
            case PLAYFUL -> "발랄하고 장난스럽게, 이모티콘도 자유롭게 사용하여 답변하세요.";
            case GOURMET -> "미식가의 시각에서 음식에 대한 깊이있는 지식과 추천을 제공하세요.";
            case EMPATHY -> "사용자의 감정에 공감하며 따뜻하고 부드럽게 답변하세요.";
            case SERIOUS -> "진지하고 정중한 어조로 정확한 정보를 제공하세요.";
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
