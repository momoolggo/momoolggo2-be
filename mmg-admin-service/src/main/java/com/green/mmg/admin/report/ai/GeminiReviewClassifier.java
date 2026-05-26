package com.green.mmg.admin.report.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.green.mmg.admin.ai.GeminiClient;
import com.green.mmg.admin.ai.exception.AiParsingException;
import com.green.mmg.admin.report.dto.AiReviewJudgement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class GeminiReviewClassifier {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public GeminiReviewClassifier(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    private static final String SYSTEM_PROMPT =
            "당신은 음식 배달 플랫폼의 리뷰 신고 심사 AI입니다. " +
            "[블라인드 가능 위반]: 욕설·혐오, 도배·스팸, 성적·폭력적 표현, 개인정보 노출, 외부 광고·홍보. " +
            "[절대 블라인드 금지]: 주관적 평가(맛없다/양적다/비싸다), 음식상태 주장, 배달원·포장 불만, 별점. " +
            "반드시 아래 JSON만 출력(마크다운 금지): " +
            "{\"shouldBlind\":true/false,\"confidence\":\"HIGH|MEDIUM|LOW\",\"reason\":\"판단이유(50자이내)\",\"violations\":[]} " +
            "shouldBlind=false이면 violations는 빈 배열. 확신없으면 confidence=LOW, shouldBlind=false.";

    public AiReviewJudgement judge(String reviewContent, String reportReason) {
        String userContent = SYSTEM_PROMPT + "\n\n---\n신고 사유: " + reportReason + "\n\n리뷰 내용:\n" + reviewContent +
                "\n\n위 내용을 분석하여 JSON 객체만 출력하세요. 다른 텍스트는 절대 포함하지 마세요.";
        String raw = geminiClient.generateText(
                null, null, userContent,
                1000, false, "REVIEW_BLIND_JUDGE", null
        );
        return parse(raw);
    }

    private AiReviewJudgement parse(String raw) {
        try {
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            String cleaned = (start >= 0 && end > start) ? raw.substring(start, end + 1) : raw.strip();
            log.info("AI 파싱 시도: [{}]", cleaned);
            JsonNode node = objectMapper.readTree(cleaned);
            boolean shouldBlind = node.path("shouldBlind").asBoolean(false);
            String confidence = node.path("confidence").asText("LOW");
            String reason = node.path("reason").asText("");
            List<String> violations = new ArrayList<>();
            node.path("violations").forEach(v -> violations.add(v.asText()));
            return new AiReviewJudgement(shouldBlind, confidence, reason, violations);
        } catch (Exception e) {
            log.error("파싱 실패 원인: {}", e.getMessage());
            throw new AiParsingException("AI 응답 파싱 실패: " + raw.substring(0, Math.min(100, raw.length())), e);
        }
    }
}
