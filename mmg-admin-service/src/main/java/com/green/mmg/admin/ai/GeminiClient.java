package com.green.mmg.admin.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.green.mmg.admin.ai.entity.AiOperationMetric;
import com.green.mmg.admin.ai.exception.AiApiException;
import com.green.mmg.admin.ai.repository.AiOperationMetricRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class GeminiClient {

    private final AiOperationMetricRepository metricsRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public GeminiClient(AiOperationMetricRepository metricsRepo) {
        this.metricsRepo = metricsRepo;
    }

    @Value("${app.gemini.api-key:}")
    private String apiKey;
    @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;
    @Value("${app.gemini.default-model:gemini-2.5-flash}")
    private String defaultModel;
    @Value("${app.gemini.lite-model:gemini-2.5-flash-lite}")
    private String liteModel;

    private RestClient client;

    private RestClient client() {
        if (client == null) client = RestClient.builder().baseUrl(baseUrl).build();
        return client;
    }

    public String getDefaultModel() { return defaultModel; }
    public String getLiteModel() { return liteModel; }
    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    public String generateText(String model, String systemInstruction, String userContent,
                               int maxOutputTokens, boolean jsonMode,
                               String operationType, String targetRef) {
        if (!isConfigured()) throw new AiApiException("Gemini API 키가 설정되지 않음");
        String primary = (model == null || model.isBlank()) ? defaultModel : model;
        try {
            return callOnce(primary, systemInstruction, userContent, maxOutputTokens, jsonMode,
                    operationType, targetRef);
        } catch (AiApiException e1) {
            if (!isTransient(e1)) throw e1;
            try { Thread.sleep(2000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            try {
                log.info("Gemini 재시도 op={} model={}", operationType, primary);
                return callOnce(primary, systemInstruction, userContent, maxOutputTokens, jsonMode,
                        operationType, targetRef);
            } catch (AiApiException e2) {
                if (!isTransient(e2) || primary.equalsIgnoreCase(liteModel)) throw e2;
                log.info("Gemini lite 폴백 op={} from={} to={}", operationType, primary, liteModel);
                return callOnce(liteModel, systemInstruction, userContent, maxOutputTokens, jsonMode,
                        operationType, targetRef);
            }
        }
    }

    private String callOnce(String useModel, String systemInstruction, String userContent,
                            int maxOutputTokens, boolean jsonMode,
                            String operationType, String targetRef) {
        long startMs = System.currentTimeMillis();
        try {
            String bodyJson = buildRequestBodyJson(systemInstruction, userContent, maxOutputTokens, jsonMode);
            String path = "/v1beta/models/" + useModel + ":generateContent?key=" + apiKey;
            String respStr = client().post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bodyJson)
                    .retrieve()
                    .body(String.class);
            if (respStr == null || respStr.isBlank()) throw new AiApiException("Gemini 응답이 비어있음");
            JsonNode resp = objectMapper.readTree(respStr);
            String text = extractText(resp);
            Integer inTok = numberOrNull(resp, "usageMetadata", "promptTokenCount");
            Integer outTok = numberOrNull(resp, "usageMetadata", "candidatesTokenCount");
            logMetric(operationType, targetRef, useModel, inTok, outTok,
                    (int)(System.currentTimeMillis() - startMs), true, null);
            return text;
        } catch (AiApiException e) {
            logMetric(operationType, targetRef, useModel, null, null,
                    (int)(System.currentTimeMillis() - startMs), false, safeMessage(e));
            throw e;
        } catch (Exception e) {
            logMetric(operationType, targetRef, useModel, null, null,
                    (int)(System.currentTimeMillis() - startMs), false, safeMessage(e));
            throw new AiApiException("Gemini 호출 실패: " + e.getMessage(), e);
        }
    }

    private String buildRequestBodyJson(String systemInstruction, String userContent,
                                        int maxOutputTokens, boolean jsonMode) {
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
            if (jsonMode) genConfig.put("responseMimeType", "application/json");
            body.set("generationConfig", genConfig);

            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new AiApiException("요청 바디 직렬화 실패: " + e.getMessage(), e);
        }
    }

    private String extractText(JsonNode resp) {
        JsonNode candidates = resp.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) throw new AiApiException("Gemini 응답에 candidates 없음");
        String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText(null);
        if (text == null || text.isBlank()) throw new AiApiException("Gemini 응답 텍스트 비어있음");
        return text;
    }

    private Integer numberOrNull(JsonNode node, String... path) {
        JsonNode cur = node;
        for (String key : path) cur = cur.path(key);
        return cur.isNumber() ? cur.intValue() : null;
    }

    private void logMetric(String opType, String targetRef, String model,
                           Integer in, Integer out, int durationMs,
                           boolean success, String errorMsg) {
        try {
            metricsRepo.save(new AiOperationMetric(opType, targetRef, model, in, out, durationMs, success, errorMsg));
        } catch (Exception e) {
            log.warn("AI 메트릭 저장 실패: {}", e.getMessage());
        }
    }

    private boolean isTransient(AiApiException e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("503") || msg.contains("429") || msg.contains("timeout"));
    }

    private String safeMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage().substring(0, Math.min(490, e.getMessage().length())) : "unknown";
    }
}
