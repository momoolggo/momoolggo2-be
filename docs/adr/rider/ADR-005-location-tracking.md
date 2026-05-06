# ADR-005: 라이더 위치 추적

> **상태**: Accepted (2026-05-05)
> **관련 결정**: Q6 (저장 방식), D6 (송신 빈도), ADR-003 (WebSocket+STOMP)
> **관련 Figma**: `../../figma/` 지도 + 라이더 위치 화면 (실시간 추적)

---

## 상황 (Context)

CLAUDE.md §7 라이더: "실시간 위치는 Redis 또는 별도 테이블에 저장 (선택). SSE로 사용자/사장에게 위치 푸시"
- ADR-003에서 SSE 대신 WebSocket+STOMP 채택
- Phase 4-C에서 spring-data-redis 의존성 + RefreshTokenStore 패턴 검증

api-spec 명세:
- `PUT /api/rider/location` (라이더 → rider-service, REQ-RDR-005)
- `GET /internal/rider/{riderNo}/location` (Main → Rider, REQ-INT-004)
- 손님 화면: `GET /api/order/{orderId}/track` SSE (api-spec §2.6.7) — ADR-003에서 STOMP 토픽 `/topic/order/{orderId}/location`로 변경

---

## 옵션 (Options)

### 저장 방식 (Q6)

| 옵션 | 저장 | 영속화 | 장점 | 단점 |
|---|---|---|---|---|
| **A. Redis KV `rider:loc:{riderNo}`** | 1 key/rider, TTL 30s | X (휘발) | 4-C 인프라 재사용, 단순 | 사후 분석 불가 |
| B. Redis Geospatial `GEOADD` | 1 SortedSet | X | "근처 라이더" GEORADIUS | MVP 과설계 |
| C. DB rider_location 테이블 | UPSERT 1 row/rider | O | 사후 분석 | 매 5초 UPDATE 부담 |
| D. 하이브리드 (Redis 최신 + DB 이력) | 둘 다 | O | 둘 다 | 구현 복잡 |

→ **A 채택** (Q6-A) — 4-C 인프라 그대로. 사후 분석은 Phase 6+ tech-debt.

### 송신 빈도 (D6)

- a. 1s — 부하 큼, 부드러움 최고
- **b. 5s 발표 / 10s 운영 (둘 다 명시)** — 발표 시연용 5s, 운영 부하 절감용 10s
- c. 30s — 부드러움 약함

→ **b 채택** (D6) — 발표 시연 시 5s, 운영 전환 시 10s로 변경 (`.env` 설정값)

---

## 결정 (Decision)

### Redis 키 설계

```
key:   rider:loc:{riderNo}
value: JSON {"lat": 35.125, "lng": 128.456, "updatedAt": "2026-05-05T17:01:23"}
TTL:   30s (송신 5~10s 간격이라 2~6 tick 넘기면 stale)
```

### 라이더 위치 송신 (Rider → rider-service)

```
PUT /api/rider/location
{
  "lat": 35.125,
  "lng": 128.456
}

→ rider-service:
  1. SecurityContextHolder에서 callerUserNo 추출
  2. RiderRepository.findByUserNo(callerUserNo) → riderNo
  3. 권한 검증 (status=ACTIVE 또는 EATING이어야 송신 의미 있음 — REST/SUSPENDED는 거부)
  4. Redis SET rider:loc:{riderNo} JSON TTL 30s
  5. 응답 200
```

> 주의: D1 throw — Redis 다운 시 5xx (시작점 정합성, 4-C 패턴)

### 위치 조회 (Internal Main → Rider)

```
GET /internal/rider/{riderNo}/location

→ rider-service:
  1. Redis GET rider:loc:{riderNo}
  2. NULL이면 404 (라이더가 위치 송신 0회 또는 TTL 만료)
  3. 응답 200 + JSON
```

### 위치 broadcast (Main → 브라우저, STOMP)

Main이 손님 주문 상세 화면 STOMP connect 시 자동 시작:

```java
// Main의 LocationBroadcastService (의사코드)
@Scheduled(fixedDelayString = "${rider.location.broadcast.interval:2000}")  // 2s tick
public void broadcastActiveOrders() {
    // 진행 중 배달 (orders.delivery_state IN (1, 2)) 조회
    List<ActiveOrder> orders = orderRepository.findActiveDeliveries();

    for (ActiveOrder order : orders) {
        // Feign으로 라이더 위치 조회 (timeout 5s)
        try {
            RiderLocation loc = riderInternalClient.getLocation(order.getRiderNo());
            if (loc != null) {
                // STOMP convertAndSend
                stompTemplate.convertAndSend(
                    "/topic/order/" + order.getOrderId() + "/location",
                    loc
                );
            }
        } catch (Exception e) {
            // best-effort — broadcast skip, warn 로그
            log.warn("location broadcast failed for orderId={}", order.getOrderId(), e);
        }
    }
}
```

> 주의: D1-bis best-effort — Feign 실패해도 다음 tick에서 재시도. 손님 화면 위치 미갱신은 일시적.

### 송신 빈도 설정 (D6)

```yaml
# rider-service application.yml (라이더 클라이언트가 PUT하는 빈도는 클라이언트 책임)
# Main의 broadcast tick 주기:
rider:
  location:
    broadcast:
      interval: ${RIDER_LOCATION_BROADCAST_MS:2000}  # 2s tick (Main → STOMP)
```

`.env`로 빈도 설정. 학원 발표는 2s, 운영 전환은 5s 권장.

라이더 클라이언트 PUT 빈도 (D6-b):
- 발표: 5s
- 운영: 10s
- → 클라이언트(앱/웹)에 환경 설정으로 전달

### STOMP 토픽 인가

```
Subscribe /topic/order/{orderId}/location
  → Spring Security WebSocket Subscribe interceptor
  → SecurityContextHolder의 사용자가 해당 orderId의 customer인지 검증
  → 위반 시 ErrorMessage 또는 disconnect
```

> Phase 5-R5에서 검증 패턴 작성 (interfaces.md 참조)

---

## 결과 (Consequences)

### Phase 5-R5 신규 인프라

```gradle
// mmg-main-service/build.gradle (Phase 5-R5)
implementation 'org.springframework.boot:spring-boot-starter-websocket'
// rider-service는 Phase 4-C spring-data-redis 그대로 활용 (의존성 추가)
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

### Phase 5-R5 검증 케이스

- 위치 PUT 정상 (1건)
- Redis 다운 시 D1 throw 5xx (1건 — 4-C 패턴 그대로)
- 위치 조회 NULL 시 404 (1건 — TTL 만료)
- STOMP connect + subscribe + 1 tick broadcast (통합 테스트, 1건)
- STOMP 인가 위반 (다른 사용자가 다른 orderId 구독 시도, 1건)
- broadcast Feign 실패 시 best-effort skip (1건)

### Pub/Sub 미도입 영향 (Q4-X)

- Main 단일 인스턴스 가정 — 모든 STOMP 클라이언트가 같은 JVM의 simple broker에 연결
- 다중 Main 진입 시 broadcast 누락 → tech-debt 등재 (Y 옵션)

---

## 트레이드오프

| 항목 | 채택 결과 | 미래 고려 사항 |
|---|---|---|
| Redis KV | 4-C 인프라 재사용, 단순 | 사후 분석 불가 — Phase 6+ DB 이력 검토 |
| TTL 30s | stale 위치 자동 정리 | 클라이언트 송신 중단 시 30s 동안 stale 위치 broadcast 가능 — updatedAt 비교로 클라이언트 측 필터 |
| 5s/10s 송신 | 발표/운영 둘 다 만족 | 클라이언트 환경 설정 필요 |
| Main 2s broadcast tick | 부드러운 이동 시연 | tick마다 모든 active 배달의 라이더 Feign 호출 — 활성 배달 N건 시 N call/2s. 동시 1000건 진입 시 병목, Phase 6+ Pub/Sub Y 도입 |

---

## 미해결 / Phase 5에서 결정

- STOMP CONNECT/SUBSCRIBE 인가 검증 패턴 (Phase 5-R5)
- 라이더 클라이언트(앱/웹) 위치 송신 라이브러리 — 별건 (프론트팀 협의)
- Phase 6+ Pub/Sub Y 옵션 도입 시점 — 다중 Main 진입 트리거

---

## 관련 메모리

- `project_phase4c_state.md` — D1/D1-bis 패턴
- `feedback_dead_config_avoidance.md` — Pub/Sub 미도입 근거
- `feedback_feign_null_domain_split.md` — Feign null 처리 (broadcast best-effort)
