package com.green.mmg.main.owner;

import com.green.mmg.common.dto.feign.UserBriefDto;
import com.green.mmg.common.feign.AuthFeignClient;
import com.green.mmg.main.owner.model.OwnerOrderRes;
import com.green.mmg.main.owner.model.OwnerOrderStateReq;
import com.green.mmg.main.owner.model.OwnerStoreRegReq;
import com.green.mmg.main.owner.model.OwnerStoreUpdateReq;
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
 * Phase 2-Backfill-D Step D-1-B: OwnerService 핵심 9개 메서드 단위 테스트 — 현재 동작 동결.
 *
 * <p><b>권한 분기 부재를 명시적으로 동결한다.</b><br>
 * OwnerService 14개 메서드 중 {@code getMyStore/getMyStores}만 ownerNo 파라미터로 본인 가게 필터.
 * 나머지 메서드(registerStore/updateStore/deleteStore/getOrders/updateOrderState/deleteOrder/
 * registerMenu/updateMenu/deleteMenu/매출/카테고리)는 store_id/order_id/menu_id/dto.userId만 받고
 * 점주 본인 소유 검증을 하지 않는다 — 다른 점주의 가게/메뉴/주문 변경 가능.
 * 이는 알려진 보안 부채이며 <b>Phase 2-Backfill-D-bis에서 14개 메서드 권한 분기를 일괄 추가할 예정</b>.<br>
 * 본 테스트는 현재 동작을 회귀 방지를 위해 그대로 동결한다 (D-bis에서 함께 갱신).</p>
 *
 * <p>학원 DB / Spring 컨텍스트 의존 0 — 순수 Mockito.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerService — 핵심 9개 메서드 단위 테스트 (현재 동작 동결)")
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
    @DisplayName("registerStore — 가게 등록 + storeCategory + defaultMenuCategory 후속 호출")
    class RegisterStore {

        @Test
        @DisplayName("happy: result>0 → registerStoreCategory + registerDefaultMenuCategory 순차 호출 (InOrder 동결)")
        void happyPath_registersAndChainsCategories() {
            OwnerStoreRegReq dto = newRegReq(USER_ID, CATEGORY_ID, "맛집");
            when(ownerMapper.registerStore(dto)).thenReturn(1);

            ownerService.registerStore(dto);

            InOrder inOrder = inOrder(ownerMapper);
            inOrder.verify(ownerMapper).registerStore(dto);
            inOrder.verify(ownerMapper).registerStoreCategory(USER_ID, CATEGORY_ID);
            inOrder.verify(ownerMapper).registerDefaultMenuCategory(USER_ID);
        }

        @Test
        @DisplayName("실패: result==0 → RuntimeException '가게 등록 실패' + 후속 호출 미발생")
        void registerFails_throwsAndShortCircuits() {
            OwnerStoreRegReq dto = newRegReq(USER_ID, CATEGORY_ID, "실패");
            when(ownerMapper.registerStore(dto)).thenReturn(0);

            assertThatThrownBy(() -> ownerService.registerStore(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("가게 등록 실패");

            verify(ownerMapper, never()).registerStoreCategory(anyLong(), anyLong());
            verify(ownerMapper, never()).registerDefaultMenuCategory(anyLong());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateStore — 가게 기본 정보 수정")
    class UpdateStore {

        @Test
        @DisplayName("happy: result>0 → 정상 종료")
        void happyPath_updates() {
            OwnerStoreUpdateReq dto = newUpdateReq("21", "변경");
            when(ownerMapper.updateStore(dto)).thenReturn(1);

            ownerService.updateStore(dto);

            verify(ownerMapper).updateStore(dto);
            verifyNoMoreInteractions(ownerMapper);
        }

        @Test
        @DisplayName("실패: result==0 → RuntimeException '가게 정보 수정 실패' (메시지 동결)")
        void updateFails_throws() {
            OwnerStoreUpdateReq dto = newUpdateReq("21", "변경");
            when(ownerMapper.updateStore(dto)).thenReturn(0);

            assertThatThrownBy(() -> ownerService.updateStore(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("가게 정보 수정 실패")
                    .hasMessageContaining("해당 가게를 찾을 수 없음");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteStore — 가게 삭제")
    class DeleteStore {

        @Test
        @DisplayName("happy: result>0 → 정상 종료")
        void happyPath_deletes() {
            when(ownerMapper.deleteStore(STORE_ID)).thenReturn(1);

            ownerService.deleteStore(STORE_ID);

            verify(ownerMapper).deleteStore(STORE_ID);
        }

        @Test
        @DisplayName("실패: result==0 → RuntimeException '삭제할 가게를 찾을 수 없습니다.' (메시지 동결)")
        void deleteFails_throws() {
            when(ownerMapper.deleteStore(STORE_ID)).thenReturn(0);

            assertThatThrownBy(() -> ownerService.deleteStore(STORE_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("삭제할 가게를 찾을 수 없습니다");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getOrders — Feign batch + 응답 합성 (N+1 회피)")
    class GetOrders {

        @Test
        @DisplayName("happy: 중복 userNo 제거(distinct) batch 호출 + customerName/tel 합성")
        void happyPath_batchedFeignAndAssembled() {
            OwnerOrderRes o1 = newOrder(391_000_001L, 100L);
            OwnerOrderRes o2 = newOrder(391_000_002L, 100L);  // 중복 user
            OwnerOrderRes o3 = newOrder(391_000_003L, 200L);
            when(ownerMapper.getOrders(STORE_ID, 1, "2026-04-30"))
                    .thenReturn(List.of(o1, o2, o3));
            when(authFeignClient.getUsers(anyList())).thenReturn(List.of(
                    new UserBriefDto(100L, "준하", "010-1111", ""),
                    new UserBriefDto(200L, "민수", "010-2222", "")
            ));

            List<OwnerOrderRes> result = ownerService.getOrders(STORE_ID, 1, "2026-04-30");

            // distinct userNos가 batch로 전달됐는지 동결
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
            verify(authFeignClient).getUsers(captor.capture());
            assertThat(captor.getValue()).containsExactlyInAnyOrder(100L, 200L);  // 중복 제거

            // 합성된 customerName/tel 동결
            assertThat(result.get(0).getCustomerName()).isEqualTo("준하");
            assertThat(result.get(0).getTel()).isEqualTo("010-1111");
            assertThat(result.get(1).getCustomerName()).isEqualTo("준하");  // 같은 user 합성
            assertThat(result.get(2).getCustomerName()).isEqualTo("민수");
            assertThat(result.get(2).getTel()).isEqualTo("010-2222");
        }

        @Test
        @DisplayName("orders 빈 리스트 → Feign 미호출 (early return 동결)")
        void emptyOrders_skipsFeign() {
            when(ownerMapper.getOrders(STORE_ID, null, null)).thenReturn(List.of());

            List<OwnerOrderRes> result = ownerService.getOrders(STORE_ID, null, null);

            assertThat(result).isEmpty();
            verifyNoInteractions(authFeignClient);
        }

        @Test
        @DisplayName("Feign 응답에 없는 userNo → if (u != null) 분기 → customerName/tel 미설정 (현재 동작 동결)")
        void userMissingFromFeign_keepsNullFields() {
            OwnerOrderRes o1 = newOrder(391_000_001L, 100L);
            OwnerOrderRes o2 = newOrder(391_000_002L, 999L);  // Feign 응답에 없음
            when(ownerMapper.getOrders(STORE_ID, null, null)).thenReturn(List.of(o1, o2));
            // batch 응답에 user 100만 포함, 999는 누락
            when(authFeignClient.getUsers(anyList())).thenReturn(List.of(
                    new UserBriefDto(100L, "준하", "010-1111", "")
            ));

            List<OwnerOrderRes> result = ownerService.getOrders(STORE_ID, null, null);

            assertThat(result.get(0).getCustomerName()).isEqualTo("준하");
            assertThat(result.get(0).getTel()).isEqualTo("010-1111");
            assertThat(result.get(1).getCustomerName()).isNull();  // 미설정 동결
            assertThat(result.get(1).getTel()).isNull();
        }

        @Test
        @DisplayName("Feign 예외 → 호출자에게 그대로 propagate (try-catch 없음 동결)")
        void feignException_propagates() {
            OwnerOrderRes o1 = newOrder(391_000_001L, 100L);
            when(ownerMapper.getOrders(STORE_ID, null, null)).thenReturn(List.of(o1));
            when(authFeignClient.getUsers(anyList())).thenThrow(
                    new FeignException.ServiceUnavailable(
                            "auth down",
                            Request.create(Request.HttpMethod.GET, "/internal/auth/users",
                                    new HashMap<>(), null, StandardCharsets.UTF_8, null),
                            null, null));

            assertThatThrownBy(() -> ownerService.getOrders(STORE_ID, null, null))
                    .isInstanceOf(FeignException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateOrderState — 주문 상태 변경")
    class UpdateOrderState {

        @Test
        @DisplayName("happy: result>0 → 정상 종료")
        void happyPath_updates() {
            OwnerOrderStateReq req = newStateReq(ORDER_ID, 3);
            when(ownerMapper.updateOrderState(req)).thenReturn(1);

            ownerService.updateOrderState(req);

            verify(ownerMapper).updateOrderState(req);
        }

        @Test
        @DisplayName("실패: result==0 → RuntimeException '주문 상태 변경 실패: 주문을 찾을 수 없습니다.'")
        void updateFails_throws() {
            OwnerOrderStateReq req = newStateReq(ORDER_ID, 3);
            when(ownerMapper.updateOrderState(req)).thenReturn(0);

            assertThatThrownBy(() -> ownerService.updateOrderState(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("주문 상태 변경 실패")
                    .hasMessageContaining("주문을 찾을 수 없습니다");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteOrder — @Transactional, OrderDetail → Order 순차 삭제")
    class DeleteOrder {

        @Test
        @DisplayName("호출 순서 동결: deleteOrderDetail → deleteOrder (InOrder)")
        void deletesInOrder() {
            ownerService.deleteOrder(ORDER_ID);

            InOrder inOrder = inOrder(ownerMapper);
            inOrder.verify(ownerMapper).deleteOrderDetail(ORDER_ID);
            inOrder.verify(ownerMapper).deleteOrder(ORDER_ID);
            verifyNoMoreInteractions(ownerMapper);
        }
    }

    // ─────────────────────────────────────────────────────────────────
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
