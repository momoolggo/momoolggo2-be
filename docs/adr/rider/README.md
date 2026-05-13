# 라이더 정리 (Phase 5 진입 전 사전 설계)

> **단계 성격**: 코드 작성 0. ADR + 인터페이스 명세 + Figma 분석 정정 박제만.
> **목적**: Phase 5-R1 코드 작성 시점의 헌법. 결정 박제.
> **위치**: develop 브랜치에서 진행 (라이더 정리는 문서 산출물만이라 브랜치 분리 불필요).

---

## ADR 인덱스 (9건)

| ADR | 주제 | 핵심 결정 (요약) |
|---|---|---|
| [ADR-001](ADR-001-service-boundary.md) | 서비스 경계 | mmg-rider-service 분리 + 인증은 auth-service 통합 (Q1-C) + 라이더 추가정보(license/account) 별도 endpoint |
| [ADR-002](ADR-002-data-model.md) | 데이터 모델 + DB 분리 | mmg_rider 신설 6 테이블 (rider / delivery / delivery_log / work_session / settlement / notice). rider_location은 Redis (ADR-005). 외부 참조 논리 FK |
| [ADR-003](ADR-003-communication-pattern.md) | 통신 패턴 | Feign(동기) + WebSocket+STOMP(실시간 푸시). Redis Pub/Sub MVP 미도입 (Q4-X dead config 회피) |
| [ADR-004](ADR-004-state-machine.md) | 상태 머신 (7개 상태) | WAITING_ASSIGN → ASSIGNED → ARRIVED_AT_STORE → AWAITING_PICKUP → PICKED_UP → DELIVERING → DELIVERED. 낙관적 락 `@Version` (Q5-A) |
| [ADR-005](ADR-005-location-tracking.md) | 위치 추적 | Redis KV `rider:loc:{riderNo}` TTL 30s + Main STOMP push `/topic/order/{orderId}/location`. 송신 빈도 5s/10s |
| [ADR-006](ADR-006-redis-usage.md) | Redis 활용 (4-C 연결) | KV 위치만. Pub/Sub / Geospatial 미도입. RefreshTokenStore 그대로 auth |
| [ADR-007](ADR-007-settlement.md) | 정산 도메인 | settlement 테이블, 주간(이번주→다음주 월요일 입금), admin 수동 confirm (D10-b). 자동 배치는 Phase 6+ |
| [ADR-008](ADR-008-work-session.md) | 근무 세션 + 상태 토글 | work_session 테이블, 상태 ACTIVE/EATING(내부) — UI "배달중/식사중". EATING 시 배차 차단 (D8-a). "업무 종료" = work_session.ended_at, 로그인 별개 (D9-a) |
| [ADR-009](ADR-009-notice.md) | 공지사항 도메인 | notice 테이블 (admin 작성, 라이더 GET). 카테고리 IMPORTANT/SAFETY/GENERAL. admin-service 의존 |

추가 산출물:
- [figma-analysis.md](figma-analysis.md) — Figma 분석 결과, 진단 정정 10건, 결정 매트릭스 (Q1~Q8 + D5~D10), 학원 발표 데모 시퀀스, 누적 가정 정정 9건 박제
- [interfaces.md](interfaces.md) — Feign 인터페이스 명세 (RiderInternalClient / MainInternalClient / AdminRiderClient / NoticeClient)

---

## Phase 5 작업 분할 (R1~R9)

| 단계 | 작업 | 의존 | 학원 발표 |
|---|---|---|---|
| **5-R1** | RIDER role 가입/로그인 (auth 분기) + license_type/vehicle_type 포함 + tech-debt cleanup (gateway timeout, getUsers empty) | — | ✅ |
| **5-R2** | mmg_rider 스키마 DDL + JPA entity 6개 + Repository | 5-R1 | ✅ |
| **5-R3** | 상태 머신 + DeliveryService + 단위 테스트 (7개 상태 화이트리스트) | 5-R2 | ✅ |
| **5-R4** | Internal API: Main↔Rider (assign/status/complete) + Admin→Rider (approve/suspend) + Feign 인터페이스 구현 | 5-R3 | ✅ |
| **5-R5** | 위치 추적 (Redis KV + WebSocket+STOMP + Main 1~2s tick push) | 5-R4 (병렬 가능) | ✅ |
| **5-R6** | 라이더 외부 endpoint (대기/수락/반려/가게도착/픽업대기/픽업/이동/완료 사유+사진) + 통합 테스트 (Phase 4-A 패턴) | 5-R4 (병렬 가능) | ✅ |
| **5-R7** | 정산 도메인 (settlement + admin confirm) | 5-R2 | 🟡 언급만 |
| **5-R8** | 근무 세션 + 상태 토글 (D8 EATING 배차 차단 / D9 업무 종료) | 5-R3 | 🟡 데모 |
| **5-R9** | 공지사항 (admin → rider broadcast) | admin-service 진행 동기화 | 🟡 데모 |

**학원 발표 최소 데모**: R1~R6 (5-R5 위치 + 5-R6 외부 endpoint 병렬). R7 정산은 화면만 보여주고 confirm 흐름만 시연. R8/R9는 시간 여유 시.

---

## 결정 매트릭스 요약 (figma-analysis.md 참조)

| 영역 | 결정 |
|---|---|
| Q1 가입 위치 | C — auth만 + 추가정보 별도 endpoint |
| Q2 면허/승인 | B — PENDING → admin 승인 → ACTIVE |
| Q3 account 분리 | A — rider 테이블 합침 (MVP) |
| Q4 Pub/Sub 시점 | X — MVP 미도입 (dead config 회피) |
| Q5 상태 락 | A — 낙관적 `@Version` |
| Q6 위치 저장 | A — Redis KV TTL 30s |
| Q7 작업 우선순위 | R1~R9 그대로 |
| Q8 tech-debt 처리 | 5-R1과 함께 (gateway timeout 우선) |
| D5 동시 변경 충돌 | 메시지 + HTTP 409 |
| D6 위치 송신 빈도 | 5s 발표 / 10s 운영 (둘 다 명시) |
| D7 손님 전화번호 | a — 평문 노출 (Phase 6+ 마스킹) |
| D8 EATING 배차 | a — 식사중 시 차단 |
| D9 업무 종료 | a — work_session.ended_at, 로그인 별개 |
| D10 정산 승인 | b — admin 수동 confirm |

---

## 학원 발표 데모 시퀀스 (figma-analysis.md 상세)

1. 라이더 가입 → admin 승인 → ACTIVE
2. 점주 주문 수락 → 가용 라이더에 배차 (Feign)
3. 라이더 수락 → ARRIVED_AT_STORE → PICKED_UP → DELIVERING (위치 STOMP push 실시간)
4. 손님 주문 상세 화면에서 라이더 위치 지도 표시
5. 라이더 DELIVERED + 사유/사진
6. (시간 여유) 정산 화면 + admin confirm 시연

---

## 진행 흐름 위치

`Phase 1 → 2 → 3 → 4-A → 4-B → 4-C → [라이더 정리 — 본 디렉토리] → Phase 5-R1 ...`

라이더 정리 종결 후 Phase 5-R1 진입 시점에:
- `git checkout rider` (기존 브랜치 사용, 신설 X)
- `git fetch origin` + `origin/rider` 동기화 점검
- develop 머지 필요 여부 확인 (라이더 정리 결과 + Phase 4-C 결과)
