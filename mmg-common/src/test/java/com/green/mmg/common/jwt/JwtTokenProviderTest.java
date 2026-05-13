package com.green.mmg.common.jwt;

import com.green.mmg.common.constants.ConstJwt;
import com.green.mmg.common.model.JwtUser;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider 단위 테스트")
class JwtTokenProviderTest {

    // HS256 키 길이 32바이트 이상 — 실제 키와 무관한 테스트 전용
    private static final String TEST_SECRET_RAW = "test-secret-key-must-be-long-enough-for-hmac-sha256-please";
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(TEST_SECRET_RAW.getBytes());
    private static final long ACCESS_VALIDITY_MS = 60_000L;     // 1분
    private static final long REFRESH_VALIDITY_MS = 600_000L;   // 10분

    private ObjectMapper objectMapper;
    private ConstJwt constJwt;
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();
        constJwt = newConstJwt(TEST_SECRET, ACCESS_VALIDITY_MS, REFRESH_VALIDITY_MS);
        provider = new JwtTokenProvider(objectMapper, constJwt);
    }

    private static ConstJwt newConstJwt(String secret, long atValidity, long rtValidity) {
        return new ConstJwt(
                "test-issuer",
                "JWT",
                "signedUser",
                secret,
                "access-token", "/", 1296000, atValidity,
                "refresh-token", "/api/user/reissue", 1296000, rtValidity
        );
    }

    private static SecretKey testKey(String secret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    private Date parseExpiration(String token, String secret) {
        return Jwts.parser()
                .verifyWith(testKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
    }

    @Nested
    @DisplayName("토큰 발급")
    class Generate {

        @Test
        @DisplayName("AT 발급 → JWT 형식(헤더.페이로드.서명) + claims에 JwtUser 정보 포함")
        void generateAccessToken_containsClaims() {
            JwtUser user = new JwtUser(42L, "CUSTOMER", null, "준하");

            String token = provider.generateAccessToken(user);

            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3);  // header.payload.signature

            JwtUser parsed = provider.getJwtUserFromToken(token);
            assertThat(parsed.getSignedUserNo()).isEqualTo(42L);
            assertThat(parsed.getRole()).isEqualTo("CUSTOMER");
            assertThat(parsed.getName()).isEqualTo("준하");
        }

        @Test
        @DisplayName("AT 만료시각 = 발급 시점 + ACCESS_VALIDITY_MS (오차 5초 이내)")
        void generateAccessToken_expirationMatchesValidity() {
            JwtUser user = new JwtUser(1L, "CUSTOMER", null, "kim");
            long beforeIssue = System.currentTimeMillis();

            String token = provider.generateAccessToken(user);

            long actualExp = parseExpiration(token, TEST_SECRET).getTime();
            long expectedExp = beforeIssue + ACCESS_VALIDITY_MS;
            assertThat(actualExp).isBetween(expectedExp - 5_000L, expectedExp + 5_000L);
        }

        @Test
        @DisplayName("AT vs RT — RT가 AT보다 오래 살아남 (만료시각 차이 = REFRESH-ACCESS validity)")
        void generateAccessAndRefresh_haveDifferentExpiration() {
            JwtUser user = new JwtUser(1L, "CUSTOMER", null, "kim");

            String at = provider.generateAccessToken(user);
            String rt = provider.generateRefreshToken(user);

            long atExp = parseExpiration(at, TEST_SECRET).getTime();
            long rtExp = parseExpiration(rt, TEST_SECRET).getTime();

            // 두 토큰 발급 사이 ms 단위 시차 허용. 차이는 RT-AT validity 차이(약 9분)에 근접해야 함.
            long gap = rtExp - atExp;
            long expectedGap = REFRESH_VALIDITY_MS - ACCESS_VALIDITY_MS;
            assertThat(gap).isBetween(expectedGap - 5_000L, expectedGap + 5_000L);
        }
    }

    @Nested
    @DisplayName("토큰 검증")
    class Verify {

        @Test
        @DisplayName("정상 토큰 → JwtUser 역직렬화 성공 (signedUserNo/role/name 일치)")
        void validToken_parsedSuccessfully() {
            JwtUser user = new JwtUser(7L, "OWNER", null, "owner-name");
            String token = provider.generateAccessToken(user);

            JwtUser parsed = provider.getJwtUserFromToken(token);

            assertThat(parsed.getSignedUserNo()).isEqualTo(7L);
            assertThat(parsed.getRole()).isEqualTo("OWNER");
            assertThat(parsed.getName()).isEqualTo("owner-name");
        }

        @Test
        @DisplayName("만료된 토큰 → ExpiredJwtException")
        void expiredToken_throwsExpiredJwtException() {
            // 음수 만료시간으로 즉시 만료된 토큰을 같은 시크릿으로 발급
            ConstJwt expiredConst = newConstJwt(TEST_SECRET, -10_000L, -10_000L);
            JwtTokenProvider expiredProvider = new JwtTokenProvider(objectMapper, expiredConst);
            String expired = expiredProvider.generateAccessToken(new JwtUser(1L, "CUSTOMER", null, "x"));

            assertThatThrownBy(() -> provider.getJwtUserFromToken(expired))
                    .isInstanceOf(ExpiredJwtException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("다른 시크릿으로 서명된 토큰 → JwtException 하위(SignatureException)")
        void wrongSignature_throwsJwtException() {
            String otherSecret = Base64.getEncoder().encodeToString(
                    "completely-different-secret-key-must-also-be-32-bytes!".getBytes());
            ConstJwt otherConst = newConstJwt(otherSecret, ACCESS_VALIDITY_MS, REFRESH_VALIDITY_MS);
            JwtTokenProvider otherProvider = new JwtTokenProvider(objectMapper, otherConst);
            String fakeToken = otherProvider.generateAccessToken(new JwtUser(1L, "CUSTOMER", null, "x"));

            assertThatThrownBy(() -> provider.getJwtUserFromToken(fakeToken))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("형식이 깨진 토큰 → JwtException")
        void malformedToken_throwsJwtException() {
            assertThatThrownBy(() -> provider.getJwtUserFromToken("not.a.valid.jwt.token"))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("빈 문자열 토큰 → JwtException 또는 IllegalArgumentException")
        void emptyToken_throws() {
            assertThatThrownBy(() -> provider.getJwtUserFromToken(""))
                    .isInstanceOfAny(JwtException.class, IllegalArgumentException.class);
        }
    }
}
