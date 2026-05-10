# ADR-004: 라이더 상태 머신 (7개 상태)

> **상태**: Accepted (2026-05-05)
> **관련 결정**: Q5 (락), D5 (충돌 처리), Figma 정정 2·3
> **관련 Figma**: `../../figma/` 픽업 상태 변경 모달들 (가게도착/픽업대기/픽업완료)

---

## 상황 (Context)

기존 모놀리식 `orders.delivery_state`는 3개 값만 (1배달전 / 2픽업완료 / 3배달완료, `main-schema.sql:172`). Figma 분석 결과, 픽업 단계가 더 세분화되어야 함 (정정 2):
- 가게 도착 (ARRIVED_AT_STORE)
- 픽업 대기중 (AWAITING_PICKUP)
- 픽업 완료 (PICKED_UP)

각 단계가 별개 모달로 설계됨. 또한 3-D UserAddress 사례에서 검증된 낙관적 락 적용 필요 (라이더 단일 액터지만 동시 변경 가능성 0이라고 단정 X — 비정상 클라이언트/네트워크 재시도).

orders.delivery_state는 응답 동결 (CLAUDE.md §6 규칙 7) — 프론트가 1/2/3 그대로 사용. delivery.status (라이더 도메인 7개) → orders.delivery_state 매핑 필요.

---

## 옵션 (Options)

### 상태 개수

- A. 5개 (진단 시 — WAITING_ASSIGN/ASSIGNED/PICKED_UP/DELIVERING/DELIVERED)
- **B. 7개 (Figma 정정)** — 가게도착 + 픽업대기 추가
- C. 더 세분화 (DELIVERY_FAILED 등 분기) — Phase 6+ 검토

→ **B 채택** (정정 2)

### 락 방식 (Q5)

- **A. 낙관적 `@Version`** — 라이더 단일 액터 가정 충분
- B. 비관적 `PESSIMISTIC_WRITE` — Phase 6+ 관리자 강제 변경 등 다중 액터 대비

→ **A 채택** (Q5-A)

### 동시 변경 충돌 시 응답 (D5)

- **a. 메시지 + HTTP 409 Conflict** (`OptimisticLockException` → `BusinessException(HttpStatus.CONFLICT)` — R1-A 정착 패턴 일관)
- b. 500 Internal Server Error (그대로 propagate)

→ **a 채택** (D5)

---

## 결정 (Decision)

### 상태 전이 다이어그램

```
[WAITING_ASSIGN]
       ↓ Main이 라이더에게 assign
[ASSIGNED]
   ↓ accept            ↓ reject
[ARRIVED_AT_STORE]   [WAITING_ASSIGN]  (재할당 대기)
       ↓ 가게 도착 버튼
[AWAITING_PICKUP]
       ↓ 픽업 완료 버튼
[PICKED_UP]
       ↓ 자동 (혹은 라이더 수동 "이동 시작")
[DELIVERING]
       ↓ DIRECT/CUSTOMER_REQUEST/CUSTOMER_ABSENT 사유 + (선택) 사진
[DELIVERED]
```

추가 분기:
- 어느 단계든 ADMIN이 강제 CANCELLED 가능 (Phase 5-R7 정산 영향, 별건)
- REJECTED 후 재할당 정책: MVP는 점주 수동 재요청 (Phase 5-R6)

### 화이트리스트 (DeliveryService 검증)

| from | to (허용) |
|---|---|
| WAITING_ASSIGN | ASSIGNED |
| ASSIGNED | ARRIVED_AT_STORE, WAITING_ASSIGN (reject 시) |
| ARRIVED_AT_STORE | AWAITING_PICKUP |
| AWAITING_PICKUP | PICKED_UP |
| PICKED_UP | DELIVERING |
| DELIVERING | DELIVERED |
| DELIVERED | (terminal) |

위반 시: `BusinessException("invalid state transition: " + from + " → " + to)` HTTP 400.

### orders.delivery_state 매핑 (정정 3)

| delivery.status | orders.delivery_state | 비고 |
|---|---|---|
| WAITING_ASSIGN | 1 (배달전) | |
| ASSIGNED | 1 (배달전) | |
| ARRIVED_AT_STORE | 1 (배달전, 가게에 있음) | 정정 3 |
| AWAITING_PICKUP | 1 (배달전, 가게에 있음) | 정정 3 |
| PICKED_UP | 2 (픽업완료) | |
| DELIVERING | 2 (픽업완료) | |
| DELIVERED | 3 (배달완료) | |

→ 응답 동결 (프론트 영향 0). PUT `/internal/order/{orderId}/delivery-status` 시 매핑 후 main이 orders.delivery_state UPDATE.

### 낙관적 락 (Q5-A)

- delivery entity에 `@Version private Long version`
- DeliveryService.updateStatus 시 dirty checking → JPA가 UPDATE WHERE version=? 자동 생성
- 충돌 시 Hibernate `ObjectOptimisticLockingFailureException` → **DeliveryService 내부 `try-catch + saveAndFlush()` 패턴으로 `BusinessException(HttpStatus.CONFLICT)` 변환 후 throw** → mmg-common `GlobalExceptionHandler.handleBusiness()`(line 26-31)가 `e.getStatus()` 동적 매핑으로 HTTP 409 응답 (별도 `@RestControllerAdvice` 추가 X — R1-A `RiderService.java:57` 정착 패턴 일관, mmg-common 미수정 = 영역 ✅)
- 변환 위치 명시 = R3-a 정정 후속 (사례 #19 정정, 2026-05-10) — 트랜잭션 commit 시점 발생을 `saveAndFlush()`로 메서드 내부 즉시 발생 + try-catch 캐치
- tech-debt: rider 단독 `@RestControllerAdvice` 미래 확장 (다른 Service에서도 OptimisticLockException 발생 시 일관 처리 필요할 시 검토)

### 충돌 응답 (D5-a)

```json
{
  "resultMessage": "동시 변경 충돌이 발생했습니다. 새로고침 후 다시 시도하세요.",
  "resultData": null
}
```

> 응답 형식: 실측 `ResultResponse` 2-key(message/data). `resultCode` 필드 부재 — `GlobalExceptionHandler.java:30` `new ResultResponse<>(e.getMessage(), null)` 정착 (사례 #17-B 정정).

### delivery_log 자동 INSERT

- DeliveryService.updateStatus 내부에서 from/to/actor 기록
- `@Transactional` 같은 트랜잭션 안에서 delivery UPDATE + delivery_log INSERT
- 실패 시 둘 다 롤백 (원자성)

### 비정상 전이 방어 + 권한 검증

```
public void updateStatus(String deliveryNo, DeliveryStatus to,
                         long callerUserNo, ActorRole callerActorRole) {
    Delivery delivery = repo.findById(deliveryNo)
            .orElseThrow(() -> new BusinessException("배달을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

    // 권한: RIDER 액터만 본인 배달 검증 (Objects.equals 패턴). ADMIN/SYSTEM은 검증 X (R7 강제 변경 / 자동 처리)
    if (callerActorRole == ActorRole.RIDER) {
        Rider caller = riderRepo.findByUserNo(callerUserNo)
                .orElseThrow(() -> new BusinessException(
                        "라이더 프로필이 등록되지 않았습니다.", HttpStatus.NOT_FOUND));
        if (!Objects.equals(delivery.getRiderNo(), caller.getRiderNo())) {
            throw new BusinessException("본인 배달이 아닙니다.", HttpStatus.FORBIDDEN);
        }
    }

    // 화이트리스트 검증
    DeliveryStatus from = delivery.getStatus();
    if (!ALLOWED_TRANSITIONS.get(from).contains(to)) {
        throw new BusinessException(
                "invalid state transition: " + from + " -> " + to,
                HttpStatus.BAD_REQUEST);
    }

    // 상태 변경 + 단계별 시각 자동 기록 (Delivery.changeStatus 단일 비즈니스 메서드)
    delivery.changeStatus(to, LocalDateTime.now());

    // saveAndFlush + try-catch — OptimisticLockException 메서드 내부 즉시 발생 + 변환 (사례 #19 결정 11 (i))
    try {
        repo.saveAndFlush(delivery);
    } catch (ObjectOptimisticLockingFailureException e) {
        throw new BusinessException(
                "동시 변경 충돌이 발생했습니다. 새로고침 후 다시 시도하세요.",
                HttpStatus.CONFLICT);
    }

    // log 기록 (같은 트랜잭션, callerActorRole 매개변수 명시 = 결정 8 (가) R1-A 정착 패턴 일관)
    deliveryLogRepo.save(new DeliveryLog(deliveryNo, from, to, callerActorRole, callerUserNo));

    // Main에 동기화 (Feign, 같은 트랜잭션 내부 호출 회피 — Phase 4-A 패턴)
    // → updateStatus 호출 측에서 트랜잭션 커밋 후 별도 호출
}
```

> 주의: Feign 호출은 `@Transactional` 외부에서 (PaymentService.confirmPayment의 toss 외부 호출 패턴 — tech-debt 등재됨, 라이더에서 처음부터 적용)

---

## 결과 (Consequences)

### Phase 5-R3 검증 케이스 (단위 테스트)

- 7개 상태 전이 화이트리스트 통과 케이스 — **7건** (사례 #20 정정, 2026-05-10. ASSIGNED→WAITING_ASSIGN reject 포함, 합법 전이 1+2+1+1+1+1+0=7)
- 비정상 전이 BusinessException(HttpStatus.BAD_REQUEST) 케이스 — **12건 대표 매핑** (전수 35건 중 대표):
  - WAITING_ASSIGN → {PICKED_UP, DELIVERING, DELIVERED}: 3건 (단계 건너뜀 + terminal 직행)
  - ASSIGNED → {AWAITING_PICKUP, PICKED_UP}: 2건 (단계 건너뜀)
  - ARRIVED_AT_STORE → DELIVERED: 1건 (terminal 직행)
  - AWAITING_PICKUP → DELIVERED: 1건 (terminal 직행)
  - PICKED_UP → WAITING_ASSIGN: 1건 (역방향)
  - DELIVERING → AWAITING_PICKUP: 1건 (역방향)
  - DELIVERED → {ASSIGNED, PICKED_UP, DELIVERING}: 3건 (terminal 후 모든 to 비합법, 대표 3종)
- 권한 위반 BusinessException(HttpStatus.FORBIDDEN) 케이스 — 1건
- 낙관적 락 충돌 시 BusinessException(HttpStatus.CONFLICT) 케이스 — 1건 (`@Version` mock 시뮬, `saveAndFlush` 패턴)
- delivery 부재 시 BusinessException(HttpStatus.NOT_FOUND) 케이스 — 1건 (`findById.orElseThrow`)
- delivery_log 같은 트랜잭션 INSERT 케이스 — 1건 (ArgumentCaptor verify)
- orders.delivery_state 매핑 통합 테스트 — 3건 (WAITING/PICKED/DELIVERED, R3-c 분리)
- **R3-b 단위 합계 = 23건 (7 합법 + 12 비합법 + 1 권한 + 1 충돌 + 1 NOT_FOUND + 1 log INSERT)** + R3-c 통합 3건 = R3 전체 26건

### Phase 5-R6 (외부 endpoint)

- 각 상태 변경 endpoint와 화이트리스트 일관성 검증
- 응답 동결 — orders.delivery_state 1/2/3 그대로

### tech-debt

- ADMIN 강제 CANCELLED + 정산 영향 → Phase 5-R7
- REJECTED 후 자동 재배차 알고리즘 → Phase 6+

---

## 트레이드오프

| 항목 | 채택 결과 | 미래 고려 사항 |
|---|---|---|
| 7개 상태 | Figma 정확 반영, UX 정밀 | 단위 테스트 케이스 수 증가 (6+12+1+1) |
| 낙관적 락 | 단일 액터 가정 충분, 데드락 0 | 다중 액터 진입 시 비관적 검토 |
| HTTP 409 | RESTful 정확, 클라이언트 재시도 가능 | 클라이언트가 409 핸들링 안 하면 UX 저하 — 프론트 협의 필요 |
| delivery_log 같은 트랜잭션 | 원자성 보장 | log INSERT 실패 시 상태 변경 전체 롤백 — 운영 중 log 테이블 fallback 필요 시 별도 |

---

## 미해결 / Phase 5에서 결정

- 사진 업로드 시점 — DELIVERED 전 vs 후 (Phase 5-R6)
- DELIVERING → DELIVERED 자동 이동 vs 라이더 수동 (Phase 5-R6)
- ADMIN 강제 CANCELLED 권한 (Phase 5-R7)

---

## 관련 메모리

- `feedback_owner_check_pattern.md` — Objects.equals 권한 비교
- `feedback_no_assumption_on_sql.md` — 화이트리스트 검증 후 진행
- `project_phase4a_backfill_state.md` — @Transactional 외부 Feign 호출 패턴
