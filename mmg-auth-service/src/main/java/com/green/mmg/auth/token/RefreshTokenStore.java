package com.green.mmg.auth.token;

import java.time.Duration;
import java.util.Optional;

/**
 * Phase 4-C: RT 저장소 — RT revoke 가능성 보장.
 *
 * <p>현재 동작 변경(Phase 1):
 * - signup/signin 시 발급한 RT를 쿠키에 + 저장소에 저장
 * - reissue 시 쿠키 RT == 저장소 RT 비교 (불일치 시 401, 재로그인 강제)
 * - signout 시 저장소 키 삭제 (logout = 진짜 logout)</p>
 *
 * <p>구현: {@link RedisRefreshTokenStore} (Spring Data Redis Lettuce).
 * Phase 5에서 라이더/admin도 RT 다룰 시점에 mmg-common으로 이관 검토.</p>
 */
public interface RefreshTokenStore {

    /** RT 저장 — 키 충돌 시 덮어쓰기 (단일 디바이스 가정). TTL은 RT 만료시각과 동기. */
    void save(long userNo, String refreshToken, Duration ttl);

    /** RT 조회 — 부재 시 Optional.empty(). reissue 시 쿠키 RT와 비교용. */
    Optional<String> get(long userNo);

    /** RT 삭제 — signout / 위조 의심 시 호출. */
    void delete(long userNo);
}
