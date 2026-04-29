package com.green.mmg.main.store;

import com.green.mmg.main.support.SnapshotAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
 * Phase 3-B-2 검증: LikedStore 도메인 JPA 전환 전후 응답 동결 확인.
 *
 * <p>흐름:
 * <ol>
 *   <li>MyBatis 상태에서 첫 실행 → snapshot baseline 자동 캡처</li>
 *   <li>JPA 전환 (LikedStore @Entity + @IdClass + Repository, 서비스 리팩토링)</li>
 *   <li>동일 테스트 재실행 → snapshot STRICT 비교 → diff = 0 확인</li>
 * </ol>
 *
 * <p>학원 공유 DB 사용 + @Transactional + @Rollback으로 자동 롤백 (INSERT/DELETE 영향 0).
 * favoriteList JOIN+LIMIT은 MyBatis 잔존 (Phase 3-B 하이브리드 검증 대상).</p>
 */
@SpringBootTest
@Transactional
@Rollback
class LikedStoreControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;

    /** likedstore 미존재 보장 (max user_no=15, FK 없음). storeId는 실재 row(21). */
    private static final long TEST_USER_NO  = 99999L;
    private static final long TEST_STORE_ID = 21L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("checkWish — 찜 안 한 상태 → resultData=false")
    void checkWish_notFavorite() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/store/favorite/check")
                        .param("userNo",  String.valueOf(TEST_USER_NO))
                        .param("storeId", String.valueOf(TEST_STORE_ID)))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        SnapshotAssert.assertMatches("liked-check-not-favorite",
                result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("wishToggle — 첫 호출 INSERT → resultData=true")
    void wishToggle_insert() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userNo", TEST_USER_NO);
        body.put("storeId", TEST_STORE_ID);

        MvcResult result = mockMvc.perform(post("/api/store/favorite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        SnapshotAssert.assertMatches("liked-toggle-insert",
                result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("wishToggle 2회 (insert→delete) — 두 번째 호출 → resultData=false")
    void wishToggle_insertThenDelete() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userNo", TEST_USER_NO);
        body.put("storeId", TEST_STORE_ID);
        String json = objectMapper.writeValueAsString(body);

        // 1st (insert)
        mockMvc.perform(post("/api/store/favorite")
                .contentType(MediaType.APPLICATION_JSON).content(json));

        // 2nd (delete) — 응답 캡처
        MvcResult result = mockMvc.perform(post("/api/store/favorite")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        SnapshotAssert.assertMatches("liked-toggle-delete",
                result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("wishListGet — INSERT 후 favoriteList(MyBatis JOIN+LIMIT) 1개 응답")
    void wishListGet_afterInsert() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userNo", TEST_USER_NO);
        body.put("storeId", TEST_STORE_ID);
        mockMvc.perform(post("/api/store/favorite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));

        MvcResult result = mockMvc.perform(get("/api/store/favorite")
                        .param("userNo", String.valueOf(TEST_USER_NO))
                        .param("currentPage", "1")
                        .param("size", "10"))
                .andReturn();

        String body0 = result.getResponse().getContentAsString();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        // 동적 데이터(store_name 등)는 snapshot 부적합 — 구조만 부분 assertion
        assertThat(body0).contains("\"totalCount\":1");
        assertThat(body0).contains("\"id\":" + TEST_STORE_ID);
    }
}
