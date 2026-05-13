package com.green.mmg.auth.internal;

import com.green.mmg.auth.user.UserRepository;
import com.green.mmg.auth.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4-A 백필: InternalUserController provider 통합 테스트.
 *
 * <p>Feign consumer(StoreService/OrderService/OwnerService)가 의존하는 직렬화 형식을
 * 실제 HTTP 라운드트립으로 동결. 본 테스트가 통과해야 라이더(Phase 5) 진입 시
 * Internal API 응답 스펙 회귀 즉시 검출 가능.</p>
 *
 * <p>학원 공유 DB — fixture INSERT + @Rollback으로 변경 0 보장.
 * Security: AuthSecurityConfig에서 /internal/** permitAll → principal 주입 불필요.</p>
 */
@SpringBootTest
@Transactional
@Rollback
class InternalUserControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private User seedUser(String name, String tel) {
        User u = new User();
        // user_id length 20 제약 → UUID 8자만 사용. 학원 DB unique 충돌 위험 사실상 0.
        u.setUserId("it_" + UUID.randomUUID().toString().substring(0, 8));
        u.setUserPw("BCRYPT_PLACEHOLDER_FOR_FIXTURE");
        u.setRole("CUSTOMER");
        u.setName(name);
        u.setBirth("1990-01-01");
        u.setGender(1);
        u.setGreen(0);
        u.setKind(0);
        u.setRank("BRONZE");
        u.setTel(tel);
        return userRepository.saveAndFlush(u);
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /internal/auth/user/{userNo}
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUser happy: existing userNo → 200 + UserBriefDto JSON 형식 동결 (userNo/name/tel/address=\"\")")
    void getUser_happy_serializationFrozen() throws Exception {
        User saved = seedUser("준하", "010-1111-2222");

        mockMvc.perform(get("/internal/auth/user/" + saved.getUserNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userNo").value(saved.getUserNo()))
                .andExpect(jsonPath("$.name").value("준하"))
                .andExpect(jsonPath("$.tel").value("010-1111-2222"))
                .andExpect(jsonPath("$.address").value(""));
    }

    @Test
    @DisplayName("getUser not-found: 미존재 userNo → 404 + 메시지 'user not found: {id}' (consumer FeignException.NotFound 변환)")
    void getUser_notFound() throws Exception {
        long nonexistent = 999_999_999L;

        mockMvc.perform(get("/internal/auth/user/" + nonexistent))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultMessage").value("user not found: " + nonexistent));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /internal/auth/owner/{userNo}
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOwner happy: existing userNo → 200 + UserBriefDto JSON (getUser와 동일 구현, endpoint 분리 동결)")
    void getOwner_happy() throws Exception {
        User saved = seedUser("사장님", "010-3333-4444");

        mockMvc.perform(get("/internal/auth/owner/" + saved.getUserNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userNo").value(saved.getUserNo()))
                .andExpect(jsonPath("$.name").value("사장님"))
                .andExpect(jsonPath("$.tel").value("010-3333-4444"))
                .andExpect(jsonPath("$.address").value(""));
    }

    @Test
    @DisplayName("getOwner not-found: 미존재 → 404 + 메시지 'owner not found: {id}' (user 메시지와 분리 동결)")
    void getOwner_notFound() throws Exception {
        long nonexistent = 999_999_998L;

        mockMvc.perform(get("/internal/auth/owner/" + nonexistent))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultMessage").value("owner not found: " + nonexistent));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /internal/auth/users?ids=
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUsers batch happy: 2명 INSERT → ?ids=a&ids=b → size 2 + userNo별 정확한 매핑 + address=\"\" 동결")
    void getUsers_batchHappy() throws Exception {
        User u1 = seedUser("준하", "010-1111-1111");
        User u2 = seedUser("민수", "010-2222-2222");

        mockMvc.perform(get("/internal/auth/users")
                        .param("ids", String.valueOf(u1.getUserNo()))
                        .param("ids", String.valueOf(u2.getUserNo())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // 순서 비보장 — userNo별 매핑 검증 (JsonPath filter)
                .andExpect(jsonPath("$[?(@.userNo == " + u1.getUserNo() + ")].name").value("준하"))
                .andExpect(jsonPath("$[?(@.userNo == " + u1.getUserNo() + ")].tel").value("010-1111-1111"))
                .andExpect(jsonPath("$[?(@.userNo == " + u2.getUserNo() + ")].name").value("민수"))
                .andExpect(jsonPath("$[?(@.userNo == " + u2.getUserNo() + ")].tel").value("010-2222-2222"))
                // address는 모두 빈 문자열 (consumer가 자체 채움)
                .andExpect(jsonPath("$[?(@.userNo == " + u1.getUserNo() + ")].address").value(""))
                .andExpect(jsonPath("$[?(@.userNo == " + u2.getUserNo() + ")].address").value(""));
    }

    @Test
    @DisplayName("getUsers batch partial: 존재 1명 + 미존재 1명 → 존재만 반환 (size 1, 누락된 userNo는 응답에 없음 — consumer가 nameMap.getOrDefault로 fallback)")
    void getUsers_batchPartial() throws Exception {
        User u1 = seedUser("준하", "010-1111-1111");
        long nonexistent = 999_999_997L;

        mockMvc.perform(get("/internal/auth/users")
                        .param("ids", String.valueOf(u1.getUserNo()))
                        .param("ids", String.valueOf(nonexistent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userNo").value(u1.getUserNo()))
                .andExpect(jsonPath("$[0].name").value("준하"));
    }
}
