package com.green.mmg.rider.location;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.rider.internal.dto.RiderInternalLocationRes;
import com.green.mmg.rider.location.dto.LocationUpdateReq;
import com.green.mmg.rider.location.model.RiderLocation;
import com.green.mmg.rider.rider.RiderRepository;
import com.green.mmg.rider.rider.model.Rider;
import com.green.mmg.rider.rider.model.RiderStatus;
import com.green.mmg.rider.rider.model.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 5-R5: LocationService 단위 (가짜 0건, CLAUDE.md §6.5).
 *
 * <p>publishLocation 6건 + getInternalLocation 2건 = 8건. R3-b DeliveryServiceTest 패턴 일관.</p>
 */
@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    private static final long CALLER_USER_NO = 42L;
    private static final long RIDER_NO = 5L;

    @Mock private RiderRepository riderRepository;
    @Mock private RiderLocationStore riderLocationStore;

    @InjectMocks private LocationService locationService;

    private Rider rider;

    @BeforeEach
    void setUp() {
        rider = spy(new Rider(
                CALLER_USER_NO, "11-22-3344-55", "2종보통", VehicleType.MOTORBIKE,
                "신한", "110-1", "홍길동"));
        lenient().when(rider.getRiderNo()).thenReturn(RIDER_NO);
    }

    @Nested
    @DisplayName("publishLocation (6건)")
    class PublishLocation {

        @Test
        @DisplayName("ACTIVE happy: Store.save 호출 + RiderLocation 필드 박제")
        void active_happy_savesLocation() {
            rider.approve();
            when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));

            LocationUpdateReq req = new LocationUpdateReq(35.125, 128.456);
            LocalDateTime before = LocalDateTime.now();
            locationService.publishLocation(CALLER_USER_NO, req);
            LocalDateTime after = LocalDateTime.now();

            ArgumentCaptor<RiderLocation> locCaptor = ArgumentCaptor.forClass(RiderLocation.class);
            verify(riderLocationStore).save(eq(RIDER_NO), locCaptor.capture());
            RiderLocation captured = locCaptor.getValue();
            assertThat(captured.lat()).isEqualTo(35.125);
            assertThat(captured.lng()).isEqualTo(128.456);
            assertThat(captured.updatedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("EATING happy: 송신 가능 (PUBLISHABLE_STATUSES 박제, spy status 박제)")
        void eating_happy_savesLocation() {
            when(rider.getStatus()).thenReturn(RiderStatus.EATING);
            when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));

            locationService.publishLocation(CALLER_USER_NO, new LocationUpdateReq(35.0, 128.0));

            verify(riderLocationStore).save(eq(RIDER_NO), any(RiderLocation.class));
        }

        @Test
        @DisplayName("PENDING: BAD_REQUEST + Store 미호출")
        void pending_throwsBadRequest() {
            // rider 기본 status = PENDING
            when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));

            assertThatThrownBy(() -> locationService.publishLocation(
                    CALLER_USER_NO, new LocationUpdateReq(35.0, 128.0)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("위치 송신 가능 상태가 아닙니다")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(riderLocationStore, never()).save(anyLong(), any(RiderLocation.class));
        }

        @Test
        @DisplayName("SUSPENDED: BAD_REQUEST + Store 미호출")
        void suspended_throwsBadRequest() {
            when(rider.getStatus()).thenReturn(RiderStatus.SUSPENDED);
            when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));

            assertThatThrownBy(() -> locationService.publishLocation(
                    CALLER_USER_NO, new LocationUpdateReq(35.0, 128.0)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(riderLocationStore, never()).save(anyLong(), any(RiderLocation.class));
        }

        @Test
        @DisplayName("rider 부재: NOT_FOUND")
        void noRider_throwsNotFound() {
            when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> locationService.publishLocation(
                    CALLER_USER_NO, new LocationUpdateReq(35.0, 128.0)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(riderLocationStore, never()).save(anyLong(), any(RiderLocation.class));
        }

        @Test
        @DisplayName("Redis 다운: D1 throw 그대로 전파 (5xx, best-effort 회피)")
        void redisDown_propagatesException() {
            rider.approve();
            when(riderRepository.findByUserNo(CALLER_USER_NO)).thenReturn(Optional.of(rider));
            doThrow(new RedisConnectionFailureException("connection refused"))
                    .when(riderLocationStore).save(eq(RIDER_NO), any(RiderLocation.class));

            assertThatThrownBy(() -> locationService.publishLocation(
                    CALLER_USER_NO, new LocationUpdateReq(35.0, 128.0)))
                    .isInstanceOf(RedisConnectionFailureException.class)
                    .hasMessageContaining("connection refused");
        }
    }

    @Nested
    @DisplayName("getInternalLocation (2건)")
    class GetInternalLocation {

        @Test
        @DisplayName("Store 존재: 200 dto 반환")
        void existing_returnsDto() {
            LocalDateTime now = LocalDateTime.of(2026, 5, 11, 10, 30, 0);
            RiderLocation loc = new RiderLocation(35.125, 128.456, now);
            when(riderLocationStore.get(RIDER_NO)).thenReturn(Optional.of(loc));

            RiderInternalLocationRes res = locationService.getInternalLocation(RIDER_NO);

            assertThat(res.riderNo()).isEqualTo(RIDER_NO);
            assertThat(res.lat()).isEqualTo(35.125);
            assertThat(res.lng()).isEqualTo(128.456);
            assertThat(res.updatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Store 부재(NULL/만료): NOT_FOUND")
        void missing_throwsNotFound() {
            when(riderLocationStore.get(RIDER_NO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> locationService.getInternalLocation(RIDER_NO))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("위치 송신 0회 또는 TTL 만료")
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getActiveLocations (Group 10, 2026-05-17)")
    class GetActiveLocations {

        @Test
        @DisplayName("Store 다건: List 변환 결과 정확 (riderNo/lat/lng/updatedAt)")
        void multipleStored_returnsList() {
            LocalDateTime t1 = LocalDateTime.of(2026, 5, 17, 10, 0, 0);
            LocalDateTime t2 = LocalDateTime.of(2026, 5, 17, 10, 5, 0);
            Map<Long, RiderLocation> stored = new HashMap<>();
            stored.put(1L, new RiderLocation(35.10, 128.20, t1));
            stored.put(2L, new RiderLocation(35.30, 128.40, t2));
            when(riderLocationStore.getAll()).thenReturn(stored);

            List<RiderInternalLocationRes> res = locationService.getActiveLocations();

            assertThat(res).hasSize(2);
            Map<Long, RiderInternalLocationRes> byRider = new HashMap<>();
            for (RiderInternalLocationRes r : res) byRider.put(r.riderNo(), r);
            assertThat(byRider.get(1L).lat()).isEqualTo(35.10);
            assertThat(byRider.get(1L).lng()).isEqualTo(128.20);
            assertThat(byRider.get(1L).updatedAt()).isEqualTo(t1);
            assertThat(byRider.get(2L).lat()).isEqualTo(35.30);
            assertThat(byRider.get(2L).lng()).isEqualTo(128.40);
            assertThat(byRider.get(2L).updatedAt()).isEqualTo(t2);
        }

        @Test
        @DisplayName("Store 빈 결과: 빈 List (NOT_FOUND throw X — admin 화면 정상)")
        void empty_returnsEmptyList() {
            when(riderLocationStore.getAll()).thenReturn(Map.of());

            List<RiderInternalLocationRes> res = locationService.getActiveLocations();

            assertThat(res).isEmpty();
        }
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
