package com.green.mmg.auth.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 4-C: RedisRefreshTokenStore 단위 (mock RedisTemplate).
 *
 * <p>D4 결정 반영 — RT 문자열 자체 저장. 키 prefix "rt:" + userNo.
 * 단순 동작 mock 충분 — TTL 만료/동시성은 학원 발표 단계 불필요.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisRefreshTokenStore — save / get / delete (mock RedisTemplate)")
class RedisRefreshTokenStoreTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks
    private RedisRefreshTokenStore store;

    private static final long USER_NO = 42L;
    private static final String RT = "eyJhbGciOiJIUzUxMiJ9.fakeRefreshToken";
    private static final String EXPECTED_KEY = "rt:42";

    @Nested
    @DisplayName("save — opsForValue.set 위임")
    class Save {

        @BeforeEach
        void setUp() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("정확한 키/값/TTL 동결 (ArgumentCaptor 3개)")
        void save_capturesKeyValueAndTtl() {
            Duration ttl = Duration.ofDays(15);

            store.save(USER_NO, RT, ttl);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            verify(valueOps).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

            assertThat(keyCaptor.getValue())
                    .as("키 prefix 'rt:' + userNo 동결")
                    .isEqualTo(EXPECTED_KEY);
            assertThat(valueCaptor.getValue())
                    .as("RT 문자열 그대로 저장 (D4 결정)")
                    .isEqualTo(RT);
            assertThat(ttlCaptor.getValue())
                    .as("TTL은 호출자가 전달한 Duration 그대로")
                    .isEqualTo(ttl);
        }

        @Test
        @DisplayName("재로그인 시 같은 키로 덮어쓰기 (단일 디바이스 가정)")
        void save_overwritesSameKey() {
            Duration ttl = Duration.ofDays(15);

            store.save(USER_NO, "old-rt", ttl);
            store.save(USER_NO, "new-rt", ttl);

            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOps, org.mockito.Mockito.times(2)).set(
                    org.mockito.ArgumentMatchers.eq(EXPECTED_KEY),
                    valueCaptor.capture(),
                    org.mockito.ArgumentMatchers.eq(ttl));

            assertThat(valueCaptor.getAllValues())
                    .as("같은 키 두 번 호출 — 두 번째가 덮어쓰는 동작 (Redis SET 의미)")
                    .containsExactly("old-rt", "new-rt");
        }
    }

    @Nested
    @DisplayName("get — Optional 분기 (정상 / null)")
    class Get {

        @BeforeEach
        void setUp() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("정상: 저장된 RT → Optional.of(RT)")
        void get_existingValue_returnsOptionalOf() {
            when(valueOps.get(EXPECTED_KEY)).thenReturn(RT);

            Optional<String> result = store.get(USER_NO);

            assertThat(result).contains(RT);
        }

        @Test
        @DisplayName("미존재: Redis null 반환 → Optional.empty() (NPE 차단)")
        void get_missingKey_returnsEmpty() {
            when(valueOps.get(EXPECTED_KEY)).thenReturn(null);

            Optional<String> result = store.get(USER_NO);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete — redisTemplate.delete 호출 검증")
    class Delete {

        @Test
        @DisplayName("delete: rt:{userNo} 키로 호출 동결 (signout / revoke)")
        void delete_callsWithKey() {
            store.delete(USER_NO);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisTemplate).delete(keyCaptor.capture());

            assertThat(keyCaptor.getValue()).isEqualTo(EXPECTED_KEY);
        }
    }
}
