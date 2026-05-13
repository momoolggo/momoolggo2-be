package com.green.mmg.main.cart;

import com.green.mmg.common.model.JwtUser;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.main.support.SnapshotAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Phase 3-B-3 검증: Cart 도메인 JPA 전환 + 하이브리드 영구 공존 핵심 시나리오.
 * Phase 2-Backfill-D Step D-3: principal 주입 추가 (Controller가 @AuthenticationPrincipal 사용).
 *
 * <p>SecurityContextHolder에 principal 직접 주입 — webAppContextSetup이 Filter chain
 * 자동 적용 안 함을 이용. {@code @AfterEach}에서 컨텍스트 정리.</p>
 */
@SpringBootTest
@Transactional
@Rollback
class CartControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;

    /** cart 미존재 보장 (FK 없음) */
    private static final long TEST_USER_NO = 99999L;

    /** 사전 조회: store 21에 속한 메뉴, 다른 store(22)에 속한 메뉴 */
    private static final long MENU_STORE_21 = 17L;       // 숨은집 피자 (store 21)
    private static final long MENU_STORE_21_OTHER = 18L; // 갈릭 미트 피자 (store 21)
    private static final long MENU_STORE_22 = 23L;       // 다른 매장

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Controller가 @AuthenticationPrincipal UserPrincipal 받으므로 SecurityContextHolder에 직접 주입
        UserPrincipal principal = new UserPrincipal(
                new JwtUser(TEST_USER_NO, "CUSTOMER", "ACTIVE", "테스트사용자"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCart — 카트 없음 → resultData={} (빈 Map)")
    void getCart_emptyCart() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/cart/" + TEST_USER_NO)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        SnapshotAssert.assertMatches("cart-empty", result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("addToCart 신규 — JPA save(Cart) + save(CartDetail) + MyBatis findStoreIdByMenuId 하이브리드")
    void addToCart_newCart() throws Exception {
        Map<String, Object> body = addReq(TEST_USER_NO, MENU_STORE_21, 2);
        MvcResult result = mockMvc.perform(post("/api/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        SnapshotAssert.assertMatches("cart-add-success", result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("addToCart 같은 메뉴 재추가 — dirty checking 수량 합산")
    void addToCart_sameMenuQuantityIncrement() throws Exception {
        // 1차: 신규 추가
        mockMvc.perform(post("/api/cart/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq(TEST_USER_NO, MENU_STORE_21, 2))));

        // 2차: 같은 메뉴 +3 → quantity=5 (dirty checking)
        mockMvc.perform(post("/api/cart/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq(TEST_USER_NO, MENU_STORE_21, 3))));

        // 검증: getCart로 조회 → JPA findByUserNo + MyBatis findCartItems 합성, quantity=5
        String body = mockMvc.perform(get("/api/cart/" + TEST_USER_NO))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("\"quantity\":5");
        assertThat(body).contains("\"menuId\":" + MENU_STORE_21);
    }

    @Test
    @DisplayName("addToCart 다른 매장 — 409 differentStore")
    void addToCart_differentStore() throws Exception {
        // 1차: store 21 메뉴 추가
        mockMvc.perform(post("/api/cart/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq(TEST_USER_NO, MENU_STORE_21, 1))));

        // 2차: store 22 메뉴 추가 → 409
        MvcResult result = mockMvc.perform(post("/api/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq(TEST_USER_NO, MENU_STORE_22, 1))))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        SnapshotAssert.assertMatches("cart-add-different-store", result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("getCart 후 — JPA findByUserNo + MyBatis findCartItems(JOIN) 합성 응답")
    void getCart_afterAdd() throws Exception {
        // 두 메뉴 추가
        mockMvc.perform(post("/api/cart/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq(TEST_USER_NO, MENU_STORE_21, 2))));
        mockMvc.perform(post("/api/cart/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq(TEST_USER_NO, MENU_STORE_21_OTHER, 1))));

        String body = mockMvc.perform(get("/api/cart/" + TEST_USER_NO))
                .andReturn().getResponse().getContentAsString();

        // 동적 데이터 (cartId, storeName 등)는 snapshot 부적합 — 구조 부분 assertion
        assertThat(body).contains("\"storeId\":21");
        assertThat(body).contains("\"menuId\":" + MENU_STORE_21);
        assertThat(body).contains("\"menuId\":" + MENU_STORE_21_OTHER);
        assertThat(body).contains("\"quantity\":2");
        assertThat(body).contains("\"quantity\":1");
    }

    private static Map<String, Object> addReq(long userNo, long menuId, int quantity) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userNo", userNo);
        body.put("menuId", menuId);
        body.put("quantity", quantity);
        return body;
    }
}
