package com.green.mmg.admin.settlement.payout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;

/**
 * 토스페이먼츠 지급대행 API 클라이언트
 * 실제 사용 시 사업자 등록 + 토스페이먼츠 계약 필요
 * 문서: https://docs.tosspayments.com/reference/payout
 */
@Component
@Slf4j
public class TossPayoutClient {

    @Value("${toss.payout.secret-key:test_sk_placeholder}")
    private String secretKey;

    @Value("${toss.payout.base-url:https://api.tosspayments.com}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestClient client;

    private RestClient client() {
        if (client == null) {
            String encoded = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
            client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Basic " + encoded)
                    .build();
        }
        return client;
    }

    /**
     * 지급 요청 (바로지급)
     * POST /v1/payouts
     */
    public TossPayoutRes requestPayout(TossPayoutReq req) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("amount", req.amount());
            body.put("bankCode", req.bankCode());
            body.put("accountNumber", req.accountNumber());
            body.put("holderName", req.holderName());
            body.put("requestedAt", req.requestedAt());

            String bodyJson = objectMapper.writeValueAsString(body);
            String respStr = client().post()
                    .uri("/v1/payouts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bodyJson)
                    .retrieve()
                    .body(String.class);

            JsonNode resp = objectMapper.readTree(respStr);
            return new TossPayoutRes(
                    resp.path("payoutId").asText(),
                    resp.path("status").asText(),
                    resp.path("amount").asLong(),
                    resp.path("requestedAt").asText()
            );
        } catch (Exception e) {
            log.error("토스 지급 요청 실패: {}", e.getMessage());
            throw new RuntimeException("토스페이먼츠 지급 요청 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 지급 상태 조회
     * GET /v1/payouts/{payoutId}
     */
    public TossPayoutRes getPayoutStatus(String payoutId) {
        try {
            String respStr = client().get()
                    .uri("/v1/payouts/" + payoutId)
                    .retrieve()
                    .body(String.class);

            JsonNode resp = objectMapper.readTree(respStr);
            return new TossPayoutRes(
                    resp.path("payoutId").asText(),
                    resp.path("status").asText(),
                    resp.path("amount").asLong(),
                    resp.path("requestedAt").asText()
            );
        } catch (Exception e) {
            log.error("토스 지급 조회 실패 payoutId={}: {}", payoutId, e.getMessage());
            throw new RuntimeException("토스페이먼츠 지급 조회 실패: " + e.getMessage(), e);
        }
    }

    // 요청 DTO
    public record TossPayoutReq(
            long amount,
            String bankCode,      // 은행 코드 (예: 004=국민, 088=신한)
            String accountNumber,
            String holderName,
            String requestedAt    // ISO 8601 (예: 2026-05-19T10:00:00+09:00)
    ) {}

    // 응답 DTO
    public record TossPayoutRes(
            String payoutId,
            String status,   // REQUESTED, COMPLETED, FAILED
            long amount,
            String requestedAt
    ) {}
}
