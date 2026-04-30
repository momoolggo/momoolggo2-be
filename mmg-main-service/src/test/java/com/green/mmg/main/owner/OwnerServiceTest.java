package com.green.mmg.main.owner;

import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.common.feign.AuthFeignClient;
import com.green.mmg.main.owner.model.OwnerMenuRegReq;
import com.green.mmg.main.owner.model.OwnerMenuRes;
import com.green.mmg.main.owner.model.OwnerMenuUpdateReq;
import com.green.mmg.main.owner.model.OwnerOrderRes;
import com.green.mmg.main.owner.model.OwnerOrderStateReq;
import com.green.mmg.main.owner.model.OwnerSalesStatsRes;
import com.green.mmg.main.owner.model.OwnerSalesRankingRes;
import com.green.mmg.main.owner.model.OwnerStoreRegReq;
import com.green.mmg.main.owner.model.OwnerStoreRes;
import com.green.mmg.main.owner.model.OwnerStoreUpdateReq;
import com.green.mmg.main.owner.model.OwnerStoreUpdateStatusReq;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * OwnerService 단위 테스트 — Phase 2-Backfill-D-bis 권한 분기 적용 완료.
 *
 * <p>모든 메서드는 {@code callerOwnerNo} (JWT principal) + 본인 자원 검증을 거친다:
 * <ul>
 *   <li>{@code verifyStoreOwner}: store_id → store.owner_id 비교</li>
 *   <li>{@code verifyOrderOwner}: order_id → orders.store_id → store.owner_id 비교</li>
 *   <li>{@code verifyMenuOwner}: menu_id → menu_category → store.owner_id 비교</li>
 *   <li>{@code verifyCategoryOwner}: category_id → menu_category → store.owner_id 비교</li>
 *   <li>{@code registerStore}: dto.userId 위조 방지 (불일치 시 FORBIDDEN — 옵션 B)</li>
 * </ul>
 * Cart의 {@code verifyCartItemOwner}와 동일 패턴 (Repository 교차 검증).</p>
 *
 * <p>학원 DB / Spring 컨텍스트 의존 0 — 순수 Mockito.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerService — 권한 분기 + 분기별 동작")
class OwnerServiceTest {

    @Mock private OwnerMapper ownerMapper;
    @Mock private AuthFeignClient authFeignClient;

    @InjectMocks
    private OwnerService ownerService;

    private static final long USER_ID = 100L;
    private static final long CATEGORY_ID = 7L;
    private static final long STORE_ID = 21L;
    private static final long MENU_ID = 17L;
    private static final long ORDER_ID = 391_000_001L;

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("registerStore — dto.userId 위조 방지 + 등록 + storeCategory/defaultMenuCategory")
    class RegisterStore {

        @Test
        @DisplayName("happy: dto.userId == caller → registerStore → categories (InOrder 동결)")
        void happyPath_registersAndChainsCategories() {
            OwnerStoreRegReq dto = newRegReq(USER_ID, CATEGORY_ID, "맛집");
            when(ownerMapper.registerStore(dto)).thenReturn(1);

            ownerService.registerStore(USER_ID, dto);

            InOrder inOrder = inOrder(ownerMapper);
            inOrder.verify(ownerMapper).registerStore(dto);
            inOrder.verify(ownerMapper).registerStoreCategory(USER_ID, CATEGORY_ID);
            inOrder.verify(ownerMapper).registerDefaultMenuCategory(USER_ID);
        }

        @Test
        @DisplayName("403 위조 방지: dto.userId != caller → FORBIDDEN '자신의 계정으로만 가게를 등록할 수 있습니다.' + Mapper 미호출")
        void dtoUserIdMismatch_throwsForbidden() {
            // 프론트가 다른 userId 위조 시도
            OwnerStoreRegReq dto = newRegReq(USER_ID + 1, CATEGORY_ID, "위조");

            assertThatThrownBy(() -> ownerService.registerStore(USER_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("자신의 계정으로만 가게를 등록할 수 있습니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verifyNoInteractions(ownerMapper);
        }

        @Test
        @DisplayName("실패: dto.userId == caller + result==0 → RuntimeException '가게 등록 실패' + 후속 미호출")
        void registerFails_throwsAndShortCircuits() {
            OwnerStoreRegReq dto = newRegReq(USER_ID, CATEGORY_ID, "실패");
            when(ownerMapper.registerStore(dto)).thenReturn(0);

            assertThatThrownBy(() -> ownerService.registerStore(USER_ID, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("가게 등록 실패");

            verify(ownerMapper, never()).registerStoreCategory(anyLong(), anyLong());
            verify(ownerMapper, never()).registerDefaultMenuCategory(anyLong());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateStore — verifyStoreOwner + Long.parseLong + result 검증")
    class UpdateStore {

        @Test
        @DisplayName("happy: 본인 가게 → updateStore 호출")
        void happyPath_updates() {
            OwnerStoreUpdateReq dto = newUpdateReq("21", "변경");
            when(ownerMapper.findStoreOwnerByStoreId(21L)).thenReturn(USER_ID);
            when(ownerMapper.updateStore(dto)).thenReturn(1);

            ownerService.updateStore(USER_ID, dto);

            verify(ownerMapper).updateStore(dto);
        }

        @Test
        @DisplayName("403: 다른 점주 가게 → FORBIDDEN + updateStore 미호출")
        void otherOwner_throwsForbidden() {
            OwnerStoreUpdateReq dto = newUpdateReq("21", "변경");
            when(ownerMapper.findStoreOwnerByStoreId(21L)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.updateStore(USER_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게만 접근 가능합니다.");

            verify(ownerMapper, never()).updateStore(any());
        }

        @Test
        @DisplayName("400: storeId 비숫자 → BAD_REQUEST 'storeId 형식이 잘못되었습니다.'")
        void invalidStoreIdFormat_throwsBadRequest() {
            OwnerStoreUpdateReq dto = newUpdateReq("abc", "x");

            assertThatThrownBy(() -> ownerService.updateStore(USER_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("storeId 형식이 잘못되었습니다.")
                    .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

            verifyNoInteractions(ownerMapper);
        }

        @Test
        @DisplayName("실패: 권한 통과 + result==0 → RuntimeException '가게 정보 수정 실패'")
        void updateFails_throws() {
            OwnerStoreUpdateReq dto = newUpdateReq("21", "변경");
            when(ownerMapper.findStoreOwnerByStoreId(21L)).thenReturn(USER_ID);
            when(ownerMapper.updateStore(dto)).thenReturn(0);

            assertThatThrownBy(() -> ownerService.updateStore(USER_ID, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("가게 정보 수정 실패");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateStoreStatus — verifyStoreOwner + update + getStoreById")
    class UpdateStoreStatus {

        @Test
        @DisplayName("happy: 본인 가게 → updateStoreStatus → getStoreById 호출")
        void happyPath_updates() {
            OwnerStoreUpdateStatusReq dto = new OwnerStoreUpdateStatusReq();
            dto.setStoreId(STORE_ID);
            dto.setHoliday("월");
            dto.setNotice("공지");
            dto.setState(1);
            OwnerStoreRes fetched = new OwnerStoreRes();
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID);
            when(ownerMapper.getStoreById(STORE_ID)).thenReturn(fetched);

            OwnerStoreRes result = ownerService.updateStoreStatus(USER_ID, dto);

            assertThat(result).isSameAs(fetched);
            InOrder inOrder = inOrder(ownerMapper);
            inOrder.verify(ownerMapper).findStoreOwnerByStoreId(STORE_ID);
            inOrder.verify(ownerMapper).updateStoreStatus(dto);
            inOrder.verify(ownerMapper).getStoreById(STORE_ID);
        }

        @Test
        @DisplayName("403: 다른 점주 가게 → FORBIDDEN + update/getStore 미호출")
        void otherOwner_throwsForbidden() {
            OwnerStoreUpdateStatusReq dto = new OwnerStoreUpdateStatusReq();
            dto.setStoreId(STORE_ID);
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.updateStoreStatus(USER_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게만 접근 가능합니다.");

            verify(ownerMapper, never()).updateStoreStatus(any());
            verify(ownerMapper, never()).getStoreById(anyLong());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteStore — verifyStoreOwner + delete")
    class DeleteStore {

        @Test
        @DisplayName("happy: 본인 가게 → deleteStore 호출")
        void happyPath_deletes() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID);
            when(ownerMapper.deleteStore(STORE_ID)).thenReturn(1);

            ownerService.deleteStore(USER_ID, STORE_ID);

            verify(ownerMapper).deleteStore(STORE_ID);
        }

        @Test
        @DisplayName("403: 다른 점주 가게 → FORBIDDEN + deleteStore 미호출")
        void otherOwner_throwsForbidden() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.deleteStore(USER_ID, STORE_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게만 접근 가능합니다.");

            verify(ownerMapper, never()).deleteStore(anyLong());
        }

        @Test
        @DisplayName("실패: 권한 통과 + result==0 → RuntimeException '삭제할 가게를 찾을 수 없습니다.'")
        void deleteFails_throws() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID);
            when(ownerMapper.deleteStore(STORE_ID)).thenReturn(0);

            assertThatThrownBy(() -> ownerService.deleteStore(USER_ID, STORE_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("삭제할 가게를 찾을 수 없습니다");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getMyStore / getMyStores — caller로 본인 가게만 조회")
    class GetMyStore {

        @Test
        @DisplayName("getMyStore: caller userNo로 ownerMapper.getMyStore 위임")
        void getMyStore_delegates() {
            OwnerStoreRes store = new OwnerStoreRes();
            when(ownerMapper.getMyStore(USER_ID)).thenReturn(store);

            OwnerStoreRes result = ownerService.getMyStore(USER_ID);

            assertThat(result).isSameAs(store);
            verify(ownerMapper).getMyStore(USER_ID);
        }

        @Test
        @DisplayName("getMyStores: caller userNo로 ownerMapper.getMyStores 위임")
        void getMyStores_delegates() {
            when(ownerMapper.getMyStores(USER_ID)).thenReturn(List.of());

            List<OwnerStoreRes> result = ownerService.getMyStores(USER_ID);

            assertThat(result).isEmpty();
            verify(ownerMapper).getMyStores(USER_ID);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getOrders — verifyStoreOwner + Feign batch")
    class GetOrders {

        @Test
        @DisplayName("happy: 본인 가게 → distinct batch + name/tel 합성")
        void happyPath_batchedFeignAndAssembled() {
            OwnerOrderRes o1 = newOrder(391_000_001L, 100L);
            OwnerOrderRes o2 = newOrder(391_000_002L, 100L);
            OwnerOrderRes o3 = newOrder(391_000_003L, 200L);
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID);
            when(ownerMapper.getOrders(STORE_ID, 1, "2026-04-30"))
                    .thenReturn(List.of(o1, o2, o3));
            when(authFeignClient.getUsers(anyList())).thenReturn(List.of(
                    new UserBriefDto(100L, "준하", "010-1111", ""),
                    new UserBriefDto(200L, "민수", "010-2222", "")
            ));

            List<OwnerOrderRes> result = ownerService.getOrders(USER_ID, STORE_ID, 1, "2026-04-30");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
            verify(authFeignClient).getUsers(captor.capture());
            assertThat(captor.getValue()).containsExactlyInAnyOrder(100L, 200L);
            assertThat(result.get(0).getCustomerName()).isEqualTo("준하");
            assertThat(result.get(2).getCustomerName()).isEqualTo("민수");
        }

        @Test
        @DisplayName("403: 다른 점주 가게 주문 조회 → FORBIDDEN + Mapper/Feign 미호출")
        void otherOwnerStore_throwsForbiddenAndShortCircuits() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.getOrders(USER_ID, STORE_ID, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게만 접근 가능합니다.");

            verify(ownerMapper, never()).getOrders(anyLong(), any(), any());
            verifyNoInteractions(authFeignClient);
        }

        @Test
        @DisplayName("happy: orders 빈 리스트 → Feign 미호출 (early return)")
        void emptyOrders_skipsFeign() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID);
            when(ownerMapper.getOrders(STORE_ID, null, null)).thenReturn(List.of());

            List<OwnerOrderRes> result = ownerService.getOrders(USER_ID, STORE_ID, null, null);

            assertThat(result).isEmpty();
            verifyNoInteractions(authFeignClient);
        }

        @Test
        @DisplayName("happy: Feign 응답에 없는 userNo → if (u != null) 분기 → 미설정 동결")
        void userMissingFromFeign_keepsNullFields() {
            OwnerOrderRes o1 = newOrder(391_000_001L, 100L);
            OwnerOrderRes o2 = newOrder(391_000_002L, 999L);
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID);
            when(ownerMapper.getOrders(STORE_ID, null, null)).thenReturn(List.of(o1, o2));
            when(authFeignClient.getUsers(anyList())).thenReturn(List.of(
                    new UserBriefDto(100L, "준하", "010-1111", "")
            ));

            List<OwnerOrderRes> result = ownerService.getOrders(USER_ID, STORE_ID, null, null);

            assertThat(result.get(0).getCustomerName()).isEqualTo("준하");
            assertThat(result.get(1).getCustomerName()).isNull();
        }

        @Test
        @DisplayName("Feign 예외 → 그대로 propagate (try-catch 없음 동결)")
        void feignException_propagates() {
            OwnerOrderRes o1 = newOrder(391_000_001L, 100L);
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID);
            when(ownerMapper.getOrders(STORE_ID, null, null)).thenReturn(List.of(o1));
            when(authFeignClient.getUsers(anyList())).thenThrow(
                    new FeignException.ServiceUnavailable(
                            "auth down",
                            Request.create(Request.HttpMethod.GET, "/internal/auth/users",
                                    new HashMap<>(), null, StandardCharsets.UTF_8, null),
                            null, null));

            assertThatThrownBy(() -> ownerService.getOrders(USER_ID, STORE_ID, null, null))
                    .isInstanceOf(FeignException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateOrderState — verifyOrderOwner + 상태 변경")
    class UpdateOrderState {

        @Test
        @DisplayName("happy: 본인 가게 주문 → updateOrderState 호출")
        void happyPath_updates() {
            OwnerOrderStateReq req = newStateReq(ORDER_ID, 3);
            when(ownerMapper.findStoreOwnerByOrderId(ORDER_ID)).thenReturn(USER_ID);
            when(ownerMapper.updateOrderState(req)).thenReturn(1);

            ownerService.updateOrderState(USER_ID, req);

            verify(ownerMapper).updateOrderState(req);
        }

        @Test
        @DisplayName("403: 다른 점주 주문 → FORBIDDEN '본인 가게의 주문만 접근 가능합니다.' + updateOrderState 미호출")
        void otherOwnerOrder_throwsForbidden() {
            OwnerOrderStateReq req = newStateReq(ORDER_ID, 3);
            when(ownerMapper.findStoreOwnerByOrderId(ORDER_ID)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.updateOrderState(USER_ID, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게의 주문만 접근 가능합니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verify(ownerMapper, never()).updateOrderState(any());
        }

        @Test
        @DisplayName("404: orderId 미존재 → NOT_FOUND '주문을 찾을 수 없습니다.'")
        void orderNotFound_throwsNotFound() {
            OwnerOrderStateReq req = newStateReq(ORDER_ID, 3);
            when(ownerMapper.findStoreOwnerByOrderId(ORDER_ID)).thenReturn(null);

            assertThatThrownBy(() -> ownerService.updateOrderState(USER_ID, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("주문을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("실패: 권한 통과 + result==0 → RuntimeException '주문 상태 변경 실패'")
        void updateFails_throws() {
            OwnerOrderStateReq req = newStateReq(ORDER_ID, 3);
            when(ownerMapper.findStoreOwnerByOrderId(ORDER_ID)).thenReturn(USER_ID);
            when(ownerMapper.updateOrderState(req)).thenReturn(0);

            assertThatThrownBy(() -> ownerService.updateOrderState(USER_ID, req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("주문 상태 변경 실패");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteOrder — @Transactional, verifyOrderOwner + 순차 삭제")
    class DeleteOrder {

        @Test
        @DisplayName("happy: 본인 주문 → InOrder verifyOwner → deleteOrderDetail → deleteOrder")
        void deletesInOrder() {
            when(ownerMapper.findStoreOwnerByOrderId(ORDER_ID)).thenReturn(USER_ID);

            ownerService.deleteOrder(USER_ID, ORDER_ID);

            InOrder inOrder = inOrder(ownerMapper);
            inOrder.verify(ownerMapper).findStoreOwnerByOrderId(ORDER_ID);
            inOrder.verify(ownerMapper).deleteOrderDetail(ORDER_ID);
            inOrder.verify(ownerMapper).deleteOrder(ORDER_ID);
        }

        @Test
        @DisplayName("403: 다른 점주 주문 → FORBIDDEN + 삭제 미호출")
        void otherOwnerOrder_throwsForbiddenAndShortCircuits() {
            when(ownerMapper.findStoreOwnerByOrderId(ORDER_ID)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.deleteOrder(USER_ID, ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게의 주문만 접근 가능합니다.");

            verify(ownerMapper, never()).deleteOrderDetail(anyLong());
            verify(ownerMapper, never()).deleteOrder(anyLong());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("registerMenu — verifyStoreOwner + 등록 + getMenuById 조립")
    class RegisterMenu {

        @Test
        @DisplayName("happy: 본인 가게 → registerMenu → getMenuById (InOrder 동결)")
        void happyPath_registerThenFetch() {
            OwnerMenuRegReq dto = newMenuRegReq(MENU_ID, STORE_ID, "피자", 15000);
            OwnerMenuRes registered = newMenuRes(MENU_ID, "피자");
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID);
            when(ownerMapper.getMenuById(MENU_ID)).thenReturn(registered);

            OwnerMenuRes result = ownerService.registerMenu(USER_ID, dto);

            assertThat(result).isSameAs(registered);
            InOrder inOrder = inOrder(ownerMapper);
            inOrder.verify(ownerMapper).findStoreOwnerByStoreId(STORE_ID);
            inOrder.verify(ownerMapper).registerMenu(dto);
            inOrder.verify(ownerMapper).getMenuById(MENU_ID);
        }

        @Test
        @DisplayName("403: 다른 점주 가게에 메뉴 등록 시도 → FORBIDDEN + registerMenu/getMenuById 미호출")
        void otherOwner_throwsForbiddenAndShortCircuits() {
            OwnerMenuRegReq dto = newMenuRegReq(MENU_ID, STORE_ID, "피자", 15000);
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.registerMenu(USER_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게만 접근 가능합니다.");

            verify(ownerMapper, never()).registerMenu(any());
            verify(ownerMapper, never()).getMenuById(anyLong());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateMenu — @Transactional, verifyMenuOwner → result 검증 → getMenuById")
    class UpdateMenu {

        @Test
        @DisplayName("happy: 본인 메뉴 → updateMenu → getMenuById (InOrder 동결)")
        void happyPath_updatesThenFetches() {
            OwnerMenuUpdateReq dto = newMenuUpdateReq(MENU_ID, "변경된 메뉴", 18000);
            OwnerMenuRes updated = newMenuRes(MENU_ID, "변경된 메뉴");
            when(ownerMapper.findStoreOwnerByMenuId(MENU_ID)).thenReturn(USER_ID);
            when(ownerMapper.updateMenu(dto)).thenReturn(1);
            when(ownerMapper.getMenuById(MENU_ID)).thenReturn(updated);

            OwnerMenuRes result = ownerService.updateMenu(USER_ID, dto);

            assertThat(result).isSameAs(updated);
            InOrder inOrder = inOrder(ownerMapper);
            inOrder.verify(ownerMapper).findStoreOwnerByMenuId(MENU_ID);
            inOrder.verify(ownerMapper).updateMenu(dto);
            inOrder.verify(ownerMapper).getMenuById(MENU_ID);
        }

        @Test
        @DisplayName("403: 다른 점주 메뉴 수정 시도 → FORBIDDEN '본인 가게의 메뉴만 접근 가능합니다.' + updateMenu 미호출")
        void otherOwnerMenu_throwsForbiddenAndShortCircuits() {
            OwnerMenuUpdateReq dto = newMenuUpdateReq(MENU_ID, "x", 1);
            when(ownerMapper.findStoreOwnerByMenuId(MENU_ID)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.updateMenu(USER_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게의 메뉴만 접근 가능합니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verify(ownerMapper, never()).updateMenu(any());
        }

        @Test
        @DisplayName("404: menuId 미존재 → NOT_FOUND '메뉴를 찾을 수 없습니다.'")
        void menuNotFound_throwsNotFound() {
            OwnerMenuUpdateReq dto = newMenuUpdateReq(MENU_ID, "x", 1);
            when(ownerMapper.findStoreOwnerByMenuId(MENU_ID)).thenReturn(null);

            assertThatThrownBy(() -> ownerService.updateMenu(USER_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("메뉴를 찾을 수 없습니다.")
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 권한 통과 + result==0 → RuntimeException '메뉴 수정 실패' + getMenuById 미호출")
        void updateFails_throwsAndShortCircuits() {
            OwnerMenuUpdateReq dto = newMenuUpdateReq(MENU_ID, "x", 1);
            when(ownerMapper.findStoreOwnerByMenuId(MENU_ID)).thenReturn(USER_ID);
            when(ownerMapper.updateMenu(dto)).thenReturn(0);

            assertThatThrownBy(() -> ownerService.updateMenu(USER_ID, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("메뉴 수정 실패")
                    .hasMessageContaining("해당 메뉴를 찾을 수 없음");

            verify(ownerMapper, never()).getMenuById(anyLong());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteMenu — @Transactional, verifyMenuOwner → menuId 반환")
    class DeleteMenu {

        @Test
        @DisplayName("happy: 본인 메뉴 → deleteMenu → menuId 반환")
        void happyPath_returnsMenuId() {
            when(ownerMapper.findStoreOwnerByMenuId(MENU_ID)).thenReturn(USER_ID);
            when(ownerMapper.deleteMenu(MENU_ID)).thenReturn(1);

            Long result = ownerService.deleteMenu(USER_ID, MENU_ID);

            assertThat(result).isEqualTo(MENU_ID);
            verify(ownerMapper).deleteMenu(MENU_ID);
        }

        @Test
        @DisplayName("403: 다른 점주 메뉴 삭제 시도 → FORBIDDEN + deleteMenu 미호출")
        void otherOwnerMenu_throwsForbiddenAndShortCircuits() {
            when(ownerMapper.findStoreOwnerByMenuId(MENU_ID)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.deleteMenu(USER_ID, MENU_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게의 메뉴만 접근 가능합니다.");

            verify(ownerMapper, never()).deleteMenu(anyLong());
        }

        @Test
        @DisplayName("실패: 권한 통과 + result==0 → RuntimeException '메뉴 삭제 실패'")
        void deleteFails_throws() {
            when(ownerMapper.findStoreOwnerByMenuId(MENU_ID)).thenReturn(USER_ID);
            when(ownerMapper.deleteMenu(MENU_ID)).thenReturn(0);

            assertThatThrownBy(() -> ownerService.deleteMenu(USER_ID, MENU_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("메뉴 삭제 실패");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getMenusByStoreId — verifyStoreOwner")
    class GetMenusByStoreId {

        @Test
        @DisplayName("happy: 본인 가게 → getMenusByStoreId 위임")
        void happyPath_delegates() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID);
            when(ownerMapper.getMenusByStoreId(STORE_ID)).thenReturn(List.of(newMenuRes(MENU_ID, "피자")));

            List<OwnerMenuRes> result = ownerService.getMenusByStoreId(USER_ID, STORE_ID);

            assertThat(result).hasSize(1);
            verify(ownerMapper).getMenusByStoreId(STORE_ID);
        }

        @Test
        @DisplayName("403: 다른 점주 가게 → FORBIDDEN + getMenusByStoreId 미호출")
        void otherOwner_throwsForbidden() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.getMenusByStoreId(USER_ID, STORE_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게만 접근 가능합니다.");

            verify(ownerMapper, never()).getMenusByStoreId(anyLong());
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // Phase 2-Backfill-D-bis 그룹 ㄹ: 매출 권한 분기
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getSalesStats — verifyStoreOwner")
    class GetSalesStats {

        @Test
        @DisplayName("happy: 본인 가게 → getSalesStats 위임")
        void happyPath_delegates() {
            OwnerSalesStatsRes stats = new OwnerSalesStatsRes();
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID);
            when(ownerMapper.getSalesStats(STORE_ID, "month")).thenReturn(stats);

            OwnerSalesStatsRes result = ownerService.getSalesStats(USER_ID, STORE_ID, "month");

            assertThat(result).isSameAs(stats);
            verify(ownerMapper).getSalesStats(STORE_ID, "month");
        }

        @Test
        @DisplayName("403: 다른 점주 가게 → FORBIDDEN + getSalesStats 미호출")
        void otherOwner_throwsForbidden() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.getSalesStats(USER_ID, STORE_ID, "month"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게만 접근 가능합니다.");

            verify(ownerMapper, never()).getSalesStats(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("getSalesRanking — verifyStoreOwner")
    class GetSalesRanking {

        @Test
        @DisplayName("happy: 본인 가게 → getSalesRanking 위임")
        void happyPath_delegates() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID);
            when(ownerMapper.getSalesRanking(STORE_ID, "week")).thenReturn(List.of());

            List<OwnerSalesRankingRes> result = ownerService.getSalesRanking(USER_ID, STORE_ID, "week");

            assertThat(result).isEmpty();
            verify(ownerMapper).getSalesRanking(STORE_ID, "week");
        }

        @Test
        @DisplayName("403: 다른 점주 가게 → FORBIDDEN + getSalesRanking 미호출")
        void otherOwner_throwsForbidden() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.getSalesRanking(USER_ID, STORE_ID, "week"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게만 접근 가능합니다.");

            verify(ownerMapper, never()).getSalesRanking(anyLong(), any());
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // Phase 2-Backfill-D-bis 그룹 ㅁ: 카테고리 권한 분기
    // ═════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getCategoriesByStoreId — verifyStoreOwner")
    class GetCategoriesByStoreId {

        @Test
        @DisplayName("happy: 본인 가게 → ownerMapper.getCategoriesByStoreId 위임")
        void happyPath_delegates() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID);
            when(ownerMapper.getCategoriesByStoreId(STORE_ID)).thenReturn(List.of(java.util.Map.of("id", 1)));

            List<java.util.Map<String, Object>> result = ownerService.getCategoriesByStoreId(USER_ID, STORE_ID);

            assertThat(result).hasSize(1);
            verify(ownerMapper).getCategoriesByStoreId(STORE_ID);
        }

        @Test
        @DisplayName("403: 다른 점주 가게 → FORBIDDEN '본인 가게만 접근 가능합니다.' + 후속 미호출")
        void otherOwner_throwsForbidden() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID + 1);  // 타인 소유

            assertThatThrownBy(() -> ownerService.getCategoriesByStoreId(USER_ID, STORE_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게만 접근 가능합니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verify(ownerMapper, never()).getCategoriesByStoreId(anyLong());
        }

        @Test
        @DisplayName("404: storeId 미존재 → NOT_FOUND '가게를 찾을 수 없습니다.'")
        void storeNotFound_throwsNotFound() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(null);

            assertThatThrownBy(() -> ownerService.getCategoriesByStoreId(USER_ID, STORE_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("가게를 찾을 수 없습니다.")
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("addCategory — verifyStoreOwner")
    class AddCategory {

        @Test
        @DisplayName("happy: 본인 가게 → addCategory 위임")
        void happyPath_delegates() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID);

            ownerService.addCategory(USER_ID, STORE_ID, "사이드");

            verify(ownerMapper).addCategory(STORE_ID, "사이드");
        }

        @Test
        @DisplayName("403: 다른 점주 → addCategory 미호출")
        void otherOwner_throwsForbiddenAndShortCircuits() {
            when(ownerMapper.findStoreOwnerByStoreId(STORE_ID)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.addCategory(USER_ID, STORE_ID, "x"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게만 접근 가능합니다.");

            verify(ownerMapper, never()).addCategory(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("updateCategory — verifyCategoryOwner")
    class UpdateCategory {

        private static final long CATEGORY_ID = 333L;

        @Test
        @DisplayName("happy: 본인 카테고리 → updateCategory 위임")
        void happyPath_delegates() {
            when(ownerMapper.findStoreOwnerByCategoryId(CATEGORY_ID)).thenReturn(USER_ID);

            ownerService.updateCategory(USER_ID, CATEGORY_ID, "변경");

            verify(ownerMapper).updateCategory(CATEGORY_ID, "변경");
        }

        @Test
        @DisplayName("403: 다른 점주의 카테고리 → FORBIDDEN '본인 가게의 카테고리만 접근 가능합니다.' + 미호출")
        void otherOwnerCategory_throwsForbidden() {
            when(ownerMapper.findStoreOwnerByCategoryId(CATEGORY_ID)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.updateCategory(USER_ID, CATEGORY_ID, "x"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게의 카테고리만 접근 가능합니다.")
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verify(ownerMapper, never()).updateCategory(anyLong(), any());
        }

        @Test
        @DisplayName("404: categoryId 미존재 → NOT_FOUND '카테고리를 찾을 수 없습니다.'")
        void categoryNotFound_throwsNotFound() {
            when(ownerMapper.findStoreOwnerByCategoryId(CATEGORY_ID)).thenReturn(null);

            assertThatThrownBy(() -> ownerService.updateCategory(USER_ID, CATEGORY_ID, "x"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("카테고리를 찾을 수 없습니다.")
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteCategory — verifyCategoryOwner")
    class DeleteCategory {

        private static final long CATEGORY_ID = 333L;

        @Test
        @DisplayName("happy: 본인 카테고리 → deleteCategory 위임")
        void happyPath_delegates() {
            when(ownerMapper.findStoreOwnerByCategoryId(CATEGORY_ID)).thenReturn(USER_ID);

            ownerService.deleteCategory(USER_ID, CATEGORY_ID);

            verify(ownerMapper).deleteCategory(CATEGORY_ID);
        }

        @Test
        @DisplayName("403: 다른 점주의 카테고리 → FORBIDDEN + 미호출")
        void otherOwnerCategory_throwsForbiddenAndShortCircuits() {
            when(ownerMapper.findStoreOwnerByCategoryId(CATEGORY_ID)).thenReturn(USER_ID + 1);

            assertThatThrownBy(() -> ownerService.deleteCategory(USER_ID, CATEGORY_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("본인 가게의 카테고리만 접근 가능합니다.");

            verify(ownerMapper, never()).deleteCategory(anyLong());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    private static OwnerMenuRegReq newMenuRegReq(long menuId, long storeId, String name, int price) {
        OwnerMenuRegReq dto = new OwnerMenuRegReq();
        dto.setMenuId(menuId);
        dto.setStoreId(storeId);
        dto.setName(name);
        dto.setPrice(price);
        return dto;
    }

    private static OwnerMenuUpdateReq newMenuUpdateReq(long menuId, String name, int price) {
        OwnerMenuUpdateReq dto = new OwnerMenuUpdateReq();
        dto.setMenuId(menuId);
        dto.setName(name);
        dto.setPrice(price);
        return dto;
    }

    private static OwnerMenuRes newMenuRes(Long menuId, String name) {
        OwnerMenuRes res = new OwnerMenuRes();
        res.setMenuId(menuId);
        res.setName(name);
        return res;
    }

    private static OwnerOrderStateReq newStateReq(long orderId, int state) {
        OwnerOrderStateReq req = new OwnerOrderStateReq();
        req.setOrderId(orderId);
        req.setOrderState(state);
        return req;
    }

    private static OwnerOrderRes newOrder(long orderId, long userNo) {
        OwnerOrderRes o = new OwnerOrderRes();
        o.setOrderId(orderId);
        o.setUserNo(userNo);
        return o;
    }

    private static OwnerStoreRegReq newRegReq(long userId, long categoryId, String name) {
        OwnerStoreRegReq dto = new OwnerStoreRegReq();
        dto.setUserId(userId);
        dto.setCategoryId(categoryId);
        dto.setStoreName(name);
        return dto;
    }

    private static OwnerStoreUpdateReq newUpdateReq(String storeId, String name) {
        OwnerStoreUpdateReq dto = new OwnerStoreUpdateReq();
        dto.setStoreId(storeId);
        dto.setStoreName(name);
        return dto;
    }
}
