# 라이더 Feign 인터페이스 명세

> **상태**: Accepted (2026-05-05) — orderId 타입 정정 (case-#34, 2026-05-16) + §1.1 path 정정 (case-#33-후속, 2026-05-17)
> **목적**: ADR-001~009 결정에 따른 서비스 간 인터페이스 시그니처. 구현 0, Phase 5-R1~R9에서 작성.
> **공통**: 모든 Feign client에 timeout `connect 3s / read 5s` 명시 (Phase 4-A 패턴)
> **orderId 박제**: Long (main `orders.order_id` BIGINT AUTO_INCREMENT 일관). Figma "000001A" 표기는 UI zero-pad formatter 의도 (Phase 6+ tech-debt 별 트랙).
> **§1.1 배차 흐름**: 라이더 풀 모델 박제 (team-handoff §8 R6 종결 결과 우선, Q-A9.a (β+δ) 정정). path `POST /internal/rider/assign` (riderNo path 제거), body 박제 — riderNo NULL/0이면 WAITING_ASSIGN, 명시되면 강제 배차 (admin 시연 호환).
> **case-#33 잔존 path 차이**: §3.3/§3.4 settlement path는 실제 rider Provider 박제(`/internal/rider/settlement/...`)와 차이. 별 정정 트랙 (작업 A 범위 외).

---

## 명명 규칙

| Client | 위치 | 호출 방향 | 인증 |
|---|---|---|---|
| `RiderInternalClient` | mmg-main-service / mmg-admin-service | Main/Admin → Rider | X-Internal 헤더 (Phase 6+ mTLS) |
| `MainInternalClient` | mmg-rider-service | Rider → Main | X-Internal 헤더 |
| `AuthInternalClient` | mmg-rider-service / mmg-admin-service | Rider/Admin → Auth | X-Internal 헤더 (Phase 4-A 기존) |

> X-Internal 헤더 검증은 Phase 6+에서 강화 (현재 Gateway 차단으로 외부 노출 0). 메모리 `project_phase4b_backfill_state.md` 참조.

---

## 1. RiderInternalClient (Main/Admin → Rider)

### 1.1 배차 요청 (Main → Rider) — case-#33-후속 정정 (2026-05-17, Q-A9.a (β+δ))

```
POST /internal/rider/assign
Headers: X-Internal: true
Body:
  {
    "orderId": 1,
    "riderNo": null,                        // null/0 = 라이더 풀(WAITING_ASSIGN), 명시 = 강제 배차(admin 시연 호환)
    "storeNo": 1,
    "storeName": "string",
    "storeAddress": "string",
    "storeLat": 35.123,
    "storeLng": 128.456,
    "storePhone": "053-111-2222",
    "deliveryAddress": "string",
    "deliveryLat": 35.130,
    "deliveryLng": 128.460,
    "customerPhone": "010-1234-5678",       // 평문 (D7-a)
    "baseFee": 4000,
    "extraFee": 1500
  }
Response 200:
  {
    "assigned": true,
    "deliveryNo": "00001ABC",
    "riderNo": 5,
    "assignedAt": "2026-05-05T17:01:23"
  }
Response 4xx:
  - 400 BusinessException — rider not available (EATING/REST/SUSPENDED) — D8-a
  - 404 BusinessException — rider not found
  - 409 BusinessException(HttpStatus.CONFLICT) — 동시 배차 충돌 (낙관적 락) — D5 (R3-a 정정 일관, R1-A `RiderService.java:57` 정착 패턴, mmg-common `GlobalExceptionHandler.java:26-31` `e.getStatus()` 동적 매핑)
```

검증:
- rider.status == ACTIVE (D8-a, ADR-008)
- delivery 신규 생성 (status=ASSIGNED, ADR-004 화이트리스트)
- delivery_log INSERT (from=null, to=ASSIGNED)
- 응답 후 별도 트랜잭션에서 Main에 상태 동기화 (Feign Rider→Main, ADR-003)

### 1.2 위치 조회 (Main → Rider)

```
GET /internal/rider/{riderNo}/location
Headers: X-Internal: true
Response 200:
  {
    "riderNo": 5,
    "lat": 35.125,
    "lng": 128.456,
    "updatedAt": "2026-05-05T17:01:23"
  }
Response 404 — TTL 만료 또는 위치 송신 0회
```

내부 흐름: Redis GET `rider:loc:{riderNo}` (ADR-005, ADR-006)

### 1.3 라이더 상태 확인 (Main/Admin → Rider)

```
GET /internal/rider/{riderNo}/status
Headers: X-Internal: true
Response 200:
  {
    "riderNo": 5,
    "status": "ACTIVE",
    "currentDeliveryNo": "00001ABC"  // null 가능 (배달 진행 중 아니면)
  }
```

---

## 2. MainInternalClient (Rider → Main)

### 2.1 배달 상태 변경 알림 (Rider → Main)

```
PUT /internal/order/{orderId}/delivery-status
Headers: X-Internal: true
Body:
  {
    "deliveryStatus": "PICKED_UP",   // 7개 enum (ADR-004)
    "riderNo": 5,
    "changedAt": "2026-05-05T17:05:00"
  }
Response 200:
  {
    "orderId": 1,
    "previousDeliveryState": 1,
    "newDeliveryState": 2            // ADR-004 매핑 (정정 3)
  }
Response 4xx:
  - 400 BusinessException — invalid mapping
  - 404 — order not found
  - 409 — orders 동시 변경 충돌
```

내부 흐름: Main이 delivery.status → orders.delivery_state 매핑 후 UPDATE (ADR-004 정정 3). 같은 트랜잭션에서 SSE/STOMP push (필요 시).

### 2.2 배달 완료 처리 (Rider → Main)

```
POST /internal/order/{orderId}/complete
Headers: X-Internal: true
Body:
  {
    "deliveryNo": "00001ABC",
    "riderNo": 5,
    "deliveredMethod": "DIRECT",       // DIRECT / CUSTOMER_REQUEST / CUSTOMER_ABSENT (정정 10)
    "deliveredPhotoUrl": null,         // 사진 URL (선택, 정정 10)
    "completedAt": "2026-05-05T17:15:00"
  }
Response 200:
  {
    "orderId": 1,
    "deliveryState": 3
  }
```

내부 흐름:
- Main이 orders.delivery_state=3, orders.order_state=6 UPDATE
- 친환경 적립 트리거 (REQ-INT-007 별건, Phase 5)
- SSE/STOMP push 손님/사장 (필요 시)

### 2.3 사진 업로드 (Rider → Main, multipart)

```
POST /internal/files/upload?category=delivery
Headers: X-Internal: true
Body: multipart/form-data
  - file: (binary, jpg/png/gif/webp ≤ 10MB, OwnerService.uploadImage 패턴)
Response 200:
  {
    "url": "/uploads/delivery/abc-uuid.jpg"
  }
Response 4xx:
  - 400 — content-type/size 검증 위반
```

> CLAUDE.md §5 — 사진은 main-service 단독 책임. rider-service는 multipart 받아 Feign으로 전달.

---

## 3. AdminRiderClient (Admin → Rider)

### 3.1 라이더 승인 (Admin → Rider)

```
POST /internal/rider/{riderNo}/approve
Headers: X-Internal: true
Body:
  {
    "approvedByAdminNo": 1
  }
Response 200:
  {
    "riderNo": 5,
    "status": "ACTIVE",
    "approvedAt": "2026-05-05T10:00:00"
  }
Response 4xx:
  - 400 — rider 이미 ACTIVE
  - 404 — rider not found
```

내부 흐름: rider.status PENDING → ACTIVE (Q2-B, ADR-001)

### 3.2 라이더 제재 (Admin → Rider)

```
POST /internal/rider/{riderNo}/suspend
Headers: X-Internal: true
Body:
  {
    "suspendedByAdminNo": 1,
    "reason": "string",
    "untilAt": "2026-06-05T00:00:00"  // null = 영구
  }
Response 200:
  {
    "riderNo": 5,
    "status": "SUSPENDED"
  }
```

### 3.3 정산 confirm (Admin → Rider)

```
POST /internal/settlement/{settlementId}/confirm
Headers: X-Internal: true
Body:
  {
    "confirmedByAdminNo": 1
  }
Response 200:
  {
    "settlementId": 42,
    "status": "CONFIRMED",
    "confirmedAt": "2026-05-12T10:00:00"
  }
Response 4xx:
  - 400 — settlement 이미 CONFIRMED
  - 404 — settlement not found
```

내부 흐름: settlement.status PENDING → CONFIRMED (D10-b, ADR-007)

### 3.5 라이더 목록 조회 (Admin → Rider) — Q-A1 (라++) Group 8 신설 (2026-05-17)

```
GET /internal/rider/list?status=PENDING
Headers: X-Internal: true
Query:
  - status: nullable, 4값 enum (PENDING/ACTIVE/EATING/SUSPENDED) — null이면 전체
Response 200:
  [
    {
      "riderNo": 5,
      "userNo": 42,
      "status": "PENDING",
      "licenseNo": "11-22-...",
      "licenseType": "2종보통",
      "vehicleType": "MOTORBIKE",
      "accountBank": "신한",
      "accountNo": "110-1",
      "accountHolder": "홍길동"
    }
  ]
```

내부 흐름: rider 측 `RiderRepository.findByStatusOrderByRiderNoDesc(status)` 또는 `findAll()` → `RiderProfileRes.from` 매핑.

**박제 결정 사유** (Q-A1 (라++)):
- 학원 발표 시연 가치 — admin 라이더 관리 화면에 *rider.status* 표시 필수
- auth `getUserList(role=RIDER)`는 user.status만, rider 도메인 status 부재 → 시연 가치 ↓
- case-#33 후행 박제 누락 회피 학습 적용

**분류 B 자율 정정 (Page → List, case-#36 변종)**:
- 사용자 박제 시그니처: `Page<RiderProfileRes>` (Spring Data Page)
- 본인 정정: `List<RiderProfileRes>` — Spring Data Page Jackson 기본 역직렬화 X, Feign 호환성 위험 회피
- MVP 학원 발표 = List 충분. page/size query 제거, status 필터만 유지

---

### 3.4 정산 집계 (Admin → Rider)

```
GET /internal/settlement/calculate?periodStart=2026-05-04&periodEnd=2026-05-10
Headers: X-Internal: true
Response 200:
  {
    "createdSettlements": 5,
    "settlementIds": [42, 43, 44, 45, 46]
  }
```

내부 흐름: 해당 기간 DELIVERED 배달 집계 → settlement INSERT (status=PENDING). admin이 검토 후 별도 confirm.

---

## 4. NoticeClient (Admin → Rider)

### 4.1 공지 작성 (Admin → Rider)

```
POST /internal/notice
Headers: X-Internal: true
Body:
  {
    "category": "IMPORTANT",         // IMPORTANT / SAFETY / GENERAL
    "title": "string",
    "content": "string",
    "publishedAt": "2026-05-05T09:00:00",
    "senderAdminNo": 1
  }
Response 200:
  {
    "noticeNo": 42
  }
```

### 4.2 공지 수정 / 삭제

```
PUT /internal/notice/{noticeNo}
Body: { category, title, content, publishedAt }
Response 200

DELETE /internal/notice/{noticeNo}
Response 204
```

### 4.3 라이더 측 공지 조회 (외부 endpoint, ROLE_RIDER)

```
GET /api/rider/notice?category=&page=0&size=20
Headers: Cookie: ACCESS_TOKEN=...
Response 200:
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
        "publishedAt": "2026-05-05T09:00:00"
      }
    ]
  }
```

조회 조건: `published_at <= NOW()` (ADR-009 가시성 제어)

---

## 5. AuthInternalClient (Rider/Admin → Auth) — 기존

> Phase 4-A InternalUserController 그대로 활용. 라이더 정리에서 변경 0.

```
GET /internal/users?ids=1,2,3
GET /internal/users/{userNo}
```

라이더 측 활용:
- 배달 상세 화면에서 손님 이름 조회 (현재는 customer_phone snapshot으로 충분, 이름 별도 필요 시)
- 라이더 측 활용 별건 (Phase 5-R6 검토)

---

## 6. 외부 endpoint 명세 (라이더 측, 참조용)

> Internal과 구분. 인증 ROLE_RIDER. Gateway `/api/rider/**` → 8082 (Phase 4-B 그대로).

### 6.1 라이더 프로필

```
PUT /api/rider/profile (가입 직후 추가정보 등록, ADR-001 Q1-C)
Body: { licenseNo, licenseType, vehicleType, accountBank, accountNo, accountHolder }
Response 200: { riderNo, status="PENDING" }

GET /api/rider/me
Response 200: rider 프로필 (자기 자신만)
```

### 6.2 배달 처리 (ADR-004)

```
GET  /api/rider/order/waiting              — WAITING_ASSIGN 목록 (가용 라이더에 배차 대기 중)
PUT  /api/rider/order/{deliveryNo}/accept  — ASSIGNED → ARRIVED_AT_STORE
PUT  /api/rider/order/{deliveryNo}/reject  — ASSIGNED → WAITING_ASSIGN (재할당)
GET  /api/rider/order/inprogress           — ARRIVED_AT_STORE / AWAITING_PICKUP / PICKED_UP / DELIVERING
PUT  /api/rider/order/{deliveryNo}/arrive  — ARRIVED_AT_STORE → AWAITING_PICKUP
PUT  /api/rider/order/{deliveryNo}/pickup  — AWAITING_PICKUP → PICKED_UP
PUT  /api/rider/order/{deliveryNo}/depart  — PICKED_UP → DELIVERING (선택, MVP는 PICKED_UP→DELIVERING 자동)
PUT  /api/rider/order/{deliveryNo}/complete — DELIVERING → DELIVERED
   Body: { deliveredMethod, deliveredPhotoUrl }
```

### 6.3 위치 / 상태 (ADR-005, ADR-008)

```
PUT  /api/rider/location           — 위치 송신 (5s/10s 간격, D6)
PUT  /api/rider/status             — ACTIVE↔EATING 토글 (D8)
POST /api/rider/work-session/end   — 업무 종료 (D9, work_session.ended_at)
GET  /api/rider/work-session/today — 오늘 세션 (작업 시간 / 휴게 시간)
GET  /api/rider/work-session/summary?period=week
```

### 6.4 정산 (ADR-007)

```
GET /api/rider/settlement?startDate=&endDate=
GET /api/rider/settlement/account                — 계좌 조회
PUT /api/rider/settlement/account                — 계좌 변경
```

### 6.5 공지 (ADR-009)

```
GET /api/rider/notice?category=&page=
```

### 6.6 CS

```
GET /api/rider/cs/contact   — 고객센터 연락처 (정적)
```

---

## 7. R4 화면 기반 후보 명세 (Phase 5-R4 진입 시 결정)

> **상태**: Candidate (2026-05-10)
> **출처**: 관리자 측 라이더 관제 / 라이더 공지 발송 화면 박제. 코드 0, 명세만.
> **본인 결정 자리**: R4 진입 시 통신 패턴 (Feign 동기 vs Redis Pub/Sub vs Kafka), §4.1과의 통합/정정 여부.

### 7.1 GET /internal/rider/monitor (Admin → Rider, 후보)

```
GET /internal/rider/monitor?status={status}&page={page}
Headers: X-Internal: true
Query:
  - status: ADR-004 7-state 매핑 후보 (WAITING_ASSIGN / ASSIGNED / DELIVERING / DELIVERED 등, R4 진입 시 정정)
  - page: 0-based
Response 200:
  {
    "summary": {
      "waiting": 0,
      "assigned": 0,
      "delivering": 0,
      "completed": 0
    },
    "deliveries": []
  }
```

의도: 관리자 배달관제 화면이 실시간 관제 요약 + 배달 목록 조회.

### 7.2 POST /internal/rider/notice (Admin → Rider, 후보)

```
POST /internal/rider/notice
Headers: X-Internal: true
Body:
  {
    "title": "string",
    "targetType": "ALL",
    "content": "string",
    "sendType": "NOW",
    "reservedAt": "2026-04-16T10:00:00"   // sendType=RESERVED 시 사용
  }
Response 200:
  {
    "resultMessage": "공지 발송 완료",
    "resultData": null
  }
Response 400:
  - reservedAt 과거값 (후보)
  - sendType=RESERVED인데 reservedAt=null (후보)
  - targetType 화이트리스트 위반 (후보)
```

의도: 관리자 작성 라이더 전체 공지를 Rider 서비스로 전달, 즉시/예약 발송.

### 7.3 R4 진입 시 결정 보류 항목

- **통신 패턴 (본인 결정 자리)**:
  - 옵션 A: Feign 동기 (단순, MVP)
  - 옵션 B: Redis Pub/Sub (Phase 6 Outbox 시점 통합)
  - 옵션 C: Kafka (Phase 6+ 검토)
- **인증 방식**: §3/§4 X-Internal 패턴 준수 vs 강화 (Phase 6+ mTLS 일관)
- **동기 vs 비동기 fanout**: 7.2 즉시/예약 발송 라이더 전체 push 채널 (SSE / STOMP / FCM 후보)
- **400 케이스 확정**: 7.2 reservedAt 과거값 / sendType=RESERVED인데 reservedAt=null / 7.1 status 화이트리스트 / page 음수
- **§4.1 vs §7.2 통합/정정** (R3-c 종결 시점 갱신, 2026-05-10 — 시점 분리 박제 일관):
  - **데이터 모델 = R2-e (이미 종결)** / **입구 = R4 진입 시 결정**
  - §4.1 path `/internal/notice` + category(IMPORTANT/SAFETY/GENERAL) + publishedAt 기반 (DB 영속, ADR-009 R2-e)
  - §7.2 path `/internal/rider/notice` + targetType(ALL) + sendType(NOW/RESERVED) + reservedAt 기반 (fanout)
  - **현 `notice` 테이블 컬럼** (R2-e 종결 박제, `rider-schema.sql:138-149`): notice_no / category / title / content / published_at / sender_admin_no
  - **§7.2 요구 필드**: targetType / sendType / reservedAt **부재**
  - R4 진입 시 본인 결정 (옵션 3개):
    - **(a)** notice 테이블 컬럼 추가 (target_type / send_type / reserved_at, 스키마 마이그레이션 + Q-DB (가) 학원 DB ALTER TABLE 적용)
    - **(b)** §7.2 명세 정정 (R2-e 컬럼만 추종, 화면 박제 의도 vs 실 명세 충돌 시 본인 결정)
    - **(c)** 신설 별도 테이블 (notice_dispatch 등 발송 이력 분리, R2-e notice + R4 dispatch 분리 모델)
  - Code 분석 추천 = R4 진입 시점에 결정 효과 분석 강제 절차(`feedback_decision_load_balance.md`) 작동 후 보고

### 7.4 R2-b 인덱스 설계 영향

7.1 monitor가 status별 카운트 + 최근 배달 목록 조회. R2-b delivery_log 설계 시 다음 후보 검토:
- `(delivery_id, created_at)` 복합 인덱스 — 특정 배달의 이력 시계열 조회
- `(created_at)` 단일 인덱스 — 전체 status 변경 흐름 시계열
- monitor가 delivery_log를 직접 조회 vs delivery 테이블만 조회 — R2-b 진단 시 결정

---

## 권한 검증 요약

| 패턴 | 적용 위치 |
|---|---|
| Objects.equals (rider 본인 검증) | 6.2~6.4 자기 배달/세션/정산만 |
| dto.userNo 위조 방지 | 6.1 PUT /profile 등 dto에 callerUserNo 신뢰 X |
| ROLE_RIDER | /api/rider/** 전체 |
| ROLE_ADMIN | admin-service 측 모든 endpoint |
| X-Internal 헤더 | /internal/** Phase 6+ 강화 (현재 Gateway 차단으로 외부 노출 0) |

---

## Feign timeout 설정 (Phase 4-A 패턴)

```yaml
# 모든 Feign client 적용
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 3000
            read-timeout: 5000
```

---

## 에러 응답 형식 (mmg-common ResultResponse)

```json
{
  "resultCode": "...",
  "resultMessage": "...",
  "resultData": null
}
```

| HTTP | resultCode | 시점 |
|---|---|---|
| 200 | SUCCESS | 정상 |
| 400 | BAD_REQUEST | 검증 위반 (BusinessException) |
| 401 | UNAUTHORIZED | 인증 실패 |
| 403 | FORBIDDEN | 권한 부족 / dto 위조 |
| 404 | NOT_FOUND | 리소스 부재 |
| 409 | CONFLICT | 낙관적 락 충돌 (D5-a) |
| 500 | INTERNAL_ERROR | 시스템 (D1 throw — Redis/DB 다운 등) |

---

## 미해결 / Phase 5-R1~R9에서 결정

- X-Internal 헤더 검증 강화 (Phase 6+ mTLS 또는 service-to-service token)
- WebSocket Handshake 인증 패턴 (Phase 5-R5)
- multipart 사진 업로드 시 메모리 vs 디스크 임시 저장 (Spring Boot 기본 multipart resolver)
- 사진 업로드 실패 시 배달 완료 분리 (best-effort vs throw)

---

## 관련 메모리

- `project_phase4a_backfill_state.md` — InternalUserController 패턴 + Feign timeout
- `project_phase4b_backfill_state.md` — Internal Gateway 차단 패턴
- `feedback_dto_userno_forgery.md` — 명시 403 throw
- `feedback_owner_check_pattern.md` — Objects.equals
- `feedback_feign_null_domain_split.md` — null 가드 + critical 분기
