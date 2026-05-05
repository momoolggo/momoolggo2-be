# ADR-007: 라이더 정산 도메인

> **상태**: Accepted (2026-05-05)
> **관련 결정**: D10 (admin 수동 confirm), Figma 정정 5
> **관련 Figma**: `../../figma/` 정산 화면 (매우 정밀한 설계)

---

## 상황 (Context)

진단 시점에는 정산을 "MVP 제외"로 분류했으나 Figma 화면이 매우 정밀 — 운행일/배달건/이동거리/배달료 합계/수수료/세금/보험료/실 수령액/주간 일정까지 명세됨 (정정 5).

ADR-002에서 settlement 테이블 정의:
- 주간 정산 (이번주 → 다음주 월요일 입금)
- 트랜잭션 단위
- admin 수동 confirm (D10-b)

라이더 화면 요구사항 (Figma + REQ-RDR-003):
- 누적 배달건/이동거리/배달비 합계 (기간 필터)
- 이번 주 정산내역
- 정산 계좌 변경 (account_bank/no/holder)

---

## 옵션 (Options)

### 정산 주기

- a. 일간 — 매일 자정 집계
- **b. 주간** — 이번주 월~일 → 다음주 월요일 입금
- c. 월간 — 매월 1일

→ **b 채택** (Figma 명시)

### 정산 트리거 (D10)

- a. 자동 스케줄 (배치 — 매주 월요일 새벽 자동 집계)
- **b. admin 수동 confirm** — admin이 검토 후 confirm 버튼 클릭

→ **b 채택** (D10-b) — 자동 배치는 Phase 6+

### 산출 공식

- 단순 (배달료 합계만)
- **복합 (배달료 - 수수료 - 세금 - 보험료 = 실수령액)** — Figma 표시 그대로

→ 복합 채택. Phase 5-R7에서 정확한 비율 결정 (수수료율/세율 3.3% 명시)

### 이동 거리 산출

- A. 직선 거리 (pickup → delivery)
- B. 실제 경로 거리 (외부 API 또는 라이더 위치 이력)

→ MVP는 **A 채택** (단순). B는 Phase 6+ tech-debt.

---

## 결정 (Decision)

### settlement 엔티티 (ADR-002 참조)

```
settlement_no (PK)
rider_no (FK)
period_start, period_end (DATE — 월요일~일요일)
delivery_count
total_distance_m
total_base_fee
total_extra_fee
commission (수수료)
tax (세금 3.3%)
insurance (보험료)
payout (실 수령액)
status (PENDING / CONFIRMED)
confirmed_by_admin_no
confirmed_at
paid_at
```

### 산출 공식 (Phase 5-R7)

```
gross         = total_base_fee + total_extra_fee
commission    = gross * COMMISSION_RATE   (e.g. 0.10)
tax           = (gross - commission) * 0.033
insurance     = INSURANCE_PER_WEEK         (e.g. 5000원/주)
payout        = gross - commission - tax - insurance
```

> COMMISSION_RATE, INSURANCE_PER_WEEK 등 정확한 값은 Phase 5-R7에서 결정 (Figma 검토 + 학원 정책).

### 흐름 (D10-b admin 수동)

```
1. 라이더 화면: GET /api/rider/settlement?startDate=&endDate= (Figma 기간 필터)
   → SettlementService.findByRiderAndPeriod()
   → status=CONFIRMED인 것만 조회 + status=PENDING인 진행 중 정산도 표시 (현재 주간)

2. 라이더 화면: GET /api/rider/settlement/account → account_bank/no/holder 표시
3. 라이더 화면: PUT /api/rider/settlement/account → 변경

4. 매주 월요일 자정 (혹은 임의 시점):
   - admin 화면에서 "이번 주 정산 집계" 버튼 클릭
   - admin-service: GET /internal/settlement/calculate?period_start=&period_end=
   - rider-service: 해당 기간 DELIVERED 배달 집계 → settlement INSERT (status=PENDING)

5. admin 검토:
   - admin 화면에서 PENDING 정산 목록 확인
   - 라이더별 detail (Figma 정산 상세 모달) 확인
   - confirm 버튼 → POST /internal/settlement/{settlementId}/confirm
   - rider-service: settlement.status=CONFIRMED, confirmed_by_admin_no, confirmed_at 기록
   - paid_at은 별도 (실 입금 처리 후 admin이 다시 갱신 — Phase 6+ 자동화)
```

### 권한 검증

- 라이더: 자기 settlement만 조회 (rider_no = callerRider.riderNo, Objects.equals 패턴)
- admin: 모든 settlement 조회 + confirm 권한 (ROLE_ADMIN)
- account 변경: 라이더 본인만

### 응답 dto (예시)

```json
{
  "settlementNo": 42,
  "periodStart": "2026-05-04",
  "periodEnd": "2026-05-10",
  "deliveryCount": 18,
  "totalDistanceM": 24500,
  "totalBaseFee": 72000,
  "totalExtraFee": 9000,
  "commission": 8100,
  "tax": 2407,
  "insurance": 5000,
  "payout": 65493,
  "status": "PENDING",
  "confirmedAt": null,
  "paidAt": null
}
```

---

## 결과 (Consequences)

### Phase 5-R7 작업 분량

- settlement 엔티티 + Repository (단순 CRUD JPA)
- SettlementService (집계 로직 + admin confirm)
- 외부 endpoint (라이더 4개): history / account get / account update / current-week
- Internal endpoint (admin 2개): calculate / confirm
- 단위 테스트: 산출 공식 정확성 (수수료/세금/보험료 케이스)
- 통합 테스트: 라이더 권한 (자기 정산만), admin confirm 흐름

### 학원 발표 데모 (시간 여유 시 — figma-analysis.md 데모 3)

- 라이더 정산 화면 → 이번주 PENDING 표시
- admin 화면 → confirm 버튼 → CONFIRMED 전환
- 정산 상세 모달 (배달 건별)

### tech-debt

- Phase 6+ 자동 배치 스케줄 (매주 월요일 자정 자동 집계)
- Phase 6+ 실 경로 거리 산출 (외부 API 또는 라이더 위치 이력)
- Phase 6+ paid_at 자동 갱신 (실 입금 시스템 연동)
- Phase 6+ 정산 detail 감사 로그 (산출 공식 변경 이력)

---

## 트레이드오프

| 항목 | 채택 결과 | 미래 고려 사항 |
|---|---|---|
| 주간 + admin 수동 | Figma 정확 + admin 검토 가치 | admin 수작업 부담 — Phase 6+ 자동화 |
| 직선 거리 | 단순, 외부 API 의존 X | 실제 라이더 이동 거리와 차이 — 정산 분쟁 위험 (학원 환경 무방) |
| 산출 공식 (수수료 + 세 + 보험) | Figma 정확 반영 | 비율 변경 시 settlement 행 갱신 vs 신규 행 — Phase 5-R7 결정 |
| account 단일 테이블 (Q3-A) | MVP 단순 | 변경 이력 + 정산 감사 — Phase 6+ |

---

## 미해결 / Phase 5-R7에서 결정

- COMMISSION_RATE, INSURANCE_PER_WEEK 정확한 값 (Figma + 학원 정책)
- 정산 집계 시 부분 환불/취소 배달 처리 정책
- 정산 검토 중 라이더가 account 변경 시 처리 (PENDING settlement에 영향?)

---

## 관련 메모리

- `feedback_owner_check_pattern.md` — Objects.equals 권한 비교
- `feedback_dead_config_avoidance.md` — 자동 배치 보류 근거
- `project_phase3_backfill_state.md` — 단순 CRUD JPA 패턴 (settlement 도메인 적합)
