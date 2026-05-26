package com.green.mmg.rider.location;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.green.mmg.rider.location.model.RiderLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Phase 5-R5: {@link RiderLocationStore} Redis 구현 (Spring Data Redis + Lettuce).
 *
 * <p>키 설계: {@code rider:loc:{riderNo}} — 4-C {@code rt:{userNo}} 네임스페이스 분리 일관.
 * TTL 30s — 송신 5~10s 간격 가정, 2~6 tick 미수신 시 자동 만료 (ADR-005/006 박제).</p>
 *
 * <p>Redis 연결 실패 시 {@code RedisConnectionFailureException} 그대로 throw —
 * D1 결정 (정합성 우선): 위치 저장 실패 시 PUT 자체 5xx. best-effort 회피 (4-C 정착 패턴).</p>
 *
 * <p>JSON 직렬화: Spring Boot autoconfigure ObjectMapper + JavaTimeModule (LocalDateTime ISO-8601).
 * 직렬화 실패 시 {@code IllegalStateException} 변환 → mmg-common GlobalExceptionHandler 5xx.</p>
 */
@Component
@RequiredArgsConstructor
public class RedisRiderLocationStore implements RiderLocationStore {

    private static final String KEY_PREFIX = "rider:loc:";
    private static final Duration TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(long riderNo, RiderLocation location) {
        try {
            String json = objectMapper.writeValueAsString(location);
            redisTemplate.opsForValue().set(key(riderNo), json, TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("RiderLocation JSON 직렬화 실패", e);
        }
    }

    @Override
    public Optional<RiderLocation> get(long riderNo) {
        String json = redisTemplate.opsForValue().get(key(riderNo));
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, RiderLocation.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("RiderLocation JSON 역직렬화 실패", e);
        }
    }

    @Override
    public void delete(long riderNo) {
        redisTemplate.delete(key(riderNo));
    }

    @Override
    public Map<Long, RiderLocation> getAll() {
        Map<Long, RiderLocation> result = new HashMap<>();
        ScanOptions options = ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String fullKey = cursor.next();
                String json = redisTemplate.opsForValue().get(fullKey);
                if (json == null) continue;
                try {
                    long riderNo = Long.parseLong(fullKey.substring(KEY_PREFIX.length()));
                    result.put(riderNo, objectMapper.readValue(json, RiderLocation.class));
                } catch (NumberFormatException | JsonProcessingException e) {
                    throw new IllegalStateException("RiderLocation SCAN 처리 실패 (key=" + fullKey + ")", e);
                }
            }
        }
        return result;
    }

    private static String key(long riderNo) {
        return KEY_PREFIX + riderNo;
    }
}
