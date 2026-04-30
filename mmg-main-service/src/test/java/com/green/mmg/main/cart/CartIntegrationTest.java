package com.green.mmg.main.cart;

import com.green.mmg.common.exception.BusinessException;
import com.green.mmg.main.cart.model.Cart;
import com.green.mmg.main.cart.model.CartDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 2-Backfill-D Step D-3 + Warning 1 해소: CartService UPDATE 회로 + 권한 분기 통합 테스트.
 *
 * <p>Mockito 단위 테스트로 검증 못한 부분 보완:
 * <ul>
 *   <li>JPA 영속성 컨텍스트에서 dirty checking 실제 UPDATE 발행되는지 동결 (updateCartItem)</li>
 *   <li>권한 분기 통과 후 DB row가 실제 변경되는지 동결</li>
 *   <li>403 시나리오에서 DB row가 미변경 유지되는지 동결 (롤백 안전성)</li>
 * </ul>
 *
 * <p>학원 공유 DB + @Transactional + @Rollback (INSERT 영향 0).
 * fixture row 사전 INSERT 패턴 — UserAddressIntegrationTest와 동일.</p>
 */
@SpringBootTest
@Transactional
@Rollback
class CartIntegrationTest {

    @Autowired private CartService cartService;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartDetailRepository cartDetailRepository;

    private static final long OWNER_USER_NO = 99999L;
    private static final long OTHER_USER_NO = 11111L;
    private static final long STORE_ID_21 = 21L;
    private static final long MENU_ID = 17L;

    private Long fixtureCartId;
    private Long fixtureCartItemId;

    @BeforeEach
    void seedFixture() {
        Cart cart = new Cart();
        cart.setUserNo(OWNER_USER_NO);
        cart.setStoreId(STORE_ID_21);
        cartRepository.saveAndFlush(cart);
        fixtureCartId = cart.getCartId();

        CartDetail item = new CartDetail();
        item.setCartId(fixtureCartId);
        item.setMenuId(MENU_ID);
        item.setQuantity(2);
        cartDetailRepository.saveAndFlush(item);
        fixtureCartItemId = item.getCartItemId();
    }

    @Test
    @DisplayName("updateCartItem 본인 → JPA dirty checking으로 DB quantity 실제 갱신 (Warning 1 해소)")
    void update_byOwner_actuallyPersistsToDb() {
        cartService.updateCartItem(OWNER_USER_NO, fixtureCartItemId, 7);

        // dirty checking 결과를 DB로 flush (영속성 컨텍스트가 같은 트랜잭션 내라 commit 전이지만 flush로 SQL 발행 검증)
        cartDetailRepository.flush();

        // 영속성 컨텍스트 비우고 재조회 — JPA 1차 캐시 무효화로 실제 DB row 검증
        cartDetailRepository.findById(fixtureCartItemId).ifPresent(persisted ->
                assertThat(persisted.getQuantity())
                        .as("dirty checking으로 quantity 7로 갱신")
                        .isEqualTo(7));
    }

    @Test
    @DisplayName("updateCartItem 다른 사용자 → 403 + DB quantity 미변경 (롤백 안전성 동결)")
    void update_byOtherUser_throwsForbiddenAndDbUnchanged() {
        assertThatThrownBy(() -> cartService.updateCartItem(OTHER_USER_NO, fixtureCartItemId, 99))
                .isInstanceOf(BusinessException.class)
                .hasMessage("본인 장바구니 아이템만 접근 가능합니다.");

        cartDetailRepository.flush();
        CartDetail persisted = cartDetailRepository.findById(fixtureCartItemId).orElseThrow();
        assertThat(persisted.getQuantity())
                .as("403 발생 시 quantity는 fixture 초기값(2) 유지")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("deleteCartItem 다른 사용자 → 403 + DB row 미삭제 (롤백 안전성 동결)")
    void delete_byOtherUser_throwsForbiddenAndDbUnchanged() {
        assertThatThrownBy(() -> cartService.deleteCartItem(OTHER_USER_NO, fixtureCartItemId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("본인 장바구니 아이템만 접근 가능합니다.");

        cartDetailRepository.flush();
        assertThat(cartDetailRepository.findById(fixtureCartItemId))
                .as("403 발생 시 cartItem row는 삭제되지 않고 그대로 존재")
                .isPresent();
    }

    @Test
    @DisplayName("deleteCartItem 본인 + 마지막 아이템 → cart row까지 삭제 (회로 동결)")
    void delete_byOwnerLastItem_alsoDeletesCart() {
        cartService.deleteCartItem(OWNER_USER_NO, fixtureCartItemId);
        cartDetailRepository.flush();
        cartRepository.flush();

        assertThat(cartDetailRepository.findById(fixtureCartItemId))
                .as("마지막 아이템 삭제 후 cartItem row 부재")
                .isEmpty();
        assertThat(cartRepository.findById(fixtureCartId))
                .as("countByCartId==0 → cart row도 삭제")
                .isEmpty();
    }
}
