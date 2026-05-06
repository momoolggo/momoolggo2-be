# ADR-003: 라이더 통신 패턴 (Feign + WebSocket+STOMP)

> **상태**: Accepted (2026-05-05)
> **관련 결정**: Q4 (Pub/Sub 도입 시점), Figma 정정 (실시간 위치 화면)
> **관련 Figma**: `../../figma/` 배달 중 실시간 위치 화면 + 지도

---

## 상황 (Context)

라이더는 4개 서비스(auth/main/admin/rider)와 통신해야 함:
- 배차 알림 (Main → Rider)
- 위치 송신 + broadcast (Rider → Main → 손님 브라우저)
- 배달 상태 변경 (Rider → Main `orders.delivery_state` 동기화)
- 가게/주문 정보 조회 (Rider → Main)
- 챗봇 ESCALATED 알림 (별건 — Phase 5 챗봇)

Phase 4-C 시점까지 검증된 인프라:
- Feign (auth `InternalUserController` + main `OwnerService.getUsers`) — Phase 4-A 백필
- Redis (RefreshTokenStore) — Phase 4-C
- SSE / WebSocket — 미도입
- Pub/Sub — 미도입 (dead config 회피, 라이더 정리 ADR로 결정 미뤄짐)

CLAUDE.md §7 라이더: "SSE로 사용자/사장에게 위치 푸시" 명시.

---

## 옵션 (Options)

### 시나리오별 옵션 매트릭스

| 시나리오 | 옵션 | 권장 |
|---|---|---|
| 배차 알림 (Main → Rider) | (a) Feign sync push / (b) Pub/Sub publish→subscribe / (c) Rider polling | **(a)** |
| 위치 broadcast (Rider 위치 → 손님) | (a) SSE / (b) WebSocket+STOMP / (c) Polling | **(b)** (의도적 학습/포트폴리오 가치) |
| 배달 상태 변경 (Rider → Main) | (a) Feign sync / (b) Pub/Sub | **(a)** |
| 가게/주문 정보 조회 (Rider → Main) | Feign sync | **(확정)** |
| 챗봇 ESCALATED 알림 | Phase 5 챗봇 정리 | (별건) |

### Pub/Sub 도입 시점 (Q4)

- **X. 도입 안 함 (MVP)** — 모든 통신 Feign + WebSocket만
- Y. 1개 채널만 도입 (위치 broadcast `rider.location.{riderNo}`)
- Z. 전체 비동기 (배차/상태/위치 모두 Pub/Sub)

→ **X 채택** (Q4-X) — dead config 회피 (메모리 `feedback_dead_config_avoidance.md`). MVP는 Main 단일 인스턴스 가정. 다중 인스턴스 운영 진입 시점에 Y로 진입 (tech-debt 등재).

### 위치 broadcast 기술 선택 (SSE vs WebSocket+STOMP)

- **SSE (단방향, 가벼움)** — 기술적으로 충분
- **WebSocket+STOMP (양방향, 부하 큼)** — 학습/포트폴리오 어필 가치 + 미래 채팅 확장 가능

→ **WebSocket+STOMP 채택** (의도적 선택 명시) — 기술적으론 SSE 충분하나 학원 발표/포트폴리오에서 "WebSocket을 다뤄봤다"는 가치가 크고, Phase 6+ 라이더-손님 채팅 확장 시 그대로 활용 가능

---

## 결정 (Decision)

### 통신 패턴 매트릭스

| 시나리오 | 패턴 | 엔드포인트 (interfaces.md 참조) |
|---|---|---|
| **배차 알림** | Feign Main → Rider sync push | `POST /internal/rider/{riderNo}/assign` |
| **라이더 위치 송신** | REST PUT (Rider → rider-service) | `PUT /api/rider/location` |
| **라이더 위치 조회 (Internal)** | Feign Main → Rider | `GET /internal/rider/{riderNo}/location` |
| **위치 broadcast** | WebSocket + STOMP (Main → 브라우저) | `/topic/order/{orderId}/location` |
| **배달 상태 변경** | Feign Rider → Main | `PUT /internal/order/{orderId}/delivery-status` |
| **배달 완료 (사진 포함)** | Feign Rider → Main + multipart | `POST /internal/order/{orderId}/complete` |
| **가게/주문 조회** | Feign Rider → Main | `GET /internal/order/{orderId}` 등 (Phase 5-R4) |
| **라이더 승인 (Admin)** | Feign Admin → Rider | `POST /internal/rider/{riderNo}/approve` |

### WebSocket + STOMP 인프라

- 의존성: `spring-boot-starter-websocket` + `org.springframework:spring-messaging` (STOMP)
- 도입 시점: Phase 5-R5
- 설정 위치: main-service (브라우저가 connect, broadcast endpoint)
- 토픽 네임스페이스:
  - `/topic/order/{orderId}/location` — 손님 위치 추적
  - `/topic/order/{orderId}/status` — 손님 상태 변경 알림 (선택)
- 인증: 기존 JWT 쿠키 → Spring Security WebSocket Handshake interceptor
- 라이더 → Main: 일반 REST PUT (위치 송신은 WebSocket 아님)

### Pub/Sub 미도입 근거 (Q4-X)

- MVP에서 Main 단일 인스턴스 가정 — 같은 JVM 내 STOMP convertAndSend로 모든 connect된 클라이언트에 전달 가능
- 다중 Main 인스턴스 진입 시점에 Y 옵션 (위치 broadcast만 Pub/Sub) 도입 — tech-debt 등재
- 메모리 `feedback_dead_config_avoidance.md` 원칙: 사용처 부재 시 인프라 도입 X

### 시나리오별 응답 / 에러 처리

| 시나리오 | 정상 | 실패 |
|---|---|---|
| 배차 (Feign sync) | 200 + assigned=true | 라이더 EATING/REST → 4xx + 다음 후보 (D8) |
| 위치 (REST PUT) | 200 + Redis save | Redis 다운 → D1 throw 5xx (시작점 정합성, 4-C 패턴) |
| 위치 broadcast (STOMP) | 1~2s tick에서 Main이 Redis 조회 → send | Redis 다운 → broadcast skip (warn 로그, 손님 화면 위치 미갱신은 일시적) |
| 상태 변경 (Feign Rider → Main) | 200 + delivery_state 동기화 | Main DB 다운 → D1 throw 5xx (라이더가 다시 시도) |
| 배달 완료 (Feign + multipart) | 200 + photo URL | 사진 업로드 실패 → 사유 이미지 없이 완료 가능 (D-bis best-effort, 정정 10) |

### Feign timeout (Phase 4-A 패턴)

- connect 3s / read 5s — 모든 라이더 관련 Feign client에 명시
- gateway timeout (tech-debt 5-R1과 함께 처리)

---

## 결과 (Consequences)

### 신규 의존성 (Phase 5-R5)

```gradle
// mmg-main-service/build.gradle
implementation 'org.springframework.boot:spring-boot-starter-websocket'
```

### 학원 발표 데모 가치

- WebSocket+STOMP 시연 — 손님 화면에서 라이더 위치 마커가 실시간 이동
- 의도적 선택 근거 명시 (SSE vs STOMP 선택 이유 발표 자료에 포함)

### Phase 6+ 확장

- Y 옵션 도입 (위치 broadcast Pub/Sub) → tech-debt 등재
- 라이더 ↔ 손님 채팅 (STOMP 양방향 활용) → CLAUDE.md §7 챗봇 확장

### 인증

- WebSocket Handshake 시 JWT 쿠키 검증 — Phase 5-R5 시점에 검증 패턴 작성
- STOMP CONNECT 프레임 시 Principal 추출 → 손님이 자기 주문만 구독 가능 검증

---

## 트레이드오프

| 항목 | 채택 결과 | 미래 고려 사항 |
|---|---|---|
| WebSocket+STOMP | 학습/포트폴리오 가치 + 채팅 확장 | SSE 대비 부하 약간 증가 (handshake + heartbeat) |
| Pub/Sub 미도입 | dead config 회피, 코드 단순 | 다중 Main 인스턴스 진입 시 위치 broadcast 누락 — Y 옵션 도입 |
| Feign sync 배차 | 단순, 강한 정합성 | 가용 라이더 fan-out 시 직렬 호출 — Phase 6+ Pub/Sub Y |
| timeout 명시 | Phase 4-A 패턴 일관 | gateway timeout 미설정은 tech-debt (5-R1과 함께) |

---

## 미해결 / Phase 5에서 결정

- WebSocket Handshake 시 인증 검증 패턴 (Phase 5-R5)
- 손님이 자기 주문만 구독 가능한 인가 (Phase 5-R5 Subscribe interceptor)
- STOMP 프록시 — Spring 내장 simple broker로 충분? (MVP 충분, 운영 시 RabbitMQ/ActiveMQ 검토)
- Phase 5 챗봇 통신 패턴 — STOMP 양방향 vs SSE (별건)

---

## 관련 메모리

- `feedback_dead_config_avoidance.md` — Q4-X 결정 근거
- `feedback_feign_null_domain_split.md` — Feign critical 분기 (배차 실패 시)
- `project_phase4a_backfill_state.md` — Feign timeout 패턴
- `project_phase4c_state.md` — D1/D1-bis 패턴 (위치 시작점 throw / broadcast best-effort)
