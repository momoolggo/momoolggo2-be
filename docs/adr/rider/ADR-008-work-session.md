# ADR-008: 라이더 근무 세션 + 상태 토글

> **상태**: Accepted (2026-05-05)
> **관련 결정**: D8 (EATING 시 배차 차단), D9 (업무 종료 시맨틱), Figma 정정 6·7
> **관련 Figma**: `../../figma/` 근무관리 화면 + 메인 토글

---

## 상황 (Context)

진단 시점에는 work_session 도메인이 누락 (rider.status 토글만 인지). Figma 검토 결과:
- 근무관리 화면에 총 배달 시간 (예: 04:20) / 휴게 시간 (예: 01:10) 표시
- 운행일별 누적
- 메인 화면 상단에 "배달중/식사중" 토글
- "업무 종료" 버튼 (별개)

→ 정정 6: work_session 신설
→ 정정 7: 상태 토글 라벨 분리 (내부 ACTIVE/EATING, UI "배달중/식사중")

상태 토글 시맨틱 결정 필요:
- D8: EATING 시 새 배차 차단? → 식사중 = 알림 0
- D9: "업무 종료" = 로그아웃? → work_session.ended_at만 기록, 로그인 별개

---

## 옵션 (Options)

### EATING 시 신규 배차 (D8)

- **a. 차단** — EATING이면 main이 가용 라이더 후보에서 제외
- b. 허용 — EATING이라도 배차 알림 보내고 라이더가 거절

→ **a 채택** (D8) — Figma 의도 + UX 자연스러움 (식사 중 알림 방해 X)

### 업무 종료 시맨틱 (D9)

- **a. work_session.ended_at만 기록, 로그인 유지** — "업무 종료" 버튼은 근무 세션 종료, 로그아웃은 별도
- b. 로그아웃 호출과 동일 — "업무 종료" = signout

→ **a 채택** (D9) — Figma 메인 화면에 별도 로그아웃 버튼 + 업무 종료 별도

### work_session vs rider.status 관계

- 옵션 1: work_session 시작/종료가 status 토글에 자동 영향
- **옵션 2: 분리 — work_session.start = ACTIVE 진입 + 시간 누적, status 토글은 ACTIVE↔EATING 전환만**

→ **옵션 2 채택** (개념 분리)

---

## 결정 (Decision)

### 상태 토글 시맨틱

| 내부 enum | UI 라벨 | 의미 | 배차 |
|---|---|---|---|
| ACTIVE | "배달중" | 일하는 중 | 가용 (배차 후보) |
| EATING | "식사중" | 휴게 (식사) | **차단 (D8-a)** |
| PENDING | (가입 후) | admin 승인 대기 | 차단 |
| SUSPENDED | (admin 제재) | 이용 제한 | 차단 |

### work_session 흐름

```
[로그인]
   → status = (이전 상태 또는 PENDING)
   → work_session 생성? NO. ACTIVE 진입 시 생성

[ACTIVE 토글 (배달중)]
   → work_session 신규 생성 (started_at=now, ended_at=null)
   → 또는 기존 진행 중 세션이 있으면 그대로

[EATING 토글 (식사중)]
   → work_session.ended_at 변경 X (세션 유지)
   → 별도로 break 시작 시간 기록 (내부 메모리 또는 별 컬럼)
   → ACTIVE 복귀 시 break 누적 시간 = work_session.break_seconds 증가

[ACTIVE 복귀 (식사중 → 배달중)]
   → work_session.break_seconds += (now - break_started_at)

[업무 종료 버튼 클릭 — D9-a]
   → work_session.ended_at = now
   → work_session.work_seconds = ended_at - started_at - break_seconds
   → status = (변경 없음, 그대로 유지) ← 또는 자동 EATING으로 (선택, MVP는 그대로 유지)
   → 응답 200, 세션 종료 안내
   → 로그인 세션 유지 (signout 호출 X)

[로그아웃 (signout)]
   → 별개 흐름 (auth-service signout)
   → work_session 진행 중이면? Phase 5-R8에서 결정 (자동 ended_at 기록 권장)
```

### 배차 차단 검증 (D8-a, ADR-003 Feign)

```java
// rider-service의 InternalRiderController
@PostMapping("/internal/rider/{riderNo}/assign")
public AssignResponse assign(@PathVariable long riderNo, @RequestBody AssignRequest req) {
    Rider rider = riderRepository.findById(riderNo).orElseThrow(...);

    // D8-a 차단 검증
    if (rider.getStatus() != RiderStatus.ACTIVE) {
        // EATING / PENDING / SUSPENDED 모두 거부
        throw new BusinessException("rider not available: " + rider.getStatus());
    }

    // 배차 처리 (delivery 생성 + status 머신 ASSIGNED 진입)
    ...
}
```

> Main 측에서 가용 라이더 검색 시점에도 status=ACTIVE 필터 (Phase 5-R4)

### 업무 종료 endpoint

```
POST /api/rider/work-session/end
- 진행 중 work_session.ended_at 기록
- 미완료 배달 (DELIVERING 등) 있으면? 거부 (REQ-RDR-006 "진행 중 배달 시 종료 불가")
- 응답 200 + 누적 시간 표시
- 로그인 세션 무관 (signout 호출 X)
```

> 별도 endpoint 명세는 interfaces.md 참조

### 응답 dto (예시)

근무관리 화면:
```json
{
  "todaySession": {
    "sessionNo": 1,
    "startedAt": "2026-05-05T09:00:00",
    "workTime": "04:20",     // HH:mm 변환
    "breakTime": "01:10",
    "vehicleType": "MOTORBIKE"
  },
  "weeklySummary": {
    "totalDeliveries": 18,
    "totalWorkSeconds": 50400
  }
}
```

---

## 결과 (Consequences)

### Phase 5-R8 작업 분량

- work_session 엔티티 + Repository
- WorkSessionService (start/end + break 누적)
- 외부 endpoint:
  - PUT /api/rider/status (ACTIVE↔EATING 토글)
  - POST /api/rider/work-session/end (D9 업무 종료)
  - GET /api/rider/work-session/today
  - GET /api/rider/work-session/summary?period=
- 통합 테스트:
  - status 토글 + work_session 자동 생성 (1건)
  - EATING 시 배차 차단 (1건 — Internal POST 호출 결과 4xx)
  - 업무 종료 + 미완료 배달 있을 시 거부 (1건)
  - 업무 종료 후 로그인 유지 (1건 — signout 무관 검증)
  - break 누적 정확성 (1건 — EATING → ACTIVE 전환 후 break_seconds 증가)

### 학원 발표 데모 (시간 여유 시 — figma-analysis.md 데모 3)

- 라이더 근무관리 화면 → 진행 중 세션 + 시간 누적
- "식사중" 토글 → 메인의 점주 배차 시도 4xx (EATING 차단 시연)
- "배달중" 복귀 → 배차 정상
- "업무 종료" 버튼 → ended_at 기록, 화면에 누적 시간 표시
- (로그인 유지 → 다른 endpoint 호출 200 검증)

### tech-debt

- Phase 6+ 로그아웃 시 진행 중 work_session 자동 종료 정책
- Phase 6+ break 시간 측정 — 메모리 vs 별 컬럼 vs 매 토글마다 work_session_break 행 추가

---

## 트레이드오프

| 항목 | 채택 결과 | 미래 고려 사항 |
|---|---|---|
| EATING 차단 (D8) | UX 자연스러움 (식사 중 알림 방해 X) | 점주 배차 후보 부족 시점에 라이더 알림 옵션 검토 — Phase 6+ |
| 업무 종료 별개 (D9) | 로그인 vs 근무 분리 명확 | 로그아웃 시 work_session 자동 종료 처리 누락 시 진행 중 세션 잔존 — Phase 5-R8 검증 |
| status 토글 + work_session 분리 | 개념 명확 | 코드 양 증가 (2 도메인 분리) |
| ended_at = now - break | 정확성 | break 시간 측정 방식이 메모리 의존 시 서버 재기동 시 초기화 — Phase 5-R8에서 별 컬럼 검토 |

---

## 미해결 / Phase 5-R8에서 결정

- break 시간 측정 방식 (메모리 vs 별 컬럼 vs 별 테이블)
- 로그아웃 시 진행 중 work_session 자동 종료 정책
- 자동 EATING 진입 (예: ACTIVE 1시간 무위치 송신 시) — Phase 6+

---

## 관련 메모리

- `feedback_dto_userno_forgery.md` — work-session/end 권한 검증
- `feedback_owner_check_pattern.md` — Objects.equals
- `feedback_dead_config_avoidance.md` — 자동 EATING 진입 보류 근거
