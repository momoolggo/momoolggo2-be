package com.green.mmg.rider.location.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 라이더 위치 value object — Redis KV `rider:loc:{riderNo}` 직렬화 형태.
 *
 * <p>ADR-005 §위치 송신 박제 일관 — JSON 직렬화 (decision-#28 (가) 채택).
 * Spring Boot ObjectMapper + JavaTimeModule 자동 등록으로 LocalDateTime ISO-8601 직렬화.</p>
 */
public record RiderLocation(double lat, double lng, LocalDateTime updatedAt) {

    @JsonCreator
    public RiderLocation(
            @JsonProperty("lat") double lat,
            @JsonProperty("lng") double lng,
            @JsonProperty("updatedAt") LocalDateTime updatedAt) {
        this.lat = lat;
        this.lng = lng;
        this.updatedAt = updatedAt;
    }
}
