# ADR-006: 라이더 Redis 활용 (Phase 4-C 연결)

> **상태**: Accepted (2026-05-05)
> **관련 결정**: Q4-X (Pub/Sub 미도입), Q6-A (위치 KV)
> **관련 Figma**: 직접 매핑 없음 (Redis는 인프라 통합 결정 — 사용처 화면은 ADR-003 실시간 위치 / ADR-005 지도 화면 참조)
> **메모리**: `feedback_dead_config_avoidance.md` (NAJACKS 변종 회피 원칙)

---

## 상황 (Context)

Phase 4-C 시점 도입 인프라:
- `spring-boot-starter-data-redis` (Lettuce)
- docker-compose `redis:7-alpine` + healthcheck
- `RefreshTokenStore` 인터페이스 + `RedisRefreshTokenStore` (auth-service)

라이더 정리 시점 결정 필요:
- 위치 추적 (ADR-005에서 KV 채택 — Q6-A)
- Pub/Sub (배차/위치 broadcast) — ADR-003에서 미도입 결정 (Q4-X)
- Geospatial (가용 라이더 검색) — Phase 6+ 다중 라이더 매칭에 도입
- RefreshTokenStore 이관 (mmg-common) — 라이더가 별도 RT 다룰지 여부

라이더는 Q1-C로 auth-service에 가입/로그인 통합 → 라이더가 RT를 별도로 다룰 필요 없음. RefreshTokenStore 이관 불필요.

---

## 옵션 (Options)

### 위치 추적 (Q6) — ADR-005 별건 (참조만)

→ Redis KV `rider:loc:{riderNo}` TTL 30s

### Pub/Sub 도입 (Q4) — ADR-003 별건 (참조만)

→ MVP 미도입 (X 옵션)

### Geospatial 도입

- A. Phase 5 도입 — Phase 6+ 가용 라이더 매칭 알고리즘 도입 시점에 함께
- **B. 미도입 (MVP)** — MVP는 단일 라이더 매칭, 알고리즘 부재

→ **B 채택** — dead config 회피

### RefreshTokenStore 이관

- A. mmg-common 이관 — 라이더가 별도 RT 다룰 시 재사용
- **B. 그대로 auth-service 잔존** — Q1-C 결과로 라이더 별도 RT 불필요

→ **B 채택** — 메모리 tech-debt 항목 폐기

---

## 결정 (Decision)

### 라이더 도메인 Redis 사용 (4-C 연결)

| 인프라 | 사용 | 키 / 채널 | TTL | 도입 시점 |
|---|---|---|---|---|
| **KV (위치)** | ✅ | `rider:loc:{riderNo}` | 30s | Phase 5-R5 |
| Pub/Sub | ❌ MVP | (미도입) | — | Phase 6+ Y 옵션 (다중 Main 진입 시) |
| Geospatial | ❌ MVP | (미도입) | — | Phase 6+ 가용 라이더 매칭 알고리즘 도입 시 |
| RefreshTokenStore | ❌ (auth만 사용) | — | — | (이관 불필요, Q1-C 결과) |

### KV 도입 (Phase 5-R5)

```
의존성: spring-boot-starter-data-redis (rider-service build.gradle 추가)
설정: application.yml의 spring.data.redis (4-C와 동일 host/port)
인터페이스: RiderLocationStore (4-C RefreshTokenStore와 동일 패턴)
구현: RedisRiderLocationStore (Lettuce)
```

```java
// 의사코드 (Phase 5-R5에서 구현)
public interface RiderLocationStore {
    void save(long riderNo, RiderLocation loc);  // SET + TTL 30s
    Optional<RiderLocation> get(long riderNo);   // GET
    void delete(long riderNo);                    // DEL (업무 종료 시)
}
```

### 도입 회피 항목 + 근거

#### Pub/Sub 미도입

- 사용처가 MVP에 부재 (배차는 Feign sync, 위치 broadcast는 Main 단일 인스턴스의 simple broker)
- 메모리 `feedback_dead_config_avoidance.md` 원칙: 사용처 부재 시 인프라 도입 = NAJACKS 변종
- tech-debt 등재: "다중 Main 인스턴스 진입 시 위치 broadcast Pub/Sub Y 옵션 도입"

#### Geospatial 미도입

- MVP는 단일 라이더 매칭 (점주 주문 수락 → 가용 라이더 1명에 직접 push)
- 알고리즘이 없으면 GEORADIUS 호출처 부재 → dead config
- tech-debt 등재: "가용 라이더 매칭 알고리즘 도입 시 Geospatial 검토"

#### RefreshTokenStore 이관 불필요

- Q1-C 결과: 라이더 가입/로그인은 auth-service에서 처리 → RT 발급/저장 모두 auth 책임
- rider-service는 access token 검증만 (TokenAuthenticationFilter, JwtUser)
- 메모리 tech-debt 항목 "Phase 5 라이더/admin이 RT 다룰 필요 발생 시 mmg-common 이관" → **폐기**

---

## 결과 (Consequences)

### Phase 5-R5 추가 의존성 (rider-service)

```gradle
// mmg-rider-service/build.gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

### docker-compose 변경 0

- 4-C에서 `redis:7-alpine` 이미 정의 — 라이더가 그대로 사용
- Redis 인스턴스 1개로 auth(RT) + rider(위치) 공유
- 키 충돌 0 (`rt:{userNo}` vs `rider:loc:{riderNo}` namespace 다름)

### 통합 테스트 패턴 (4-C 그대로)

- `RedisRiderLocationStoreTest` — mock RedisTemplate + ArgumentCaptor (4-C `RedisRefreshTokenStoreTest` 패턴)
- 학원 환경 무영향 (mock 위주, embedded-redis/Testcontainers 회피)

### tech-debt 갱신

- 폐기: "RefreshTokenStore mmg-common 이관"
- 신규: "Pub/Sub Y 옵션 도입 (다중 Main 진입 시)"
- 신규: "Redis Geospatial 도입 (가용 라이더 매칭 알고리즘 도입 시)"
- 신규: "위치 사후 분석 / DB 영속화 (Phase 6+)"

---

## 트레이드오프

| 항목 | 채택 결과 | 미래 고려 사항 |
|---|---|---|
| KV만 도입 | 인프라 단순, 4-C 재사용 | 사후 분석/Geospatial 부재 — Phase 6+ |
| RefreshTokenStore 그대로 | 라이더 측 코드 단순 | 라이더 자체 인증 분리 시점에 이관 (Q1-C 변경 시) |
| Redis 단일 인스턴스 공유 | 운영 단순 | 라이더 위치 부하 + auth RT 부하 동시 — Phase 6+ 분리 검토 |
| 미도입 4건 (Pub/Sub/Geospatial/이관/사후 분석) | dead config 회피 | tech-debt 4건 등재, 트리거 명시 |

---

## 미해결 / Phase 5에서 결정

- 위치 데이터 직렬화 포맷 (JSON vs MessagePack) — Phase 5-R5
- Redis Lettuce 연결 풀 사이즈 — 학원 발표 환경 default 충분, 운영 시 별도

---

## 관련 메모리

- `feedback_dead_config_avoidance.md` — NAJACKS 변종 회피 원칙 (4-C 정착, 라이더에서 4건 적용)
- `project_phase4c_state.md` — Redis 인프라 도입 사례
- `feedback_w2_split_null_vs_exception.md` — 한 항목 = 한 개념 (tech-debt 분리)
