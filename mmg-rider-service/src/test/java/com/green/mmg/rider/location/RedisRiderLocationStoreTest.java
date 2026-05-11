package com.green.mmg.rider.location;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.green.mmg.rider.location.model.RiderLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 5-R5: RedisRiderLocationStore 단위 (mock RedisTemplate + 실 ObjectMapper).
 *
 * <p>4-C {@code RedisRefreshTokenStoreTest} 패턴 일관 (mock RedisTemplate + ArgumentCaptor 키/값/TTL).
 * ObjectMapper는 mock 대신 실 Bean 사용 (JavaTimeModule 등록) — JSON 직렬화 결과 자체 검증.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisRiderLocationStore — save / get / delete (mock RedisTemplate, 실 ObjectMapper)")
class RedisRiderLocationStoreTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private ObjectMapper objectMapper;
    private RedisRiderLocationStore store;

    private static final long RIDER_NO = 5L;
    private static final String EXPECTED_KEY = "rider:loc:5";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        store = new RedisRiderLocationStore(redisTemplate, objectMapper);
    }

    @Nested
    @DisplayName("save")
    class Save {
        @BeforeEach
        void wireValueOps() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("키 'rider:loc:{riderNo}' + JSON value + TTL 30s 동결")
        void save_capturesKeyJsonAndTtl() {
            LocalDateTime now = LocalDateTime.of(2026, 5, 11, 10, 30, 0);
            RiderLocation loc = new RiderLocation(35.125, 128.456, now);

            store.save(RIDER_NO, loc);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            verify(valueOps).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

            assertThat(keyCaptor.getValue()).isEqualTo(EXPECTED_KEY);
            assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofSeconds(30));
            assertThat(valueCaptor.getValue())
                    .contains("\"lat\":35.125")
                    .contains("\"lng\":128.456")
                    .contains("\"updatedAt\":\"2026-05-11T10:30:00\"");
        }
    }

    @Nested
    @DisplayName("get")
    class Get {
        @BeforeEach
        void wireValueOps() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("정상: JSON 역직렬화 후 RiderLocation 필드 동결")
        void get_existingJson_returnsRiderLocation() {
            String json = "{\"lat\":35.125,\"lng\":128.456,\"updatedAt\":\"2026-05-11T10:30:00\"}";
            when(valueOps.get(EXPECTED_KEY)).thenReturn(json);

            Optional<RiderLocation> result = store.get(RIDER_NO);

            assertThat(result).isPresent();
            RiderLocation loc = result.get();
            assertThat(loc.lat()).isEqualTo(35.125);
            assertThat(loc.lng()).isEqualTo(128.456);
            assertThat(loc.updatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 11, 10, 30, 0));
        }

        @Test
        @DisplayName("미존재(NULL/만료): Optional.empty()")
        void get_missingKey_returnsEmpty() {
            when(valueOps.get(EXPECTED_KEY)).thenReturn(null);

            Optional<RiderLocation> result = store.get(RIDER_NO);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {
        @Test
        @DisplayName("키 'rider:loc:{riderNo}' delete 호출")
        void delete_callsWithKey() {
            store.delete(RIDER_NO);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisTemplate).delete(keyCaptor.capture());

            assertThat(keyCaptor.getValue()).isEqualTo(EXPECTED_KEY);
        }
    }
}
