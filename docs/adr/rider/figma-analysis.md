# Figma 분석 결과 + 진단 정정 박제 (라이더 정리)

> **목적**: 라이더 정리 사전 설계 시점의 진단(코드/문서 기반)과 사용자가 Figma 화면을 검토하여 발견한 차이를 박제. ADR-001~009 작성 시 헌법.
> **자산**: `docs/figma/스크린샷 2026-05-05 17XXXX.png` 10장 (라이더 화면)
> **분석 일자**: 2026-05-05

---

## 진단 가정 정정 — 누적 사례 (라이더 정리 추가)

> 메모리 `feedback_verify_diagnostic_assumptions.md` 누적 원칙. Phase 4 시리즈에서 5건 정착, 라이더 정리에서 4건 추가 → **총 9건**.

| # | 단계 | 가정 | 검증 결과 |
|---|---|---|---|
| 1 | 4-A | ResultResponse 필드명 | 정정 |
| 2 | 4-B | build.gradle testImplementation | 정정 |
| 3 | 4-B | InternalBlock 헤더 검증 동작 | 정정 |
| 4 | 4-B | CorsFilter 자동 등록 | 정정 |
| 5 | 4-C | JwtUser 메서드명 (getUserNo → getSignedUserNo) | 정정 |
| 6 | 라이더 진단 | mmg-common Role enum 존재 | **거짓** — JwtUser.role은 String 필드, enum 부재 |
| 7 | 라이더 진단 | main에 /internal/order/** 엔드포인트 존재 | **거짓** — main에 /internal/ 매핑 0건. Rider→Main 상태 전파 흐름 미구현 |
| 8 | 라이더 진단 | mmg_rider DDL 또는 main에 rider/delivery 테이블 존재 | **거짓** — orders 테이블에 rider_request, delivery_state 컬럼만. rider/delivery 테이블 0 |
| 9 | 라이더 진단 | Gateway rider 라우트는 Phase 5에서 추가 예정 | **거짓** — 이미 정의됨 (`mmg-gateway/application.yml:58-62` rider-route → 8082) |
| 10 | 라이더 정리 (Step 3) | Figma 13장 | **거짓** — 실제 10장 (`docs/figma/`) |
| 11 | R6-FE 사용 중 발견 (2026-05-19) | "BE 가입 endpoint 통합 + Feign 호출 누락이 정답, FE 정상" (작업 명세 4단계 + [정정] 박제 #30 정정) | **거꾸로** — ADR-001 line 23-71 박제 정답은 "(C) 채택: rider→auth Feign 호출 없음, 클라이언트(FE)가 두 endpoint 순차 호출". 사가 패턴 회피로 (A) 옵션 명시 거부됨. FE RiderSignupView.signup()이 `userService.signup()` 1회만 호출하고 `riderService.putProfile()` 호출 누락 = **FE 흐름 결함**이 박제 정답. [정정] 항목 자체가 박제 검증 없이 작성됨 → 6번 강제 절차 발동. F1 작업으로 정정. |
| 12 | R6-FE 사용 중 발견 (2026-05-19) | Rider entity account_* nullable=true vs RiderService.validate requireNonBlank 검증 (박제 내부 모순) | 박제 모순 — entity (Rider.java:50-57 + rider-schema.sql:23-25) nullable=true가 박제 정답, validate가 entity 박제 위반. 사용자 결정 A1'(계좌 가입 시 X, 마이페이지에서) 정합성 위해 validate에서 account_* 검증 제거. |
| 13 | 라이더 가입 중 발견 (2026-05-19) | "BE 예외 처리는 친화 메시지 변환됨" (FE catch 가정) | **거짓** — `mmg-common GlobalExceptionHandler.java:71-76` `handleRuntime`이 RuntimeException 전체 catch → `e.getMessage()` 그대로 응답. `DataIntegrityViolationException extends RuntimeException`라 Unique 제약 위반 시 SQL raw 메시지("could not execute statement [Duplicate entry 'rider2' for key 'uq_user_id']") 그대로 노출. 별도 핸들러 0건. FE rider 영역에서 `RiderSignupView.mapSignupError` 매퍼 임시 대응 + `team-handoff.md` Q-DataIntegrity + Q-SignupDupCheck 등재. |
| 14 | 라이더 진입 화면 사용 중 발견 (2026-05-19) | "라이더 랜딩에 로그인 동선 존재" (UX 가정) | **거짓** — `RiderLandingView.vue` 박제는 "라이더 신청하기" 버튼 1건만. 로그인 버튼/링크 0건. 기존 라이더 재진입 동선 부재(가입 화면만). `/rider/signin` 라우트 + `RiderSigninView` 모두 박제되어 있어 동선만 누락. RiderLandingView에 "이미 라이더이신가요? 로그인" 링크 추가로 정정. |
| 15 | (D) 통합 승인 박제 신설 시점 발견 (2026-05-19) | "DB 테이블 분리 = 비즈니스 승인 흐름 분리" (ADR-001 (C) 박제 결함) | **혼동 패턴** — (C) 박제 의도는 "DB 트랜잭션 분리(auth와 rider 별 트랜잭션, 사가 패턴 회피)"였으나, "비즈니스 승인 흐름도 2단계로 분리"로 잘못 확장 적용. 결과: admin이 같은 라이더에 대해 `/admin/user`에서 1회 + `/admin/rider`에서 1회 = **2번 클릭** 필요. 비즈니스 직관 위반. `RiderApprovalController.java:17` 주석 박제에 이미 "Q-A18 (b) cross-schema 정합성 → Phase 6+ outbox tech-debt 등재"로 인지됐으나 (D) 신설로 admin Feign 조율 + try-catch 보상으로 MVP 해결. **박제 #34 정정 동반** (admin/user는 일반 회원만 처리, RIDER는 /admin/rider 통합 승인). 사장 도메인은 박제 부재(owner.owner 테이블 0건)로 (D) 적용 X — auth.user.status 단독 마스터 유지. |
| 16 | (D) 구현 직후 사용자 정정 발견 (2026-05-19) | "RIDER는 /admin/rider에서 승인, /admin/user에서 'RIDER는 다른 화면으로' 안내" (본인이 사용자 작업 명세 4-7을 그대로 해석) | **거꾸로** — 사용자 진짜 의도: "라이더든 사장이든 회원관리(/admin/user)에서 승인". /admin/rider는 라이더 단독 관리(목록/제재)만. 사용자 작업 명세 자체가 본인 의도와 반대로 작성됐는데 본인이 박제 의도 확인 없이 명세 그대로 구현 → 5번 강제 절차 발동(`feedback_verify_spec_assumptions.md` 6번 절차 위반 사례). 정정: `/admin/user` AdminUserController.updateApproval에서 status=ACTIVE 시 RiderApprovalService.approveByUserNoIfRider(userNo) 호출 (라이더면 통합, 아니면 skip). FE AdminUserView "라이더 관리로" 버튼 revert → "승인하기" 원위치. 박제 #34 재정정 (메인 진입은 /admin/user 통합, /admin/rider는 보조 경로). |
| 17 | (D) 작업 종료 후 사용자 SQL DELETE 시 dangling rider 발견 (2026-05-19) | "MSA에서도 두 테이블 DB 자동 연결" (사용자 가정) | **거꾸로** — CLAUDE.md §3 박제: "MSA 경계 외부 참조는 논리 FK만, 물리 FK 금지. application 레벨 보장". `Rider.java:13-15` + `rider-schema.sql:15` 박제 일관. 사용자가 SQL로 user 직접 삭제 = application 레벨 우회 → rider 잔존이 박제 자연 결과. 단 admin user delete endpoint **자체가 박제 0건**이라 가입/승인은 (F1/D)로 보장됐지만 삭제 cascade는 박제 부재. 본 작업으로 신설: `DELETE /api/admin/user/{userNo}` → `RiderApprovalService.deleteByUserNoIfRider` → `riderFeign.deleteRiderByUserNo` → `auth.deleteUser` 순. rider 먼저 삭제 후 auth (dangling rider 회피). |
| 18 | code-reviewer FAIL 검증 (2026-05-19) | "FE WaitingTab의 '배차 수락' 버튼 → BE `accept` endpoint 호출이 풀 잡기 흐름 처리" (FE-BE 명세 정렬 가정) | **거꾸로** — code-reviewer 검증 결과 Critical 2건. `DeliveryService.acceptDelivery:374-378` 박제는 ASSIGNED → ARRIVED_AT_STORE (본인 ASSIGNED 가정), `performRiderTransition:473-474` 권한 검증이 `Objects.equals(NULL, riderNo)` → 403. ADR-004 line 53-56 박제 "Main이 라이더에게 assign"이지만 라이더 풀 모델에서는 라이더 self-claim 필요. claim endpoint 박제 **0건** = HIGH 결함. 본 작업으로 신설: `DeliveryService.claimDelivery` 별 메서드 (performRiderTransition X) + `PUT /api/rider/order/{deliveryNo}/claim` + FE `deliveryService.claim` + WaitingTab "배차 수락" 호출 변경. ACTIVE 라이더만 D8-a 박제 일관. Warning #3~#6 (FE 버튼 의미 / ADMIN cancel reason / Feign timeout / 순서 의존성) 별 트랙. |
| 19 | claim 신설 후 InProgressTab 다음 단계 버튼 미노출 (2026-05-19) | "InProgressTab은 ASSIGNED 상태 처리" (R6-FE 박제 가정) | **거꾸로** — `InProgressTab.vue:14-26` 박제(reviewer C-1 정정): "ASSIGNED는 accept(R6-FE-3)가 처리하므로 InProgressTab 진입 시점은 ARRIVED부터". 그러나 (D) 작업 claim 흐름은 WAITING_ASSIGN → ASSIGNED만 변경 (ARRIVED 아님). 결과: InProgressTab의 nextAction map에 ASSIGNED 항목 0건 → 버튼 미노출 → 라이더가 가게 도착/픽업/배달 진행 불가. 본 작업으로 ASSIGNED 항목 1줄 추가 + accept 분기 + 박제 주석 정정. R6-FE 박제(C-1 정정) 의도가 (D) 작업으로 무효 — 박제 간 의존성 추적 필요 패턴. |
| 20 | ARRIVED_AT_STORE 단계 UX 중복 발견 (2026-05-19) | "ADR-004 7-state는 모두 라이더 UX 필요" (ADR-004 박제 가정) | **부분 정정** — Figma 정정 2(7-state 신설)는 박제 정답이나 실제 라이더 UX에서 "가게 도착"(ASSIGNED→ARRIVED_AT_STORE) + "가게 도착 확인"(ARRIVED_AT_STORE→AWAITING_PICKUP) 두 단계 중복. 라이더 관점 "가게 도착 = 픽업 대기 진입"이 자연. 본 작업으로 ARRIVED_AT_STORE 단계 라이더 흐름에서 제외: BE `acceptDelivery` to=AWAITING_PICKUP 직행 + ALLOWED_TRANSITIONS에 `ASSIGNED → AWAITING_PICKUP` 추가 (기존 ARRIVED_AT_STORE 경로 보존). FE InProgressTab map에서 ARRIVED_AT_STORE 항목 + arrive fn 제거. ARRIVED_AT_STORE 상태 enum은 유지 (외부 호출/admin 경로 보존). 정정된 라이더 흐름: ASSIGNED → AWAITING_PICKUP → PICKED_UP → DELIVERING → DELIVERED (5-step). 사용자 보고 "완료 안 됨"의 근본 원인 = ARRIVED_AT_STORE 단계 막힘. |

**원칙**: 진단 시 가정한 사실은 코드/실행/파일 검증 후 단언. 20건 누적 정착.

---

## Figma 분석 정정 사항 (10건) — 사용자 검토 결과 박제

> 코드/문서 기반 진단(ADR 작성 전 보고)과 Figma 실제 화면 사이의 차이.
> 각 정정은 ADR-002 데이터 모델 / ADR-004 상태 머신 / ADR-007 정산 / ADR-008 근무 세션에 반영됨.

### 정정 1. rider 엔티티 필드 추가 (ADR-002)

- 추가: `license_type` (운전면허 종류), `vehicle_type` (배달수단 — 도보/자전거/오토바이/자동차), `account_holder` (계좌주명)
- 근거: Figma 회원가입 화면 — 운전면허/배달수단 드롭다운 / 정산 화면 — 계좌주명 입력
- 영향: ADR-002 rider 테이블 컬럼 추가, ADR-007 정산 화면에 account_holder 표시

### 정정 2. delivery 상태 5개 → 7개 (ADR-004)

- 진단: `WAITING_ASSIGN → ASSIGNED → PICKED_UP → DELIVERING → DELIVERED` (5개)
- 정정: `WAITING_ASSIGN → ASSIGNED → ARRIVED_AT_STORE → AWAITING_PICKUP → PICKED_UP → DELIVERING → DELIVERED` (7개)
- 근거: Figma 픽업 상태 변경 모달들 — 가게도착 / 픽업대기중 / 픽업완료가 별개 모달로 존재
- 영향: ADR-004 상태 전이 다이어그램 + Service 화이트리스트 7개

### 정정 3. orders.delivery_state 매핑 (ADR-004)

```
WAITING_ASSIGN, ASSIGNED                 → 1 (배달전)
ARRIVED_AT_STORE, AWAITING_PICKUP        → 1 (배달전, 가게에 있는 상태)
PICKED_UP, DELIVERING                    → 2 (픽업완료)
DELIVERED                                → 3 (배달완료)
```

- 근거: 정정 2 반영 + 기존 `orders.delivery_state` 3개 값 (`main-schema.sql:172`) 호환
- 영향: 응답 동결 (CLAUDE.md §6 규칙 7) — 프론트의 delivery_state=1/2/3 그대로 사용

### 정정 4. 배달료 base_fee + extra_fee 분리 (ADR-002)

- delivery 테이블에 `base_fee`, `extra_fee` 분리 컬럼 (단일 `delivery_fee` X)
- 근거: Figma "4,000원 + 1,500원" 명시
- 영향: ADR-002 delivery 컬럼 추가, ADR-007 정산 시 base/extra 합산

### 정정 5. 정산 도메인 MVP 포함 격상 (ADR-007 신설)

- 진단: 정산은 학원 발표 제외 가능 후순위
- 정정: MVP 포함 — Figma 정산 화면이 매우 정밀하게 설계됨
- 신설 엔티티: `settlement` (정산 트랜잭션)
- 필드: 운행일, 배달건수, 이동거리, 배달료 합계, 수수료, 세금(3.3%), 보험료, 실 수령액, 상태 (대기/입금완료)
- 주기: 주간 (이번주 → 다음주 월요일 입금)
- 근거: Figma 정산 화면 매우 정밀
- 영향: ADR-007 신설, Phase 5-R7 학원 데모 포함

### 정정 6. work_session 도메인 추가 (ADR-008 신설)

- 진단: 누락
- 정정: 신설 엔티티 `work_session`
- 컬럼: `session_no` PK, `rider_no` FK, `started_at`, `ended_at`, `work_seconds`, `break_seconds`, `vehicle_type`
- 근거: Figma 근무관리 화면 — 총 배달 시간 04:20 / 휴게 시간 01:10 표시
- 영향: ADR-008 신설, Phase 5-R8 추가

### 정정 7. 상태 토글 라벨 분리 (ADR-008)

- 내부 enum: `ACTIVE` / `EATING`
- UI 라벨: "배달중" / "식사중"
- 근거: Figma 메인 토글 + D8 결정 (식사중 시 배차 차단)
- 영향: ADR-008 + Phase 5-R8

### 정정 8. 공지사항 도메인 추가 (ADR-009 신설)

- 진단: 누락
- 정정: 신설 엔티티 `notice`
- 카테고리: `IMPORTANT(중요)` / `SAFETY(안전)` / `GENERAL(일반)`
- 흐름: admin → rider broadcast (admin-service 의존)
- 라이더 측: GET 조회만, 작성/관리는 admin
- 근거: Figma 공지사항 화면
- 영향: ADR-009 신설, Phase 5-R9 추가

### 정정 9. 주문번호 ≠ 배차번호 분리 (ADR-002)

- `delivery_no` (배차번호, 형식: `00001ABC`) — mmg_rider.delivery PK
- `order_id` (주문번호, 형식: `000001A`) — mmg_main.orders FK (논리)
- 근거: Figma 명시 ("배차번호랑 다름")
- 영향: ADR-002 delivery 테이블 PK 별도 + order_id 논리 FK

### 정정 10. 전달 완료 사유 분류 + 사진 (ADR-002, ADR-006)

- `delivered_method`: `DIRECT` (직접 전달) / `CUSTOMER_REQUEST` (고객 요청) / `CUSTOMER_ABSENT` (고객 부재)
- `delivered_photo_url`: 사진 URL (선택, 직접 전달 X 시 권장)
- 근거: Figma "고객님께 직접 전달하지 않는 사유" 모달
- 사진 인증: 우선순위 낮음 (구현 가능 시만 — Figma 메모 "촬영 시스템 구현할 수 있으면 하고 아니면 못함")
- 영향: ADR-002 delivery 컬럼 추가, ADR-006 사진은 main-service 단독 책임 (CLAUDE.md §5)

### 추가 정정 11. 픽업지/손님 전화번호 평문 노출 (ADR-002, D7)

- delivery 응답 dto에 `pickup_phone` (가게 전화), `customer_phone` (손님 전화) **평문**
- 근거: Figma 가게 053-111-2222, 손님 010-1234-5678 평문 표시
- 결정: D7-a 그대로 평문 (마스킹/프록시는 Phase 6+ 개인정보 강화)
- 영향: ADR-002 delivery 응답 dto + ADR-007/008/009 무관

---

## 결정 매트릭스 (Q1~Q8 + D5~D10)

### Q 시리즈 (진단 보고 시 사용자 결정 항목)

| ID | 질문 | 결정 | 근거 |
|---|---|---|---|
| Q1 | 라이더 가입 위치 | **(C)** auth-service만 + 추가정보 별도 endpoint | 사가 패턴 회피, 4-A InternalUserController 패턴 재사용 |
| Q2 | 면허 인증 / 관리자 승인 | **(B)** PENDING → admin 승인 → ACTIVE | admin-service Phase 5 동시 진행이라 흐름 검증 가치 |
| Q3 | rider_account 분리 | **(A)** rider 테이블 합침 | MVP 단순. 변경 이력은 Phase 6+ |
| Q4 | Pub/Sub 도입 시점 | **(X)** MVP 미도입 | dead config 회피 (메모리 `feedback_dead_config_avoidance.md`) |
| Q5 | 상태 머신 락 | **(A)** 낙관적 `@Version` | 라이더 단일 액터 가정 (자기 배달만 변경) |
| Q6 | 위치 저장 방식 | **(A)** Redis KV `rider:loc:{riderNo}` TTL 30s | 4-C Redis 인프라 재사용. Geospatial은 Phase 6+ |
| Q7 | 작업 우선순위 R1~R9 | (확정) 그대로 | — |
| Q8 | tech-debt 처리 시점 | 5-R1과 함께 (gateway timeout 우선) | 라이더 라우트 활성화 직전이라 적합 |

### D 시리즈 (Figma 분석 후 추가 결정)

| ID | 질문 | 결정 | 근거 |
|---|---|---|---|
| D5 | 상태 동시 변경 충돌 | 메시지 + HTTP 409 (`OptimisticLockException` → `ConflictException`) | 낙관적 락 (Q5-A) 채택 결과 |
| D6 | 위치 송신 빈도 | **5s 발표 / 10s 운영** (둘 다 명시) | 발표 시 부드러운 이동 시연 vs 운영 부하 |
| D7 | 손님 전화번호 노출 | **(a)** 평문 노출 그대로 | Figma 010-1234-5678 평문 표시 그대로. 마스킹/프록시는 Phase 6+ 개인정보 강화 |
| D8 | EATING 시 배차 | **(a)** 식사중 = 신규 배차 알림 0 | 다 먹고 ACTIVE 토글 후 알림 재개 |
| D9 | 업무 종료 시맨틱 | **(a)** "업무 종료" 버튼 = `work_session.ended_at` 기록, 로그인 세션 유지 | 로그인/로그아웃은 별개 토글 |
| D10 | 정산 승인 방식 | **(b)** admin이 정산 검토 후 confirm 버튼 | 자동 스케줄은 Phase 6+ 배치 |

---

## 학원 발표 데모 시퀀스 (상세)

### 사전 준비

1. `docker compose up -d redis` (Phase 4-C 인프라)
2. 5 서비스 동시 기동 — gateway:8000 / auth:8081 / main:8080 / rider:8082 / admin:8083
3. fixture INSERT — ACTIVE 라이더 1명, 점주 1명, 손님 1명, 가게/메뉴 1세트
4. STOMP 클라이언트 (또는 손님 화면 앱) 라이더 위치 구독 준비

### 데모 1: 라이더 가입 + 승인 (3분)

- 화면: Figma 회원가입 화면
- 흐름: rider-service POST /api/rider/join → auth-service Feign user 생성 (RIDER role) → rider-service rider 프로필 PENDING
- admin 화면: PENDING 라이더 목록 → 승인 버튼 → ACTIVE 전환
- 검증: rider-service GET /api/rider/me → status=ACTIVE

### 데모 2: 주문 → 배차 → 픽업 → 배달 (5분)

- 손님: POST /api/order
- 점주: PUT /api/owner/order/accept → orders.order_state=3
- 점주: 배차 요청 → main → Feign rider POST /internal/rider/{n}/assign
  - 라이더 EATING 시 거절 (D8 검증 — 시연 시 일시 EATING 토글)
  - 라이더 ACTIVE 시 ASSIGNED → orders.order_state=5
- 라이더: PUT /api/rider/order/{id}/accept → ARRIVED_AT_STORE → AWAITING_PICKUP → PICKED_UP → DELIVERING
- 라이더 위치: 5초 간격 PUT /api/rider/location → Redis KV 업데이트
- Main: 1~2초 tick에서 Redis 조회 → STOMP `/topic/order/{orderId}/location` send
- 손님 화면: 지도 위 라이더 마커 실시간 이동 확인
- 라이더: PUT /api/rider/order/{id}/complete (사유=DIRECT, 사진 선택) → orders.delivery_state=3

### 데모 3 (시간 여유): 정산 + 근무 세션 + 공지 (3분)

- 라이더: 정산 화면 → 이번주 정산 내역 (settlement.status=PENDING)
- admin: 정산 검토 → confirm 버튼 (D10) → settlement.status=CONFIRMED
- 라이더: 근무 세션 → started_at/work_seconds 표시 → "업무 종료" → ended_at 기록 (로그인 유지, D9)
- admin: 공지 작성 (IMPORTANT) → 라이더 공지 목록 갱신

---

## ADR ↔ Figma 화면 매핑 (10장 인덱스, 2026-05-07 시각 검증 정정 4건)

| Figma 파일 | 화면 내용 (시각 검증) | ADR 관련 |
|---|---|---|
| `스크린샷 2026-05-05 170116.png` | 라이더 랜딩 ("배달 파트너 신청하기" 버튼) | ADR-001 진입점 (회원가입/로그인 화면 X) |
| `스크린샷 2026-05-05 170121.png` | 라이더 회원가입 (이름/아이디/비번/성별/생년월일/휴대폰/운전면허/배달수단/약관) | ADR-001 + ADR-002 정정 1 (license_type/vehicle_type) |
| `스크린샷 2026-05-05 170125.png` | 3장 묶음: ① 라이더 로그인 ② 배달대기 ③ 배달현황(상세확인+지도) | ADR-001 (로그인) + ADR-004 (배달대기/현황) |
| `스크린샷 2026-05-05 170131.png` | 배달현황 (대기/진행/완료 탭, 픽업대기중/픽업완료 모달) | ADR-004 상태 머신 |
| `스크린샷 2026-05-05 170137.png` | 배달현황 (배달중 + 지도 + 픽업지 정보) | ADR-005 위치 추적 |
| `스크린샷 2026-05-05 170143.png` | 배달중 + 직접 전달 사유 모달 (고객 요청/고객 부재) | ADR-002 정정 10 (delivered_method) |
| `스크린샷 2026-05-05 170148.png` | 배달완료 + 사진 촬영 + 완료 모달 + 배달내역 5건 | ADR-002 정정 10 + ADR-006 (사진은 main-service 단독) |
| `스크린샷 2026-05-05 170153.png` | 메인 + 배달중/식사중 토글 + 사이드 메뉴 (공지사항/배달내역/근무관리) + 업무종료 | ADR-008 근무 세션 + 토글 |
| `스크린샷 2026-05-05 170158.png` | 공지사항 (중요/안전/일반 + 본문 예시) | ADR-009 공지사항 |
| `스크린샷 2026-05-05 170202.png` | 정산 (오늘 정산/이번주 내역/계좌) + 근무관리 (운행 상태/오늘의 운행 기록) | ADR-007 정산 + ADR-008 근무 세션 |

> **2026-05-07 정정 4건** (R1-FE 라이더 가입/로그인 화면 진입 시점, Q-Spec6 (b) 8장 시각 검증 결과):
> 1. 170116 = 라이더 랜딩 (회원가입/로그인 묶음 X). 회원가입은 170121 단독.
> 2. 170125 = 라이더 로그인 + 배달대기 + 배달현황 (3장 묶음, 첫 번째가 로그인). ADR-004 상태 머신 묶음 X.
> 3. 170148 = 배달완료 + 사진 + 배달내역 (정산 X). 정산은 170202.
> 4. 170158 = 공지사항 단독 (ADR-002 모델 종합 X). 종합은 별도 매핑 미요.
>
> **주의**: 본 매핑은 시각 검증 후 박제. ADR 본문 인용은 `../../figma/` 디렉토리 전체 참조 패턴 유지.

---

## 누적 패턴 적용 체크리스트 (라이더 처음부터)

> 메모리 `feedback_*` 누적 원칙. Phase 5-R1~R9 작성 시 항목별 체크.

- [ ] 권한 분기: `verifyRiderSelf(deliveryNo, callerUserNo)` Objects.equals (메모리 `feedback_owner_check_pattern.md`)
- [ ] dto.userNo 위조 방지: 명시 403 throw (메모리 `feedback_dto_userno_forgery.md`)
- [ ] Feign null 가드 + critical 분기 (메모리 `feedback_feign_null_domain_split.md`)
- [ ] @Transactional / readOnly 일관 (조회 readOnly, 쓰기 @Transactional)
- [ ] Snapshot 동결 (응답 dto, Phase 3 STRICT 패턴)
- [ ] Feign timeout (connect 3s / read 5s, Phase 4-A)
- [ ] 통합 테스트 셋업: SecurityContextHolder + fixture INSERT + em.clear (메모리 `feedback_integration_test_setup.md`)
- [ ] Redis D1/D1-bis: 시작점 throw / 종료점 best-effort (메모리 `project_phase4c_state.md`)
- [ ] 한 항목 = 한 개념 (tech-debt 등재 시, 메모리 `feedback_w2_split_null_vs_exception.md`)
- [ ] dead config 회피 (Pub/Sub 미도입, 메모리 `feedback_dead_config_avoidance.md`)
- [ ] 진단 가정 = 코드 검증 (라이더 정리 4건 정정, 누적 9건)
- [ ] 가짜 테스트 0건 (CLAUDE.md §6.5 NAJACKS 재발 방지)
- [ ] 응답 동결: orders.delivery_state 1/2/3 매핑 유지 (정정 3)

---

## tech-debt 등재 (라이더 정리에서 발견 / Phase 5 또는 후속 처리)

| 항목 | 처리 시점 |
|---|---|
| Phase 6+ 개인정보 강화 — 손님 전화번호 마스킹/프록시 (D7-a 결과) | Phase 6+ |
| Phase 6+ 사가 패턴 — 라이더 가입 시 auth user 생성 후 rider 프로필 생성 실패 보상 (Q1-C 결과) | Phase 6+ |
| Phase 6+ 다중 라이더 fan-out 시 Pub/Sub 도입 (Q4-X 결과) | Phase 6+ |
| Phase 6+ Redis Geospatial 가용 라이더 검색 (Q6-A 결과) | Phase 6+ |
| Phase 6+ 정산 자동 배치 스케줄 (D10-b 결과) | Phase 6+ |
| Phase 6+ 위치 사후 분석 / DB 영속화 (Q6-A 결과) | Phase 6+ |
| Phase 6+ 변경 이력 — rider_account 분리 + audit (Q3-A 결과) | Phase 6+ |
| 5-R1과 함께: gateway timeout 설정 (rider/admin 라우트 활성화) | Phase 5-R1 |
| 5-R1과 함께: getUsers `?ids=` empty 통합 테스트 보완 | Phase 5-R1 |
| 5-R1과 함께: GatewayIntegrationTest cosmetic 2건 (W-1/W-2) | Phase 5-R1 |
| RefreshTokenStore mmg-common 이관 — Q1-C로 라이더 별도 인증 X → 이관 불필요 (tech-debt 항목 폐기) | (해소) |

---

## 검증 원칙 (이 문서 갱신 시)

- 라이더 정리 도중 새로운 정정/결정 발견 시 본 문서에 추가 박제
- ADR 본문에서는 **결정만** 명시, 본 문서에서는 **정정 사실 + 결정 매트릭스** 박제
- Phase 5-R1 진입 후 코드 작성 중 ADR 충돌 발생 시: 본 문서에 정정 사례 추가 + 해당 ADR 갱신 (재커밋)
