# ADR-009: 라이더 공지사항 도메인

> **상태**: Accepted (2026-05-05)
> **관련 결정**: Figma 정정 8 (도메인 누락 정정), admin-service 의존
> **관련 Figma**: `../../figma/` 공지사항 화면

---

## 상황 (Context)

진단 시점에 공지사항 도메인 누락. Figma 검토 결과:
- 라이더 화면에 공지사항 목록 (안전수칙, 서비스 태도, 긴급사항 등)
- 카테고리 분류
- admin이 작성/관리, 라이더는 읽기만

api-spec 명세:
- 라이더: `GET /api/rider/notice` (REQ-RDR-004)
- admin: `GET /api/admin/rider/notice?page=&sendType=` / `POST /api/admin/rider/notice` / `PUT/DELETE /api/admin/rider/notice/{noticeId}` (api-spec §5.6.4)
- Internal: admin → rider broadcast (api-spec §6.7.2)

→ 정정 8: notice 신설 (rider-service에 테이블, admin-service가 작성)

---

## 옵션 (Options)

### 도메인 위치

- A. mmg_admin 스키마에 notice 테이블 — admin이 작성/관리하니 자연
- **B. mmg_rider 스키마에 notice 테이블** — 라이더 화면이 읽는 데이터, rider-service가 도메인 소유
- C. 별도 mmg_common 스키마

→ **B 채택** (CLAUDE.md §3 도메인 분배 — 라이더가 읽기 주도이므로 rider 스키마)

### 작성 흐름

- A. admin-service가 직접 mmg_rider.notice INSERT (DB 직접 접근) — MSA 위반
- **B. admin-service가 rider-service Feign 호출** (`POST /internal/notice`)

→ **B 채택** (MSA 정석)

### 카테고리 enum

- A. IMPORTANT / SAFETY / GENERAL (Figma 추정)
- B. 자유 텍스트
- C. 더 세분화 (ACCIDENT / ROUTE / PROMOTION 등)

→ **A 채택** (Figma 분석 + MVP 단순)

### 발송 시점 (sendType)

- 즉시 (NOW) — 작성 즉시 라이더 목록에 표시
- 예약 (RESERVE) — published_at에 도달해야 표시

→ 둘 다 지원 (api-spec §5.6.4 sendType: NOW/RESERVE 명시)

---

## 결정 (Decision)

### notice 엔티티 (ADR-002 참조)

```
notice_no PK
category (IMPORTANT / SAFETY / GENERAL)
title VARCHAR(200)
content TEXT
published_at DATETIME (즉시 = now, 예약 = 미래)
sender_admin_no BIGINT (논리 FK → my_mmg_admin.admin)
created_at
```

### admin-service 의존 (Phase 5 동기화)

- admin-service Phase 5에서 작성 endpoint 만들면 → rider-service Feign 호출
- admin-service가 아직 미진행 시점이면 — rider-service 측 endpoint만 먼저 (Phase 5-R9). admin-service 진행 동기화 별도

### 흐름

```
[admin 작성]
  POST /api/admin/rider/notice (admin-service)
    → admin이 인증된 ROLE_ADMIN 검증
    → admin-service가 Feign으로 rider-service 호출:
       POST /internal/notice
       Body: {category, title, content, publishedAt, senderAdminNo}
    → rider-service: notice INSERT
    → 응답 200 (noticeId)

[admin 수정]
  PUT /api/admin/rider/notice/{noticeId} → Feign PUT /internal/notice/{noticeId}

[admin 삭제]
  DELETE /api/admin/rider/notice/{noticeId} → Feign DELETE /internal/notice/{noticeId}

[admin 목록 조회]
  GET /api/admin/rider/notice?page=&sendType=
    → admin-service가 자체 필터 (또는 Feign rider GET /internal/notice?page=&sendType=)

[라이더 조회]
  GET /api/rider/notice
    → rider-service: published_at <= now() AND deleted=false 조회
    → 카테고리 필터 (?category=IMPORTANT)
    → 페이징 지원
```

### published_at 처리

- 즉시 (NOW): published_at = now()
- 예약 (RESERVE): published_at = 입력값
- 라이더 조회 시 `WHERE published_at <= NOW()` 필터로 자동 가시성 제어
- 별도 cron 불필요 (조회 시점 필터로 처리)

### 권한 검증

- admin-service: ROLE_ADMIN
- rider-service /internal/notice/**: Phase 4-B InternalBlockController 패턴 (Gateway 외부 차단)
- rider-service /api/rider/notice: ROLE_RIDER 인증 (모든 라이더 조회 가능)

### 응답 dto (예시)

```json
{
  "totalElements": 23,
  "page": 0,
  "size": 20,
  "content": [
    {
      "noticeNo": 42,
      "category": "IMPORTANT",
      "title": "5월 안전 운전 캠페인",
      "content": "...",
      "publishedAt": "2026-05-05T09:00:00",
      "senderAdminNo": 1
    }
  ]
}
```

### 카테고리 색상 (UI 힌트, 백엔드 무관)

| category | 의미 | UI 색상 (Figma 참조) |
|---|---|---|
| IMPORTANT | 중요 | 적색 |
| SAFETY | 안전 | 황색 |
| GENERAL | 일반 | 회색 |

> 백엔드는 enum만, UI 매핑은 프론트 책임

---

## 결과 (Consequences)

### Phase 5-R9 작업 분량 (rider-service)

- notice 엔티티 + Repository (단순 CRUD JPA)
- NoticeService (Internal POST/PUT/DELETE, 외부 GET)
- 외부 endpoint:
  - GET /api/rider/notice?category=&page= (라이더, ROLE_RIDER)
- Internal endpoint:
  - POST /internal/notice (admin → rider, ROLE_ADMIN 검증은 admin-service에서)
  - PUT /internal/notice/{noticeId}
  - DELETE /internal/notice/{noticeId}
  - GET /internal/notice?page=&sendType= (admin 목록 조회)
- 통합 테스트:
  - 라이더 GET 조회 (1건)
  - 라이더 카테고리 필터 (1건)
  - published_at 미래 시 미노출 (1건)
  - admin POST → 라이더 GET 즉시 반영 (1건)
  - Internal endpoint Gateway 차단 검증 (1건 — 4-B 패턴)

### admin-service 측 작업 (별건, Phase 5)

- admin-service Phase 5-A1~ (가칭) — 라이더 정리 ADR-009 참조
- 라이더 정리는 rider-service 측 인프라만 결정. admin-service가 호출하는 Feign 인터페이스는 interfaces.md에 명세

### 학원 발표 데모 (시간 여유 시 — figma-analysis.md 데모 3)

- admin 화면 → 공지 작성 (IMPORTANT) → 라이더 공지 목록 갱신 확인

### tech-debt

- Phase 6+ Push 알림 (라이더 디바이스로 즉시 알림)
- Phase 6+ 공지 읽음 표시 (라이더별 읽음 추적)
- Phase 6+ 공지 첨부 파일 (이미지/PDF) — main-service 위임 (CLAUDE.md §5)

---

## 트레이드오프

| 항목 | 채택 결과 | 미래 고려 사항 |
|---|---|---|
| rider 스키마에 notice | 라이더 도메인 단일 | admin 작성 흐름이 Feign 의존 — admin-service Phase 5 동기화 필수 |
| 카테고리 3개 (IMPORTANT/SAFETY/GENERAL) | Figma 정확 + MVP 단순 | 운영 중 추가 필요 시 enum 확장 |
| sendType NOW/RESERVE | 별도 cron 불필요 | published_at 조회 필터 의존 — 인덱스 필요 (`INDEX (published_at)`) |
| Push 알림 미도입 | MVP 단순 | Phase 6+ FCM/APNS 검토 |

---

## 미해결 / Phase 5-R9에서 결정

- admin-service 진행 시점과 동기화 — admin-service가 아직 미시작이면 rider-service 측 endpoint만 먼저 만들고 admin-service는 별건 진행
- 카테고리 정확한 분류 (Figma 메모 추가 검토 필요 시)
- 공지 첨부 이미지 (Figma에 명시 X — Phase 6+)

---

## 관련 메모리

- `feedback_dead_config_avoidance.md` — Push 알림 보류 근거
- `project_phase4b_backfill_state.md` — Internal endpoint Gateway 차단 패턴
- `project_phase4a_backfill_state.md` — Feign 패턴
