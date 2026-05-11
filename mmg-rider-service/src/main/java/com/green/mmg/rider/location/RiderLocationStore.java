package com.green.mmg.rider.location;

import com.green.mmg.rider.location.model.RiderLocation;

import java.util.Optional;

/**
 * Phase 5-R5: 라이더 위치 KV 저장소 — ADR-005/006 박제 일관.
 *
 * <p>4-C {@code RefreshTokenStore} 패턴 일관 — interface + Redis 구현 분리.
 * 키: {@code rider:loc:{riderNo}}, TTL 30s (ADR-006 박제).</p>
 *
 * <p>D1 throw — Redis 다운 시 RuntimeException 그대로 전파 → mmg-common GlobalExceptionHandler가
 * 5xx로 매핑 (4-C `RedisRefreshTokenStore` 패턴 일관, best-effort 회피).</p>
 */
public interface RiderLocationStore {

    /** 위치 저장 — TTL 30s 동결. 같은 키 호출 시 덮어쓰기 (단일 라이더 단일 위치). */
    void save(long riderNo, RiderLocation location);

    /** 위치 조회 — 부재(NULL/만료) 시 Optional.empty(). */
    Optional<RiderLocation> get(long riderNo);

    /** 위치 삭제 — 업무 종료 시 호출 (R8 work_session 종료 시점). */
    void delete(long riderNo);
}
