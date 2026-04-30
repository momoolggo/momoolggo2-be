package com.green.mmg.main.store;

import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.feign.AuthFeignClient;
import com.green.mmg.main.store.model.StoreOneGetRes;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Phase 2-Backfill-D Step D-2-A: StoreService.storeOneGet — Feign null NPE 처리 검증.
 *
 * <p>수정 전: {@code authFeignClient.getOwner(...)} null 반환 시 {@code owner.getName()}에서 NPE.<br>
 * 수정 후: null 체크 후 {@code BusinessException NOT_FOUND}.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StoreService.storeOneGet — Feign null NPE 처리")
class StoreServiceTest {

    @Mock private StoreMapper storeMapper;
    @Mock private LikedStoreRepository likedStoreRepository;
    @Mock private AuthFeignClient authFeignClient;

    @InjectMocks
    private StoreService storeService;

    private static final long STORE_ID = 21L;
    private static final long OWNER_ID = 100L;

    @Nested
    @DisplayName("storeOneGet — 가게 상세 + 사장 이름 합성")
    class StoreOneGet {

        @Test
        @DisplayName("happy: store 있음 + ownerId 있음 + Feign 정상 → ownerName 합성")
        void happyPath_assemblesOwnerName() {
            StoreOneGetRes res = newStore(OWNER_ID);
            when(storeMapper.findOne(STORE_ID)).thenReturn(res);
            when(authFeignClient.getOwner(OWNER_ID))
                    .thenReturn(new UserBriefDto(OWNER_ID, "사장님", "010-1111", ""));

            StoreOneGetRes result = storeService.storeOneGet(STORE_ID);

            assertThat(result).isSameAs(res);
            assertThat(result.getOwnerName()).isEqualTo("사장님");
            verify(authFeignClient).getOwner(OWNER_ID);
        }

        @Test
        @DisplayName("store 없음 → null 반환 + Feign 미호출 (early return)")
        void storeNotFound_returnsNullAndSkipsFeign() {
            when(storeMapper.findOne(STORE_ID)).thenReturn(null);

            StoreOneGetRes result = storeService.storeOneGet(STORE_ID);

            assertThat(result).isNull();
            verifyNoInteractions(authFeignClient);
        }

        @Test
        @DisplayName("store 있지만 ownerId null → Feign 미호출 + ownerName 미설정 (현재 동작 동결)")
        void ownerIdNull_skipsFeign() {
            StoreOneGetRes res = newStore(null);
            when(storeMapper.findOne(STORE_ID)).thenReturn(res);

            StoreOneGetRes result = storeService.storeOneGet(STORE_ID);

            assertThat(result).isSameAs(res);
            assertThat(result.getOwnerName()).isNull();
            verifyNoInteractions(authFeignClient);
        }

        @Test
        @DisplayName("Feign null 응답 → BusinessException NOT_FOUND '사장 정보를 찾을 수 없습니다.' (수정 핵심)")
        void feignNull_throwsNotFound() {
            StoreOneGetRes res = newStore(OWNER_ID);
            when(storeMapper.findOne(STORE_ID)).thenReturn(res);
            when(authFeignClient.getOwner(OWNER_ID)).thenReturn(null);

            assertThatThrownBy(() -> storeService.storeOneGet(STORE_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("사장 정보를 찾을 수 없습니다.")
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

            assertThat(res.getOwnerName()).isNull();  // setOwnerName 미실행 동결
        }

        @Test
        @DisplayName("Feign 예외(NotFound 404) → 그대로 propagate (catch 없음 동결)")
        void feignException_propagates() {
            StoreOneGetRes res = newStore(OWNER_ID);
            when(storeMapper.findOne(STORE_ID)).thenReturn(res);
            when(authFeignClient.getOwner(OWNER_ID)).thenThrow(
                    new FeignException.NotFound(
                            "owner not found",
                            Request.create(Request.HttpMethod.GET, "/internal/auth/owner/100",
                                    new HashMap<>(), null, StandardCharsets.UTF_8, null),
                            null, null));

            assertThatThrownBy(() -> storeService.storeOneGet(STORE_ID))
                    .isInstanceOf(FeignException.NotFound.class);
        }
    }

    private static StoreOneGetRes newStore(Long ownerId) {
        StoreOneGetRes res = new StoreOneGetRes();
        res.setStoreName("가게");
        res.setOwnerId(ownerId);
        return res;
    }
}
