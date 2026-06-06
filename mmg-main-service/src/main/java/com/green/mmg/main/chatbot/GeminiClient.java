package com.green.mmg.main.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * main 측 Gemini 호출 (admin GeminiClient 패턴 복제).
 * admin 측 AiOperationMetric 저장은 admin 단독 책임 — main은 단순 호출만.
 * 재시도 + lite 폴백 패턴은 admin 동일.
 */
@Component
@Slf4j
public class GeminiClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.gemini.api-key:}")
    private String apiKey;
    @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;
    @Value("${app.gemini.default-model:gemini-2.5-flash}")
    private String defaultModel;
    @Value("${app.gemini.lite-model:gemini-2.0-flash}")
    private String liteModel;

    private RestClient client;

    private RestClient client() {
        if (client == null) client = RestClient.builder().baseUrl(baseUrl).build();
        return client;
    }

    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }
    public String getDefaultModel() { return defaultModel; }

    public String generateText(String systemInstruction, String userContent, int maxOutputTokens) {
        return generateInternal(systemInstruction, userContent, maxOutputTokens, null);
    }

    /**
     * #1+#2 메뉴 추천 카드용 — responseSchema(JSON) 강제 호출.
     * 반환 = candidates[0].text (JSON 문자열, caller가 파싱).
     */
    public String generateJson(String systemInstruction, String userContent, int maxOutputTokens, String responseSchemaJson) {
        return generateInternal(systemInstruction, userContent, maxOutputTokens, responseSchemaJson);
    }

    private String generateInternal(String systemInstruction, String userContent, int maxOutputTokens, String responseSchemaJson) {
        if (!isConfigured()) throw new GeminiException("Gemini API 키가 설정되지 않음");
        try {
            return callOnce(defaultModel, systemInstruction, userContent, maxOutputTokens, responseSchemaJson);
        } catch (GeminiException e1) {
            if (!isTransient(e1)) throw e1;
            try { Thread.sleep(1500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            try {
                log.info("Gemini 재시도 model={}", defaultModel);
                return callOnce(defaultModel, systemInstruction, userContent, maxOutputTokens, responseSchemaJson);
            } catch (GeminiException e2) {
                if (!isTransient(e2) || defaultModel.equalsIgnoreCase(liteModel)) throw e2;
                log.info("Gemini lite 폴백 from={} to={}", defaultModel, liteModel);
                return callOnce(liteModel, systemInstruction, userContent, maxOutputTokens, responseSchemaJson);
            }
        }
    }

    private String callOnce(String useModel, String systemInstruction, String userContent, int maxOutputTokens, String responseSchemaJson) {
        try {
            String bodyJson = buildRequestBodyJson(systemInstruction, userContent, maxOutputTokens, responseSchemaJson);
            String path = "/v1beta/models/" + useModel + ":generateContent?key=" + apiKey;
            String respStr = client().post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bodyJson)
                    .retrieve()
                    .body(String.class);
            if (respStr == null || respStr.isBlank()) throw new GeminiException("Gemini 응답이 비어있음");
            JsonNode resp = objectMapper.readTree(respStr);
            return extractText(resp);
        } catch (GeminiException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiException("Gemini 호출 실패: " + e.getMessage(), e);
        }
    }

    private String buildRequestBodyJson(String systemInstruction, String userContent, int maxOutputTokens, String responseSchemaJson) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            if (systemInstruction != null && !systemInstruction.isBlank()) {
                ObjectNode sysNode = objectMapper.createObjectNode();
                ArrayNode sysParts = objectMapper.createArrayNode();
                ObjectNode sysPart = objectMapper.createObjectNode();
                sysPart.put("text", systemInstruction);
                sysParts.add(sysPart);
                sysNode.set("parts", sysParts);
                body.set("system_instruction", sysNode);
            }
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            ArrayNode parts = objectMapper.createArrayNode();
            ObjectNode part = objectMapper.createObjectNode();
            part.put("text", userContent);
            parts.add(part);
            userMsg.set("parts", parts);
            contents.add(userMsg);
            body.set("contents", contents);

            ObjectNode genConfig = objectMapper.createObjectNode();
            genConfig.put("maxOutputTokens", maxOutputTokens);
            if (responseSchemaJson != null && !responseSchemaJson.isBlank()) {
                genConfig.put("responseMimeType", "application/json");
                genConfig.set("responseSchema", objectMapper.readTree(responseSchemaJson));
            }
            body.set("generationConfig", genConfig);

            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new GeminiException("요청 바디 직렬화 실패: " + e.getMessage(), e);
        }
    }

    private String extractText(JsonNode resp) {
        JsonNode candidates = resp.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) throw new GeminiException("Gemini 응답에 candidates 없음");
        String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText(null);
        if (text == null || text.isBlank()) throw new GeminiException("Gemini 응답 텍스트 비어있음");
        return text;
    }

    private boolean isTransient(GeminiException e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("503") || msg.contains("429") || msg.contains("timeout"));
    }
}
