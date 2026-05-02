package com.green.mmg.main.store;

import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.feign.AuthFeignClient;
import com.green.mmg.main.store.model.FavoriteToggleReq;
import com.green.mmg.main.store.model.StoreFavoriteReq;
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
import java.util.List;

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

    // ─────────────────────────────────────────────────────────────────
    // Phase 3-Backfill-A-2: dto.userNo 위조 방지
    // ─────────────────────────────────────────────────────────────────

    private static final long USER_NO = 42L;
    private static final long OTHER_USER_NO = 99L;

    @Nested
    @DisplayName("wishToggle — dto.userNo 위조 방지")
    class WishToggle {

        @Test
        @DisplayName("happy: dto.userNo == caller → likedStoreRepository INSERT (existsByUserNoAndStoreId=false 분기)")
        void happyPath_callerMatchesDto_insertsLike() {
            FavoriteToggleReq req = new FavoriteToggleReq();
            req.setUserNo(USER_NO);
            req.setStoreId(STORE_ID);
            when(likedStoreRepository.existsByUserNoAndStoreId(USER_NO, STORE_ID)).thenReturn(false);

            boolean result = storeService.wishToggle(USER_NO, req);

            assertThat(result).isTrue();
            verify(likedStoreRepository).saveAndFlush(any());
        }

        @Test
        @DisplayName("403 위조: dto.userNo != caller → FORBIDDEN '자신의 계정으로만 찜할 수 있습니다.' + Repository 미호출")
        void dtoUserNoMismatch_throwsForbidden() {
            FavoriteToggleReq req = new FavoriteToggleReq();
            req.setUserNo(OTHER_USER_NO);  // 위조: 다른 사용자 userNo
            req.setStoreId(STORE_ID);

            assertThatThrownBy(() -> storeService.wishToggle(USER_NO, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("자신의 계정으로만 찜할 수 있습니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verifyNoInteractions(likedStoreRepository);
        }
    }

    @Nested
    @DisplayName("checkWish — dto.userNo 위조 방지")
    class CheckWish {

        @Test
        @DisplayName("happy: dto.userNo == caller → existsByUserNoAndStoreId 위임")
        void happyPath_callerMatches_returnsExist() {
            FavoriteToggleReq req = new FavoriteToggleReq();
            req.setUserNo(USER_NO);
            req.setStoreId(STORE_ID);
            when(likedStoreRepository.existsByUserNoAndStoreId(USER_NO, STORE_ID)).thenReturn(true);

            boolean result = storeService.checkWish(USER_NO, req);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("403 위조: dto.userNo != caller → FORBIDDEN '자신의 계정으로만 조회할 수 있습니다.' + Repository 미호출")
        void dtoUserNoMismatch_throwsForbidden() {
            FavoriteToggleReq req = new FavoriteToggleReq();
            req.setUserNo(OTHER_USER_NO);
            req.setStoreId(STORE_ID);

            assertThatThrownBy(() -> storeService.checkWish(USER_NO, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("자신의 계정으로만 조회할 수 있습니다.");

            verifyNoInteractions(likedStoreRepository);
        }
    }

    @Nested
    @DisplayName("getStoreReviews — Feign batch null 처리 (A-4 패턴 전파)")
    class GetStoreReviews {

        @Test
        @DisplayName("happy: rows 있음 + Feign 정상 → userName 합성")
        void happyPath_assemblesUserName() {
            java.util.Map<String, Object> row1 = new java.util.HashMap<>();
            row1.put("reviewId", 100L);
            row1.put("userNo", 42L);
            when(storeMapper.getStoreReviews(STORE_ID)).thenReturn(List.of(row1));
            when(authFeignClient.getUsers(any())).thenReturn(List.of(
                    new UserBriefDto(42L, "준하", "010-1111", "")));

            List<java.util.Map<String, Object>> result = storeService.getStoreReviews(STORE_ID);

            assertThat(result.get(0).get("userName")).isEqualTo("준하");
        }

        @Test
        @DisplayName("rows 빈 리스트 → Feign 미호출 (early return)")
        void emptyRows_skipsFeign() {
            when(storeMapper.getStoreReviews(STORE_ID)).thenReturn(List.of());

            List<java.util.Map<String, Object>> result = storeService.getStoreReviews(STORE_ID);

            assertThat(result).isEmpty();
            verifyNoInteractions(authFeignClient);
        }

        @Test
        @DisplayName("Feign null → 빈 Map → 모든 row의 userName=빈 문자열 (NPE 차단)")
        void feignNull_userNamesAreBlank() {
            java.util.Map<String, Object> row1 = new java.util.HashMap<>();
            row1.put("reviewId", 100L);
            row1.put("userNo", 42L);
            when(storeMapper.getStoreReviews(STORE_ID)).thenReturn(List.of(row1));
            when(authFeignClient.getUsers(any())).thenReturn(null);

            List<java.util.Map<String, Object>> result = storeService.getStoreReviews(STORE_ID);

            assertThat(result.get(0).get("userName")).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("getWishListResponse — dto.userNo 위조 방지")
    class GetWishListResponse {

        @Test
        @DisplayName("happy: dto.userNo == caller → favoriteList + countByUserNo 합성")
        void happyPath_callerMatches_assemblesResponse() {
            StoreFavoriteReq req = new StoreFavoriteReq();
            req.setUserNo(USER_NO);
            req.setCurrentPage(1);
            req.setSize(10);
            when(storeMapper.favoriteList(req)).thenReturn(List.of());
            when(likedStoreRepository.countByUserNo(USER_NO)).thenReturn(3L);

            var result = storeService.getWishListResponse(USER_NO, req);

            assertThat(result.get("totalCount")).isEqualTo(3);
            assertThat(((List<?>) result.get("list"))).isEmpty();
        }

        @Test
        @DisplayName("403 위조: dto.userNo != caller → FORBIDDEN '본인 찜 목록만 조회 가능합니다.' + Repository/Mapper 미호출")
        void dtoUserNoMismatch_throwsForbidden() {
            StoreFavoriteReq req = new StoreFavoriteReq();
            req.setUserNo(OTHER_USER_NO);

            assertThatThrownBy(() -> storeService.getWishListResponse(USER_NO, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 찜 목록만 조회 가능합니다.");

            verifyNoInteractions(likedStoreRepository);
            verifyNoInteractions(storeMapper);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Phase 3-Backfill-C-2: storeSearchList 입력 검증 동결
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("storeSearchList — null/blank early return")
    class StoreSearchList {

        @Test
        @DisplayName("searchText null → 빈 리스트 + StoreMapper 미호출 (early return)")
        void nullSearchText_returnsEmptyAndSkipsMapper() {
            List<?> result = storeService.storeSearchList(null);

            assertThat(result).isEmpty();
            verifyNoInteractions(storeMapper);
        }

        @Test
        @DisplayName("searchText 공백/blank → 빈 리스트 + StoreMapper 미호출 (trim 후 isEmpty)")
        void blankSearchText_returnsEmptyAndSkipsMapper() {
            List<?> result = storeService.storeSearchList("   ");

            assertThat(result).isEmpty();
            verifyNoInteractions(storeMapper);
        }
    }
}
