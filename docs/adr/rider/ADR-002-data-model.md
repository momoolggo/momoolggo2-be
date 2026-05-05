# ADR-002: 라이더 데이터 모델 + DB 분리

> **상태**: Accepted (2026-05-05)
> **관련 결정**: Q3 (account 분리), Figma 정정 1·4·5·6·9·10·11
> **관련 Figma**: `../../figma/` 모든 화면 (figma-analysis.md ADR-002 매핑)

---

## 상황 (Context)

CLAUDE.md §3에 명시:
- 라이더 도메인은 `my_mmg_rider` 스키마에 분리
- 예시 테이블: rider, delivery, rider_location, delivery_logs
- MSA 경계 외부 참조는 논리 FK만 (물리 FK 제약 금지)

진단 시 (코드 검증 결과):
- `main-schema.sql`에는 rider/delivery 테이블 0건. orders 테이블에만 rider 관련 컬럼 (`rider_request`, `delivery_state`)
- 따라서 모놀리식에서 가져올 게 없음. mmg_rider 스키마 신설 필요

Figma 분석 후 정정:
- 정정 1: rider 엔티티 필드 추가 (license_type, vehicle_type, account_holder)
- 정정 4: 배달료 분리 (base_fee + extra_fee)
- 정정 5: settlement 도메인 신설
- 정정 6: work_session 도메인 신설
- 정정 8: notice 도메인 신설
- 정정 9: delivery_no ≠ order_id (별도 PK)
- 정정 10: delivered_method 사유 분류 + delivered_photo_url
- 정정 11: 손님 전화번호 평문 (D7-a)

---

## 옵션 (Options)

### DB 분리 방식

- **A. mmg_rider 스키마 신설 (CLAUDE.md 권장)** — 도메인 격리, MSA 정석
- B. mmg_main 스키마 공유 (rider/delivery 테이블 추가) — 단일 DB 유지, JOIN 자유, 그러나 MSA 경계 위반

→ **A 채택** (CLAUDE.md §3 그대로)

### 엔티티 구성

- A. rider + delivery 단순 2개 (기존 진단)
- **B. 6개 분리 (Figma 정정 반영)**: rider / delivery / delivery_log / work_session / settlement / notice
- C. 더 세분화 (rider_account, rider_location 별도 테이블 등) — 과설계

→ **B 채택** (정정 5·6·8 반영)

### account 분리 (Q3)

- **A. rider 테이블에 account_bank/account_no/account_holder 합침** — MVP 단순
- B. rider_account 별도 테이블 — 변경 이력 + 정산 감사

→ **A 채택** (Q3-A) — 변경 이력은 Phase 6+ audit 컬럼

### rider_location 위치

- A. mmg_rider.rider_location 테이블 (DB 영속)
- **B. Redis KV (휘발)** — ADR-005 참조
- C. 하이브리드

→ **B 채택** (ADR-005 별도)

---

## 결정 (Decision)

### mmg_rider 스키마 6 테이블

#### 1. `rider` — 라이더 프로필

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `rider_no` | BIGINT AUTO_INCREMENT PK | 라이더 ID |
| `user_no` | BIGINT NOT NULL UNIQUE | 논리 FK → my_mmg_auth.user.user_no |
| `license_no` | VARCHAR(50) NOT NULL | 운전면허 번호 |
| `license_type` | VARCHAR(20) NOT NULL | 운전면허 종류 (1종/2종/원동기 등) — 정정 1 |
| `vehicle_type` | VARCHAR(20) NOT NULL | 배달수단 (WALK/BICYCLE/MOTORBIKE/CAR) — 정정 1 |
| `status` | VARCHAR(20) NOT NULL DEFAULT 'PENDING' | PENDING / ACTIVE / EATING / SUSPENDED |
| `account_bank` | VARCHAR(50) | 정산 은행 |
| `account_no` | VARCHAR(50) | 정산 계좌 |
| `account_holder` | VARCHAR(50) | 계좌주명 — 정정 1 |
| `created_at` | DATETIME DEFAULT CURRENT_TIMESTAMP | |
| `updated_at` | DATETIME ON UPDATE CURRENT_TIMESTAMP | |

> 주의: `status`는 ACTIVE/EATING이 토글 — ADR-008 참조. SUSPENDED는 admin 제재(별건).

#### 2. `delivery` — 배달 단건

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `delivery_no` | VARCHAR(20) PK | 배차번호 (형식 `00001ABC`, application 생성) — 정정 9 |
| `order_id` | VARCHAR(20) NOT NULL | 논리 FK → my_mmg_main.orders.order_id (형식 `000001A`) — 정정 9 |
| `rider_no` | BIGINT | NULL 허용 (WAITING_ASSIGN 상태) — FK to rider |
| `status` | VARCHAR(30) NOT NULL | 7개 상태 enum (ADR-004 참조) |
| `pickup_phone` | VARCHAR(20) | 가게 전화 (Main에서 snapshot) — 정정 11 평문 |
| `customer_phone` | VARCHAR(20) | 손님 전화 (Main에서 snapshot) — 정정 11 평문 (D7-a) |
| `pickup_address` | VARCHAR(200) | 가게 주소 snapshot |
| `pickup_lat` | DECIMAL(16,13) | 가게 위경도 |
| `pickup_lng` | DECIMAL(16,13) | |
| `delivery_address` | VARCHAR(200) | 배달 주소 snapshot |
| `delivery_lat` | DECIMAL(16,13) | |
| `delivery_lng` | DECIMAL(16,13) | |
| `base_fee` | INT NOT NULL | 기본 배달료 — 정정 4 |
| `extra_fee` | INT NOT NULL DEFAULT 0 | 추가 배달료 — 정정 4 |
| `delivered_method` | VARCHAR(30) | DIRECT / CUSTOMER_REQUEST / CUSTOMER_ABSENT — 정정 10 |
| `delivered_photo_url` | VARCHAR(500) | 사진 URL (선택, main-service `/uploads/delivery/`) — 정정 10 |
| `assigned_at` | DATETIME | |
| `arrived_at_store_at` | DATETIME | |
| `picked_at` | DATETIME | |
| `delivering_at` | DATETIME | |
| `delivered_at` | DATETIME | |
| `version` | BIGINT | `@Version` (낙관적 락 — ADR-004 Q5-A) |
| `created_at` | DATETIME DEFAULT CURRENT_TIMESTAMP | |
| `updated_at` | DATETIME ON UPDATE CURRENT_TIMESTAMP | |

> 주의: 사진은 main-service 단독 책임 (CLAUDE.md §5). rider-service는 multipart 받아 `/internal/files/upload?category=delivery` Feign 호출 → URL만 저장.

#### 3. `delivery_log` — 상태 이력

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `log_no` | BIGINT AUTO_INCREMENT PK | |
| `delivery_no` | VARCHAR(20) NOT NULL | FK to delivery |
| `from_status` | VARCHAR(30) | NULL 허용 (최초 INSERT 시) |
| `to_status` | VARCHAR(30) NOT NULL | |
| `actor_role` | VARCHAR(20) NOT NULL | RIDER / SYSTEM / ADMIN |
| `actor_user_no` | BIGINT | |
| `changed_at` | DATETIME DEFAULT CURRENT_TIMESTAMP | |

> 주의: 상태 변경 시 application 레벨에서 자동 INSERT (DeliveryService).

#### 4. `work_session` — 근무 세션 (정정 6)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `session_no` | BIGINT AUTO_INCREMENT PK | |
| `rider_no` | BIGINT NOT NULL | FK to rider |
| `vehicle_type` | VARCHAR(20) NOT NULL | snapshot at session start |
| `started_at` | DATETIME NOT NULL | |
| `ended_at` | DATETIME | NULL = 진행 중 (D9) |
| `work_seconds` | INT DEFAULT 0 | 누적 배달 시간 (초) |
| `break_seconds` | INT DEFAULT 0 | 누적 휴게 시간 (초) — EATING 상태 합산 |

> 주의: ended_at 기록 시점 = "업무 종료" 버튼 (D9-a). 로그인 세션은 별개 (signout 호출 무관).

#### 5. `settlement` — 정산 트랜잭션 (정정 5)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `settlement_no` | BIGINT AUTO_INCREMENT PK | |
| `rider_no` | BIGINT NOT NULL | FK to rider |
| `period_start` | DATE NOT NULL | 정산 기간 시작 (월요일) |
| `period_end` | DATE NOT NULL | 정산 기간 종료 (일요일) |
| `delivery_count` | INT NOT NULL | 배달 건수 |
| `total_distance_m` | INT NOT NULL DEFAULT 0 | 총 이동 거리 (m) — Phase 5-R7 산출 방식 별도 |
| `total_base_fee` | INT NOT NULL | base_fee 합계 |
| `total_extra_fee` | INT NOT NULL | extra_fee 합계 |
| `commission` | INT NOT NULL | 수수료 |
| `tax` | INT NOT NULL | 세금 (3.3%) |
| `insurance` | INT NOT NULL | 보험료 |
| `payout` | INT NOT NULL | 실 수령액 |
| `status` | VARCHAR(20) NOT NULL DEFAULT 'PENDING' | PENDING / CONFIRMED — D10-b |
| `confirmed_by_admin_no` | BIGINT | admin이 confirm 시 기록 |
| `confirmed_at` | DATETIME | |
| `paid_at` | DATETIME | NULL = 미입금. 다음 주 월요일 입금 |
| `created_at` | DATETIME DEFAULT CURRENT_TIMESTAMP | |

> 주의: 주간 집계 트리거는 admin 수동 (D10-b). Phase 6+ 자동 배치.

#### 6. `notice` — 공지사항 (정정 8, ADR-009 별건)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `notice_no` | BIGINT AUTO_INCREMENT PK | |
| `category` | VARCHAR(20) NOT NULL | IMPORTANT / SAFETY / GENERAL |
| `title` | VARCHAR(200) NOT NULL | |
| `content` | TEXT NOT NULL | |
| `published_at` | DATETIME NOT NULL | 발송 시점 (즉시/예약) |
| `sender_admin_no` | BIGINT NOT NULL | admin user_no (논리 FK → my_mmg_admin.admin) |
| `created_at` | DATETIME DEFAULT CURRENT_TIMESTAMP | |

> 주의: admin → rider broadcast 단방향. 라이더는 GET만.

---

## 외부 참조 (논리 FK)

| 컬럼 | 참조 대상 | 정합성 보장 |
|---|---|---|
| `rider.user_no` | my_mmg_auth.user.user_no | 가입 흐름 (ADR-001) — auth user 생성 후 rider 프로필 INSERT |
| `delivery.order_id` | my_mmg_main.orders.order_id | 배차 요청 시 main이 Feign 전달 |
| `notice.sender_admin_no` | my_mmg_admin.admin.admin_no | admin-service Phase 5 진행 동기화 (ADR-009) |
| `settlement.confirmed_by_admin_no` | my_mmg_admin.admin.admin_no | 동일 |

물리 FK 제약 X (CLAUDE.md §3). 데이터 정합성은 application/Feign으로 보장.

---

## 응답 동결 (CLAUDE.md §6 규칙 7)

- `orders.delivery_state` (1/2/3)는 main-service 응답에서 그대로 노출 (프론트 영향 0)
- delivery.status 7개 → orders.delivery_state 매핑 (ADR-004 정정 3 참조)

---

## JPA 매핑 주의사항

- `delivery.delivery_no` — VARCHAR PK + application 생성 (`Persistable<String>` 패턴, Phase 3-C Order 사례)
- `pickup_lat`/`pickup_lng`/`delivery_lat`/`delivery_lng` — DECIMAL(16,13) ↔ Java Double, `@JdbcTypeCode(SqlTypes.NUMERIC)` 명시 (Phase 3-D UserAddress 사례)
- `delivery.version` — `@Version` Long
- BaseEntity (created_at/updated_at) — Phase 3-C 검증 패턴 그대로 사용 가능. Review처럼 컬럼명 다른 경우 `@AttributeOverride`

---

## 트레이드오프

| 항목 | 채택 결과 | 미래 고려 사항 |
|---|---|---|
| 6 테이블 분리 | 도메인 명확 | 통합 테스트 fixture 부담 — Phase 4-A 패턴 그대로 SecurityContextHolder + INSERT |
| 평문 전화번호 | UX 단순 (Figma 그대로) | Phase 6+ 마스킹/프록시 (D7-a 결과로 tech-debt 등재) |
| account 단일 테이블 | MVP 단순 | Phase 6+ rider_account 분리 + audit (Q3-A 결과로 tech-debt 등재) |
| 사진 main-service 위임 | CLAUDE.md §5 일관 | rider-service multipart 전달 Feign 비용 — 학원 발표 환경 무방 |
| 논리 FK | MSA 정석 | 데이터 부정합 발생 시 application 레벨 보상 — ADR-003 Feign critical 분기 |

---

## 미해결 / Phase 5에서 결정

- 정산 산출 공식 (수수료율/세율/보험료) — Phase 5-R7
- 이동 거리 산출 방식 — pickup_lat/lng → delivery_lat/lng 직선 거리? 실제 경로? Phase 5-R7
- delivery_no 생성 알고리즘 (`00001ABC` 형식 그대로 vs UUID) — Phase 5-R3

---

## 관련 메모리

- `feedback_no_assumption_on_sql.md` — DDL 신설 시 사용자 검토 후 진행
- `feedback_verify_diagnostic_assumptions.md` — DDL 부재 검증 (정정 #8)
- `feedback_dead_config_avoidance.md` — rider_account 분리 보류 근거
