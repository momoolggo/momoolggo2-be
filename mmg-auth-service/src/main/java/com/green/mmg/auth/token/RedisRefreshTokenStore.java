package com.green.mmg.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Phase 4-C: {@link RefreshTokenStore} Redis 구현 (Spring Data Redis + Lettuce).
 *
 * <p>키 설계: {@code rt:{userNo}} (단일 디바이스). 다중 디바이스는 Phase 5 검토.<br>
 * Redis 연결 실패 시 {@code RedisConnectionFailureException} 등을 그대로 throw —
 * D1 결정(정합성 우선): RT 저장 실패 시 login 자체 실패. best-effort 회피.</p>
 */
@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "rt:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(long userNo, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(key(userNo), refreshToken, ttl);
    }

    @Override
    public Optional<String> get(long userNo) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(userNo)));
    }

    @Override
    public void delete(long userNo) {
        redisTemplate.delete(key(userNo));
    }

    private static String key(long userNo) {
        return KEY_PREFIX + userNo;
    }
}
