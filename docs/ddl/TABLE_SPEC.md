# 전체 테이블 명세서 (live DB 기준)

> **스냅샷 일자**: 2026-05-31
> **출처**: 학원 MariaDB `112.222.157.157:5012`의 `information_schema` 실 조회 + 코드 @Entity 교차 검증
> **서버 버전**: MariaDB 11.8.6-ubu2404
> **연결 계정**: `green2` / DB pw는 `.env`
> **collation**: `utf8mb4_unicode_ci` (대부분) — main 일부 신규 테이블은 `utf8mb4_uca1400_ai_ci` (MariaDB 11.x 신규 default)

## 0. 요약

| Schema | 테이블 수 | 소속 서비스 | ddl-auto |
|---|---|---|---|
| `my_mmg_auth` | 1 | auth-service | validate |
| `my_mmg_main` | 27 | main-service | validate |
| `my_mmg_rider` | 6 | rider-service | validate |
| `my_mmg_admin` | 8 | admin-service | **update** ⚠️ |

**총 42개 테이블, 약 418개 컬럼.**

⚠️ `my_mmg_admin`은 `ddl-auto: update`라 DDL 파일 없이 JPA 엔티티로부터 자동 생성됨. Phase 5 admin 작업의 부산물.

### 외부 schema 논리 FK 매트릭스 (물리 FK 없음, 애플리케이션 레벨 정합성)

| From | → | To | 비고 |
|---|---|---|---|
| `main.address.user_no` | → | `auth.user.user_no` | 회원 주소 |
| `main.cart.user_no` | → | `auth.user.user_no` | |
| `main.likedstore.user_no` | → | `auth.user.user_no` | |
| `main.orders.user_no` | → | `auth.user.user_no` | |
| `main.store.owner_id` | → | `auth.user.user_no` | OWNER role |
| `main.review_reply.owner_id` | → | `auth.user.user_no` | OWNER role |
| `main.notification.user_no` | → | `auth.user.user_no` | |
| `main.chat_sessions.user_no` | → | `auth.user.user_no` | |
| `main.pets.user_no` | → | `auth.user.user_no` | UNIQUE (1:1) |
| `main.owner_profile.user_no` | → | `auth.user.user_no` | UNIQUE (1:1, OWNER) |
| `main.attendance_log.user_no` | → | `auth.user.user_no` | |
| `main.coupon_reward_code.user_no` | → | `auth.user.user_no` | |
| `main.green_point_log.user_no` | → | `auth.user.user_no` | |
| `main.couponlist.user_no` | → | `auth.user.user_no` | |
| `rider.rider.user_no` | → | `auth.user.user_no` | UNIQUE (1:1, RIDER) |
| `rider.delivery.order_id` | → | `main.orders.order_id` | |
| `rider.delivery_log.actor_user_no` | → | `auth.user.user_no` | NULL = SYSTEM |
| `rider.notice.sender_admin_no` | → | `admin.???` | admin 테이블 미존재 (TODO §6) |
| `rider.settlement.confirmed_by_admin_no` | → | `admin.???` | 동일 |
| `admin.report.reporter_no` / `target_no` | → | `auth.user.user_no` | target_type에 따라 |
| `admin.blind.user_no` / `review_no` | → | `auth.user.user_no` / `main.review.review_id` | |
| `admin.chatbot_inquiry.user_no` / `order_no` | → | `auth.user.user_no` / `main.orders.order_id` | |
| `admin.settlement.target_no` | → | target_type=STORE → `main.store.store_id`, RIDER → `rider.rider.rider_no` | |

**물리 FK는 같은 schema 내부에만 존재** (총 16개, §6 참조).

---

## 1. `my_mmg_auth` (auth-service)

### 1.1 `user` — 회원

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | user_no | bigint AI | NO | PK | — | 회원번호 |
| 2 | user_id | varchar(20) | YES | UNI | NULL | 로그인 ID (대소문자 무시) |
| 3 | user_pw | varchar(1000) | YES | | NULL | BCrypt 해시 |
| 4 | role | enum(CUSTOMER,OWNER,RIDER,ADMIN) | YES | | 'CUSTOMER' | 역할 |
| 5 | name | varchar(10) | YES | MUL | NULL | 이름 |
| 6 | birth | date | YES | | NULL | 생년월일 |
| 7 | gender | int | YES | | NULL | 1:남 2:여 |
| 8 | green | int | YES | | 0 | 친환경 점수 |
| 9 | kind | int | YES | | 0 | 주문 빈도 |
| 10 | rank | enum(BRONZE,SILVER,GOLD,VIP,VVIP) | YES | | 'BRONZE' | 등급 (예약어 → \`rank\`) |
| 11 | tel | varchar(20) | YES | | NULL | 핸드폰번호 |
| 12 | email | varchar(100) | YES | UNI | NULL | 이메일 |
| 13 | created_at | datetime | NO | | CURRENT_TIMESTAMP | 가입일 |
| 14 | status | enum(PENDING,ACTIVE,REJECTED,SUSPENDED,WITHDRAWN) | NO | | 'ACTIVE' | 회원 상태 |
| 15 | business_no | varchar(10) | YES | | NULL | 사업자등록번호 (OWNER) |
| 16 | process_memo | varchar(255) | YES | | NULL | 관리자 처리 메모 |
| 17 | withdrawn_at | datetime | YES | | NULL | 탈퇴 시각 |
| 18 | suspension_until | datetime | YES | | NULL | 정지 종료일 |

- **PK**: user_no
- **UQ**: uq_user_id(user_id), uq_user_email(email)
- **IDX**: idx_user_find_auth(name, tel, email)
- **물리 FK**: 없음

> ⚠️ 메모리 상 `address` / `terms` / `user_agreements` / `policies` / `policy_agreements`가 auth에 있다고 했지만, **실 DB에는 `user` 1개만 존재**. address는 `my_mmg_main`으로 이전됨 (Phase 1-B-3.5 정정). 약관 계열은 `my_mmg_admin.policy`로 통합됨.

---

## 2. `my_mmg_main` (main-service)

### 2.1 `address` — 회원 배달 주소

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | address_id | bigint AI | NO | PK | — | 주소번호 |
| 2 | user_no | bigint | NO | MUL | — | 논리FK auth.user |
| 3 | default_ad | int | YES | | 0 | 기본주소 여부 (1=기본) |
| 4 | address | varchar(100) | YES | | NULL | 기본주소 |
| 5 | address_detail | varchar(200) | YES | | NULL | 상세주소 |
| 6 | latitude | decimal(17,2) | YES | | NULL | 위도 |
| 7 | longitude | decimal(17,2) | YES | | NULL | 경도 |

- IDX: idx_address_user_no(user_no)

> ⚠️ DDL 파일 `auth-schema.sql`은 `address`를 auth에 두지만, 실 DB는 main에 있음. 컬럼명도 다름 (`lat/lng` → `latitude/longitude`, `decimal(16,13)` → `decimal(17,2)`). DDL 파일이 stale.

### 2.2 `attendance_log` — 출석 체크

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | attendance_id | bigint AI | NO | PK | — | |
| 2 | user_no | bigint | NO | MUL | — | 논리FK auth.user |
| 3 | attendance_date | date | NO | | — | 출석일 |
| 4 | created_at | datetime | YES | | CURRENT_TIMESTAMP | |

- UQ: uk_user_date(user_no, attendance_date) — 1일 1회

### 2.3 `cart` / `cart_detail` — 장바구니

**cart**

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | cart_id | bigint AI | NO | PK | — | |
| 2 | user_no | bigint | NO | MUL | — | 논리FK auth.user |
| 3 | store_id | bigint | NO | MUL→store | — | |

- 물리 FK: cart_ibfk_2(store_id) → store

**cart_detail**

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | cart_item_id | bigint AI | NO | PK | — | |
| 2 | cart_id | bigint | NO | MUL→cart | — | |
| 3 | menu_id | bigint | NO | MUL→menu | — | |
| 4 | quantity | int | NO | | 1 | 수량 |

- 물리 FK: cart_id→cart, menu_id→menu (둘 다 CASCADE)

### 2.4 `category` / `store_category` — 카테고리 (마스터)

**category**

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | category_id | bigint AI | NO | PK | — | |
| 2 | category_name | varchar(20) | NO | | — | 카테고리명 |

**store_category** (M:N)

| # | 컬럼 | 타입 | NULL | Key | 코멘트 |
|---|---|---|---|---|---|
| 1 | store_id | bigint | NO | PK→store | |
| 2 | category_id | bigint | NO | PK→category | |

- 복합 PK (store_id, category_id)
- 물리 FK 둘 다 CASCADE

### 2.5 `chat_sessions` / `chat_messages` — 챗봇

**chat_sessions**

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | session_id | bigint AI | NO | PK | — | 세션 PK |
| 2 | user_no | bigint | NO | MUL | — | 논리FK auth.user |
| 3 | pet_no | bigint | YES | | NULL | MYPET 진입 시, 논리FK pets |
| 4 | entry_point | enum(MYPET,CS) | NO | | — | 진입점 |
| 5 | tone_mode | enum(PLAYFUL,GOURMET,EMPATHY,SERIOUS) | NO | | 'PLAYFUL' | 톤 |
| 6 | status | enum(ACTIVE,ESCALATED,CLOSED) | NO | MUL | 'ACTIVE' | ESCALATED=상담원 연결 대기 |
| 7 | escalated_at | datetime(6) | YES | | NULL | |
| 8 | closed_at | datetime(6) | YES | | NULL | |
| 9-10 | created_at / updated_at | datetime(6) | NO | | — | |

- IDX: user_no, status

**chat_messages**

| # | 컬럼 | 타입 | NULL | Key | 코멘트 |
|---|---|---|---|---|---|
| 1 | message_id | bigint AI | NO | PK | |
| 2 | session_id | bigint | NO | MUL→chat_sessions | |
| 3 | role | enum(USER,ASSISTANT,SYSTEM) | NO | | 발화 주체 |
| 4 | content | text | NO | | 본문 |
| 5-6 | created_at / updated_at | datetime(6) | NO | | |

- 물리 FK: fk_chat_messages_session(session_id) → chat_sessions CASCADE

### 2.6 `coupon` — 쿠폰 마스터

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | coupon_id | bigint AI | NO | PK | — | |
| 2 | name | varchar(50) | YES | | NULL | 쿠폰명 |
| 3 | discount_type | varchar(10) | YES | | NULL | FIXED/PERCENT |
| 4 | discount_value | int | YES | | NULL | 할인값 |
| 5 | min_discount_amount | int | YES | | NULL | 최소 할인금액 |
| 6 | max_discount_amount | int | YES | | NULL | 최대 할인금액 |
| 7 | total_count | int | YES | | NULL | 총 발급 수량 |
| 8 | remaining_count | int | YES | | NULL | 남은 수량 (선착순용) |
| 9 | issue_start_date | date | YES | | NULL | 발급 시작 |
| 10 | issue_end_date | date | YES | | NULL | 발급 종료 |
| 11 | validity_days | int | YES | | NULL | 발급 후 N일 유효 |
| 12 | issue_type | varchar(20) | YES | | NULL | FCFS/EVENT/WELCOME/ECO_LEVEL |
| 13 | is_active | tinyint(1) | YES | | NULL | 활성 여부 |
| 14 | description | varchar(200) | YES | | NULL | |
| 15 | created_at | datetime | YES | | CURRENT_TIMESTAMP | |

### 2.7 `couponlist` — 회원 보유 쿠폰

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | couponlist_id | bigint AI | NO | PK | — | |
| 2 | user_no | bigint | NO | MUL | — | 논리FK auth.user |
| 3 | coupon_id | bigint | NO | MUL→coupon | — | |
| 4 | is_used | tinyint(1) | YES | | 0 | |
| 5 | used_at | datetime | YES | | NULL | |
| 6 | order_id | bigint | YES | MUL | NULL | 사용 주문 (논리FK orders) |
| 7 | issued_at | datetime | YES | | CURRENT_TIMESTAMP | |
| 8 | expires_at | datetime | YES | | NULL | |

- 물리 FK: fk_couponlist_coupon → coupon
- IDX: user_no+is_used+expires_at (보유 쿠폰 조회), coupon_id, order_id

### 2.8 `coupon_reward_code` — 이벤트 보상 코드 (친환경 레벨 등)

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | reward_code_id | bigint AI | NO | PK | — | |
| 2 | user_no | bigint | NO | MUL | — | 논리FK auth.user |
| 3 | event_code | varchar(50) | NO | | — | 이벤트 식별 |
| 4 | reward_stage | int | NO | | — | 보상 단계 |
| 5 | coupon_id | bigint | NO | | — | 발급 대상 쿠폰 (논리FK coupon) |
| 6 | code | varchar(30) | NO | UNI | — | 발급 코드 |
| 7 | issue_date | date | NO | | — | |
| 8 | expires_at | datetime | NO | | — | |
| 9 | is_used | tinyint(1) | NO | | 0 | |
| 10 | used_at | datetime | YES | | NULL | |
| 11 | couponlist_id | bigint | YES | MUL | NULL | 사용 시 couponlist 매핑 |
| 12 | created_at | datetime | NO | | CURRENT_TIMESTAMP | |

- UQ: code, (user_no, event_code, issue_date)
- IDX: couponlist_id, (user_no, code)

### 2.9 `green_point_log` — 친환경 포인트 이력 (Outbox 패턴)

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | green_point_log_id | bigint AI | NO | PK | — | |
| 2 | order_id | bigint | NO | UNI | — | 1주문 1지급 (논리FK orders) |
| 3 | user_no | bigint | NO | | — | 논리FK auth.user |
| 4 | point | int | NO | | — | 지급/차감 포인트 |
| 5 | reason | varchar(100) | NO | | — | 사유 |
| 6 | status | varchar(20) | NO | | 'PENDING' | PENDING/SUCCESS/FAILED |
| 7 | retry_count | int | NO | | 0 | |
| 8 | last_error | varchar(500) | YES | | NULL | |
| 9-10 | created_at / updated_at | datetime | NO | | CURRENT_TIMESTAMP | |

- UQ: uk_green_point_log_order_id(order_id) — 멱등성 보장

### 2.10 `likedstore` — 가게 좋아요

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | user_no | bigint | NO | PK | — | 논리FK auth.user |
| 2 | store_id | bigint | NO | PK→store | — | |
| 3 | created_at | datetime | NO | | CURRENT_TIMESTAMP | |

- 복합 PK
- 물리 FK: likedstore_ibfk_2(store_id) → store CASCADE

### 2.11 `menu` / `menu_category` — 가게 메뉴

**menu_category** (가게별 메뉴 카테고리)

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | category_id | bigint AI | NO | PK | — | |
| 2 | store_id | bigint | NO | MUL→store | — | |
| 3 | category | varchar(15) | NO | | — | 카테고리명 |
| 4 | category_order | int | YES | | 1 | 노출 순서 |

**menu**

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | menu_id | bigint AI | NO | PK | — | |
| 2 | category_id | bigint | YES | MUL→menu_category | NULL | |
| 3 | name | varchar(30) | YES | | NULL | 메뉴명 |
| 4 | price | int | YES | | 0 | |
| 5 | menu_pic | varchar(1000) | YES | | NULL | 이미지 경로 |
| 6 | soldout | int | YES | | 0 | 1=품절 |
| 7 | menu_info | varchar(300) | YES | | NULL | 메뉴 설명 |
| 8 | menu_order | int | NO | | 1 | 노출 순서 |

### 2.12 `menu_option` / `menu_option_category` — 메뉴 옵션

**menu_option_category**

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | option_category_no | bigint AI | NO | PK | — | |
| 2 | is_required | bit(1) | NO | | — | 필수 여부 |
| 3 | max_select | int | NO | | — | 최대 선택 개수 |
| 4 | menu_id | bigint | NO | | — | 논리FK menu |
| 5 | option_category_name | varchar(40) | NO | | — | 그룹명 |

**menu_option**

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | option_id | bigint AI | NO | PK | — | |
| 2 | name | varchar(100) | NO | | — | 옵션명 |
| 3 | option_category_no | bigint | NO | | — | 논리FK menu_option_category |
| 4 | price | int | NO | | — | 추가 금액 |
| 5 | sold_out | varchar(20) | NO | | — | 품절 여부 |

### 2.13 `notification` — 사이트 내 알림

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | notification_id | bigint AI | NO | PK | — | |
| 2 | user_no | bigint | NO | MUL | — | 논리FK auth.user |
| 3 | notification_type | varchar(50) | NO | | — | 유형 |
| 4 | title | varchar(100) | NO | | — | 제목 |
| 5 | content | varchar(500) | NO | | — | 본문 |
| 6 | target_url | varchar(255) | YES | | NULL | 클릭 시 이동 경로 |
| 7 | is_read | tinyint(1) | NO | | 0 | |
| 8 | created_at | datetime | NO | | CURRENT_TIMESTAMP | |
| 9 | read_at | datetime | YES | | NULL | |

- IDX: (user_no, is_read, created_at), (user_no, created_at)

### 2.14 `orders` — 주문

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | order_id | bigint AI | NO | PK | — | |
| 2 | user_no | bigint | NO | MUL | — | 논리FK auth.user |
| 3 | store_id | bigint | NO | MUL→store | — | |
| 4 | order_time | datetime | YES | | CURRENT_TIMESTAMP | |
| 5 | request | varchar(255) | YES | | NULL | 가게 요청사항 |
| 6 | rider_request | varchar(255) | YES | | NULL | 라이더 요청사항 |
| 7 | address | varchar(255) | YES | | NULL | 배달 주소 snapshot |
| 8 | address_detail | varchar(255) | YES | | NULL | |
| 9 | delivery_fee | int | YES | | 1500 | 배달비 |
| 10 | amount | int | YES | | NULL | 총 결제금액 |
| 11 | delivery_state | int | YES | | 1 | 1배달전 2픽업완료 3배달완료 |
| 12 | pay_state | int | YES | | 1 | 1결제전 2결제완료 3결제환불 |
| 13 | order_state | int | YES | | 1 | 1주문수락전 2주문취소 3조리중 4배차중 5배차완료 6배달완료 |
| 14 | eco_selected | tinyint(1) | NO | | 0 | 친환경 선택 여부 |
| 15 | delivered_photo_url | varchar(255) | YES | | NULL | 배달 완료 사진 URL |

- 물리 FK: orders_ibfk_2(store_id) → store ON DELETE NO ACTION
- IDX: orders_ibfk_1(user_no), orders_ibfk_2(store_id), (store_id, order_time), (user_no, order_time), (store_id, pay_state, order_state, order_time)
- ⚠️ AUTO_INCREMENT가 비정상적으로 큰 값(`391775460588724`)으로 출발했던 흔적은 DDL 파일에 남아있지만, 현재 실 row는 정상 채번. 정정 흔적.

### 2.15 `order_detail` — 주문 상세

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | detail_id | bigint AI | NO | PK | — | |
| 2 | order_id | **bigint(100)** | NO | MUL→orders | — | ⚠️ 표시 폭 100 — 의미 없음 (bigint 동일), 정정 가능 |
| 3 | menu_id | bigint | NO | MUL→menu | — | |
| 4 | quantity | int | NO | | 1 | |
| 5 | menu_name | varchar(30) | YES | | NULL | 주문 당시 메뉴명 snapshot |
| 6 | menu_price | int | YES | | 0 | 주문 당시 가격 snapshot |

- 물리 FK: order_id → orders CASCADE

### 2.16 `order_status_log` — 주문 상태 이력

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | log_no | bigint AI | NO | PK | — | |
| 2 | order_id | bigint | NO | MUL→orders | — | |
| 3 | before_state | int | YES | | NULL | |
| 4 | after_state | int | YES | | NULL | |
| 5 | changed_by_type | varchar(20) | NO | | — | USER/OWNER/RIDER/SYSTEM |
| 6 | changed_by_no | bigint | YES | | NULL | 변경 주체 ID |
| 7 | memo | varchar(255) | YES | | NULL | |
| 8 | changed_at | datetime | YES | | CURRENT_TIMESTAMP | |

- 물리 FK: fk_order_status_log_order → orders

### 2.17 `owner_profile` — 사장 프로필 (사업자/정산)

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | owner_profile_id | bigint AI | NO | PK | — | |
| 2 | user_no | bigint | NO | UNI | — | 1:1, 논리FK auth.user (OWNER) |
| 3 | store_address | varchar(255) | NO | | — | 사업장 주소 |
| 4 | business_number | varchar(30) | NO | | — | 사업자등록번호 |
| 5 | business_license_url | varchar(500) | NO | | — | 영업신고증 URL |
| 6 | mail_order_license_url | varchar(500) | NO | | — | 통신판매업 신고증 URL |
| 7 | bank_name | varchar(50) | NO | | — | 정산 은행 |
| 8 | account_number | varchar(50) | NO | | — | 정산 계좌 |
| 9 | account_holder | varchar(50) | NO | | — | 예금주 |
| 10-11 | created_at / updated_at | datetime | NO | | CURRENT_TIMESTAMP | |

### 2.18 `payment` — 결제

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | payment_id | bigint AI | NO | PK | — | |
| 2 | order_id | bigint | NO | MUL→orders | — | |
| 3 | payment_key | varchar(200) | NO | | — | 토스 결제키 |
| 4 | amount | int | NO | | — | 결제금액 |
| 5 | pay_state | int | NO | | 1 | 1카드 2카카오 3네이버 4만나서 |
| 6 | payment_time | datetime | NO | | CURRENT_TIMESTAMP | |

- 물리 FK: payment_ibfk_1 → orders CASCADE

### 2.19 `pets` — 펫 (회원 1:1)

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | pet_no | bigint AI | NO | PK | — | |
| 2 | user_no | bigint | NO | UNI | — | 1:1, 논리FK auth.user |
| 3 | species | enum(DOG,CAT,RABBIT,HAMSTER,BEAR,FOX,PANDA,FROG) | NO | | 'DOG' | |
| 4 | name | varchar(50) | NO | | — | 펫 이름 (가입 시 자동 지정) |
| 5 | level | int | NO | | 1 | 1~ |
| 6 | exp | int | NO | | 0 | 경험치 |
| 7 | intimacy | int | NO | | 0 | 친밀도 |
| 8-9 | created_at / updated_at | datetime(6) | NO | | — | |

### 2.20 `review` / `review_reply` — 리뷰

**review**

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | review_id | bigint AI | NO | PK | — | |
| 2 | order_id | bigint | NO | UNI→orders | — | 주문당 1 리뷰 |
| 3 | rating | int | YES | | 5 | 1~5 |
| 4 | contents | varchar(1000) | YES | | NULL | |
| 5 | photo | varchar(1000) | YES | | NULL | |
| 6 | written_at | datetime | YES | | CURRENT_TIMESTAMP | |
| 7 | amended_at | datetime | YES | | NULL | |
| 8 | blinded | tinyint(1) | NO | | 0 | 블라인드 여부 |
| 9 | blinded_at | datetime | YES | | NULL | |
| 10 | blind_source | varchar(20) | YES | | NULL | AUTO/MANUAL |
| 11 | blind_reason | varchar(200) | YES | | NULL | |
| 12 | blind_report_id | bigint | YES | | NULL | 논리FK admin.report |

- 물리 FK: FK_review_orders → orders CASCADE

**review_reply** — 사장 답글

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | reply_id | bigint AI | NO | PK | — | |
| 2 | review_id | bigint | NO | MUL→review | — | |
| 3 | owner_id | bigint | NO | MUL | — | 논리FK auth.user (OWNER) |
| 4 | content | varchar(1000) | YES | | NULL | |
| 5 | written_at | datetime | YES | | CURRENT_TIMESTAMP | |

- 물리 FK: review_reply_ibfk_1 → review CASCADE

### 2.21 `store` — 가게

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | store_id | bigint AI | NO | PK | — | |
| 2 | owner_id | bigint | NO | MUL | — | 논리FK auth.user (OWNER) |
| 3 | store_name | varchar(30) | NO | | — | 가게명 |
| 4 | business_hours | varchar(30) | YES | | NULL | 운영시간 |
| 5 | min_price | int | YES | | 0 | 최소주문금액 |
| 6 | holiday | varchar(30) | YES | | NULL | 휴무일 |
| 7 | state | int | YES | | 0 | 0종료 1오픈 |
| 8-9 | location / detail_location | varchar(100) | YES | | NULL | 주소/상세 |
| 10-11 | latitude / longitude | decimal(16,13) | YES | | NULL | |
| 12-13 | created_at / updated_at | date | YES | | CURDATE() | |
| 14 | notice | varchar(300) | YES | | NULL | 공지 |
| 15 | business_number | varchar(30) | YES | | NULL | 사업자등록번호 |
| 16 | business_name | varchar(30) | YES | | NULL | |
| 17 | store_tel | varchar(20) | YES | | NULL | |
| 18 | store_pic | varchar(1000) | YES | | NULL | 가게 사진 |
| 19 | store_info | varchar(300) | YES | | NULL | 소개글 |
| 20-22 | rating_avg / rating_count / order_count | int | YES | | NULL | 집계 |
| 23 | bank_name | varchar(30) | YES | | NULL | 정산 은행 |
| 24 | account_number | varchar(50) | YES | | NULL | 정산 계좌 |
| 25 | account_holder | varchar(30) | YES | | NULL | 예금주 |
| 26 | business_license_url | varchar(500) | YES | | NULL | 영업신고증 이미지 |
| 27 | mail_order_license_url | varchar(500) | YES | | NULL | 통신판매업 신고증 이미지 |

- IDX: owner_id

> ⚠️ `owner_profile`과 `store`에 사업자번호/정산정보가 중복. 향후 정리 후보 (tech-debt).

---

## 3. `my_mmg_rider` (rider-service)

### 3.1 `rider` — 라이더 프로필

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | rider_no | bigint AI | NO | PK | — | |
| 2 | user_no | bigint | NO | UNI | — | 1:1, 논리FK auth.user (RIDER) |
| 3 | license_no | varchar(50) | NO | | — | 운전면허 번호 |
| 4 | license_type | varchar(20) | NO | | — | 1종/2종/원동기 |
| 5 | vehicle_type | varchar(20) | NO | | — | WALK/BICYCLE/MOTORBIKE/CAR |
| 6 | status | varchar(20) | NO | | 'PENDING' | PENDING/ACTIVE/EATING/SUSPENDED |
| 7 | account_bank | varchar(50) | YES | | NULL | 정산 은행 |
| 8 | account_no | varchar(50) | YES | | NULL | 정산 계좌 |
| 9 | account_holder | varchar(50) | YES | | NULL | 예금주 |
| 10 | phone | varchar(20) | YES | | NULL | 가입 시점 snapshot |
| 11-12 | created_at / updated_at | datetime | NO | | CURRENT_TIMESTAMP | |
| 13 | license_image_url | varchar(500) | YES | | NULL | 운전면허증 사진 URL |

### 3.2 `delivery` — 배달 단건

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | delivery_no | **varchar(20)** | NO | PK | — | 형식 00001ABC, application 생성 |
| 2 | order_id | bigint | NO | MUL | — | 논리FK main.orders |
| 3 | rider_no | bigint | YES | MUL | NULL | NULL=WAITING_ASSIGN, 논리FK rider |
| 4 | status | varchar(30) | NO | MUL | — | 7개 enum (ADR-004) |
| 5 | pickup_phone | varchar(20) | YES | | NULL | 가게 전화 snapshot |
| 6 | customer_phone | varchar(20) | YES | | NULL | 손님 전화 snapshot |
| 7 | store_name | varchar(200) | YES | | NULL | 가게명 snapshot |
| 8-10 | pickup_address / pickup_lat / pickup_lng | varchar/decimal | YES | | NULL | 가게 좌표 snapshot |
| 11-13 | delivery_address / delivery_lat / delivery_lng | 동일 | YES | | NULL | 배달 좌표 snapshot |
| 14 | base_fee | int | NO | | — | 기본 배달료 |
| 15 | extra_fee | int | NO | | 0 | 추가 배달료 |
| 16 | delivered_method | varchar(30) | YES | | NULL | DIRECT/CUSTOMER_REQUEST/CUSTOMER_ABSENT |
| 17 | delivered_photo_url | varchar(500) | YES | | NULL | main /uploads/delivery/ |
| 18-22 | assigned_at / arrived_at_store_at / picked_at / delivering_at / delivered_at | datetime | YES | | NULL | 상태 전환 시각 |
| 23 | version | bigint | NO | | 0 | @Version 낙관적 락 |
| 24-25 | created_at / updated_at | datetime | NO | | CURRENT_TIMESTAMP | |
| 26 | order_request | varchar(500) | YES | | NULL | 가게 요청사항 snapshot |
| 27 | rider_request | varchar(500) | YES | | NULL | 라이더 요청사항 snapshot |

- IDX: order_id, rider_no, status

### 3.3 `delivery_log` — 배달 상태 이력

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | log_no | bigint AI | NO | PK | — | |
| 2 | delivery_no | varchar(20) | NO | MUL | — | 논리FK delivery |
| 3 | from_status | varchar(30) | YES | | NULL | 최초 INSERT 시 NULL |
| 4 | to_status | varchar(30) | NO | | — | |
| 5 | actor_role | varchar(20) | NO | | — | RIDER/SYSTEM/ADMIN |
| 6 | actor_user_no | bigint | YES | | NULL | SYSTEM 시 NULL |
| 7 | reason | varchar(20) | YES | | NULL | cancel 시 ACCIDENT/PERSONAL/OTHER |
| 8 | changed_at | datetime | NO | | CURRENT_TIMESTAMP | |

### 3.4 `notice` — 라이더 공지

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | notice_no | bigint AI | NO | PK | — | |
| 2 | category | varchar(20) | NO | | — | IMPORTANT/SAFETY/GENERAL |
| 3 | title | varchar(200) | NO | | — | |
| 4 | content | text | NO | | — | |
| 5 | target_type | varchar(20) | NO | | 'ALL' | ALL/RIDER/SPECIFIC |
| 6 | send_type | varchar(20) | NO | | 'NOW' | NOW/RESERVED |
| 7 | reserved_at | datetime | YES | | NULL | 예약 발송 |
| 8 | published_at | datetime | NO | MUL | — | 노출 기준 시각 |
| 9 | sender_admin_no | bigint | NO | | — | 논리FK admin (테이블 X — 주의) |
| 10-11 | created_at / updated_at | datetime | NO | | CURRENT_TIMESTAMP | |

### 3.5 `work_session` — 라이더 근무 세션

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | session_no | bigint AI | NO | PK | — | |
| 2 | rider_no | bigint | NO | MUL | — | 논리FK rider |
| 3 | vehicle_type | varchar(20) | NO | | — | 세션 시작 시점 snapshot |
| 4 | started_at | datetime | NO | | — | |
| 5 | ended_at | datetime | YES | | NULL | NULL = 진행 중 |
| 6 | work_seconds | int | NO | | 0 | 누적 배달 시간 |
| 7 | break_seconds | int | NO | | 0 | 누적 휴게 시간 (EATING 합산) |
| 8-9 | created_at / updated_at | datetime | NO | | CURRENT_TIMESTAMP | |

### 3.6 `settlement` — 라이더 정산

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | settlement_no | bigint AI | NO | PK | — | |
| 2 | rider_no | bigint | NO | MUL | — | 논리FK rider |
| 3-4 | period_start / period_end | date | NO | | — | 월요일/일요일 |
| 5 | delivery_count | int | NO | | — | 배달 건수 |
| 6 | total_distance_m | int | NO | | 0 | 총 이동 거리 |
| 7-8 | total_base_fee / total_extra_fee | int | NO | | — | 합계 |
| 9 | commission | int | NO | | — | 수수료 |
| 10 | tax | int | NO | | — | 3.3% |
| 11 | insurance | int | NO | | — | 보험료 |
| 12 | payout | int | NO | | — | 실 수령액 |
| 13 | status | varchar(20) | NO | | 'PENDING' | PENDING/CONFIRMED |
| 14 | confirmed_by_admin_no | bigint | YES | | NULL | 논리FK admin |
| 15 | confirmed_at | datetime | YES | | NULL | |
| 16 | paid_at | datetime | YES | | NULL | NULL = 미입금 |
| 17-18 | created_at / updated_at | datetime | NO | | CURRENT_TIMESTAMP | |

- UQ: (rider_no, period_start, period_end)

---

## 4. `my_mmg_admin` (admin-service)

> ⚠️ `ddl-auto: update`로 JPA가 자동 생성. DDL 파일 없음. 컬럼 순서/타입 약간 임시 (예: enum이 varchar로 강제되지 않고 그대로 들어감).
> **주의**: `admin` 테이블 자체가 없음 → 관리자 인증은 `auth.user.role = ADMIN`으로 식별하는 구조. notice/settlement의 `sender_admin_no` 등 admin FK는 모두 `auth.user.user_no`로 해석해야 함.

### 4.1 `ai_operation_metrics` — AI 호출 메트릭

| # | 컬럼 | 타입 | NULL | Key | 코멘트 |
|---|---|---|---|---|---|
| 1 | id | bigint AI | NO | PK | |
| 2 | operation_type | varchar(50) | NO | MUL | |
| 3 | target_ref | varchar(100) | YES | | 관련 대상 ID |
| 4 | model | varchar(50) | YES | | gemini-2.0-flash 등 |
| 5-6 | input_tokens / output_tokens | int | YES | | |
| 7 | duration_ms | int | YES | | |
| 8 | success | tinyint(1) | NO | | |
| 9 | error_message | varchar(500) | YES | | |
| 10 | created_at | datetime | NO | | |

- IDX: idx_type_created(operation_type, created_at)

### 4.2 `blind` — 리뷰 블라인드

| # | 컬럼 | 타입 | NULL | Key | 코멘트 |
|---|---|---|---|---|---|
| 1 | blind_id | bigint AI | NO | PK | |
| 2 | created_at | datetime(6) | NO | | |
| 3 | duration_days | int | NO | | |
| 4 | ends_at | datetime(6) | YES | | |
| 5 | reason | enum(ADVERTISEMENT,ETC,FALSE_FACT,PROFANITY) | NO | | |
| 6 | review_no | bigint | NO | | 논리FK main.review.review_id |
| 7 | start_at | datetime(6) | NO | | |
| 8 | status | enum(BLINDED,PERMANENT,RELEASED,REVIEWING,SUSPENDED) | NO | | |
| 9 | user_no | bigint | NO | | 작성자, 논리FK auth.user |
| 10 | content | varchar(255) | YES | | 리뷰 본문 snapshot |
| 11 | rating | double | YES | | snapshot |
| 12 | store_name | varchar(255) | YES | | snapshot |
| 13 | writer | varchar(255) | YES | | snapshot |
| 14 | extra_description | varchar(500) | YES | | 관리자 메모 |

### 4.3 `chatbot_inquiry` — 챗봇 → 상담원 인계 문의

| # | 컬럼 | 타입 | NULL | Key | 코멘트 |
|---|---|---|---|---|---|
| 1 | inquiry_id | bigint AI | NO | PK | |
| 2-3 | created_at / updated_at | datetime(6) | YES | | |
| 4 | answer | varchar(200) | YES | | 관리자 답변 |
| 5 | answered_at | datetime(6) | YES | | |
| 6 | category | enum(CUSTOMER,OWNER,RIDER) | YES | | |
| 7 | content | varchar(500) | YES | | 문의 본문 |
| 8 | minimum_content | varchar(500) | YES | | 요약 |
| 9 | state | enum(PENDING,PROCESSING,RESOLVED) | YES | | |
| 10 | user_no | bigint | NO | | 논리FK auth.user |
| 11 | inquiry_code | varchar(20) | YES | | ABC-YYYYMMDD-001 |
| 12 | order_no | bigint | YES | | 관련 주문번호 (논리FK main.orders) |

### 4.4 `faq` — FAQ

| # | 컬럼 | 타입 | NULL | Key | Default | 코멘트 |
|---|---|---|---|---|---|---|
| 1 | faq_id | bigint AI | NO | PK | — | |
| 2-3 | created_at / updated_at | datetime(6) | YES | | NULL | |
| 4 | answer | varchar(200) | NO | | — | |
| 5 | question | varchar(300) | NO | | — | |
| 6 | type | varchar(20) | YES | | NULL | 카테고리 |
| 7 | is_active | tinyint(1) | NO | | 1 | |

### 4.5 `notice` (admin) — 관리자 발송 공지

| # | 컬럼 | 타입 | NULL | Key | 코멘트 |
|---|---|---|---|---|---|
| 1 | notice_id | bigint AI | NO | PK | |
| 2-3 | created_at / updated_at | datetime(6) | YES | | |
| 4 | content | varchar(300) | NO | | |
| 5 | region_filter | varchar(20) | YES | | 지역 필터 |
| 6 | send_at | datetime(6) | YES | | |
| 7 | send_type | enum(NOW,RESERVE) | YES | | |
| 8 | target | varchar(20) | YES | | |
| 9 | title | varchar(100) | NO | | |

> ⚠️ rider 측 `notice`와 동명. 용도 다름 (rider notice = 라이더 대상 공지, admin notice = 통합 발송).

### 4.6 `policy` — 약관/정책

| # | 컬럼 | 타입 | NULL | Key | 코멘트 |
|---|---|---|---|---|---|
| 1 | policy_id | bigint AI | NO | PK | |
| 2-3 | created_at / updated_at | datetime(6) | YES | | |
| 4 | content | varchar(1000) | NO | | 본문 |
| 5 | is_active | bit(1) | NO | | |
| 6 | title | varchar(100) | NO | | |
| 7 | type | varchar(20) | NO | | 이용약관/개인정보처리방침 등 |
| 8 | version | int | YES | | |

### 4.7 `report` — 신고

| # | 컬럼 | 타입 | NULL | Key | 코멘트 |
|---|---|---|---|---|---|
| 1 | report_id | bigint AI | NO | PK | |
| 2 | admin_memo | varchar(500) | YES | | |
| 3 | content | varchar(500) | YES | | 신고 사유 |
| 4 | created_at | datetime(6) | NO | | |
| 5 | processed_at | datetime(6) | YES | | |
| 6 | reason | varchar(50) | NO | | |
| 7 | reporter_no | bigint | NO | | 논리FK auth.user |
| 8 | status | varchar(20) | NO | | PENDING/PROCESSING/RESOLVED |
| 9 | target_no | bigint | NO | | target_type별 ID |
| 10 | target_type | varchar(20) | NO | | REVIEW/USER/STORE 등 |
| 11 | ai_confidence | varchar(10) | YES | | LOW/MEDIUM/HIGH |
| 12 | ai_fail_reason | varchar(500) | YES | | |
| 13 | ai_processed_at | datetime(6) | YES | | |
| 14 | ai_reason | varchar(100) | YES | | AI 판단 사유 |
| 15 | ai_retry_count | int | NO | | |
| 16 | ai_should_blind | bit(1) | YES | | AI 추천 |
| 17 | ai_status | enum(DONE,FAILED,PENDING,PROCESSING) | NO | | |
| 18 | ai_violations | varchar(300) | YES | | 위반 카테고리 |
| 19 | auto_blinded | bit(1) | NO | | 자동 블라인드 수행 여부 |
| 20 | auto_blinded_at | datetime(6) | YES | | |
| 21 | blind_fail_reason | varchar(500) | YES | | |
| 22 | review_content | text | YES | | 신고 시점 리뷰 snapshot |

### 4.8 `settlement` (admin) — 통합 정산

| # | 컬럼 | 타입 | NULL | Key | 코멘트 |
|---|---|---|---|---|---|
| 1 | settlement_id | bigint AI | NO | PK | |
| 2 | bank_account | varchar(50) | YES | | snapshot |
| 3 | created_at | datetime(6) | NO | | |
| 4 | fee_amount | int | YES | | 수수료 |
| 5 | gross_amount | int | YES | | 총액 |
| 6 | item_count | int | YES | | |
| 7 | net_amount | int | YES | | 실지급액 |
| 8 | other_deduction | int | YES | | 기타 공제 |
| 9 | paid_at | datetime(6) | YES | | |
| 10-11 | period_end / period_start | date | NO | | |
| 12 | status | enum(COMPLETED,DONE,HELD,PENDING) | NO | | |
| 13 | target_no | bigint | NO | | target_type별 ID |
| 14 | target_type | enum(ALL,RIDER,STORE) | NO | | |
| 15 | tax_amount | int | YES | | |
| 16 | toss_payout_id | varchar(100) | YES | | 토스 페이아웃 ID |

> ⚠️ rider 측 `settlement`와 별개. 이쪽은 store + rider 통합용. 데이터 일관성 정리 필요 (tech-debt).

---

## 5. 물리 FK 전체 목록 (16건, 모두 schema 내부)

| Schema | From | Column | → | To |
|---|---|---|---|---|
| main | cart | store_id | → | store |
| main | cart_detail | cart_id | → | cart |
| main | cart_detail | menu_id | → | menu |
| main | chat_messages | session_id | → | chat_sessions |
| main | couponlist | coupon_id | → | coupon |
| main | likedstore | store_id | → | store |
| main | menu | category_id | → | menu_category |
| main | menu_category | store_id | → | store |
| main | orders | store_id | → | store |
| main | order_detail | order_id | → | orders |
| main | order_status_log | order_id | → | orders |
| main | payment | order_id | → | orders |
| main | review | order_id | → | orders |
| main | review_reply | review_id | → | review |
| main | store_category | store_id | → | store |
| main | store_category | category_id | → | category |

**auth / rider / admin schema 내부에는 물리 FK 없음.** auth는 표가 1개라 자체 FK 불필요, rider/admin은 코드 작성 시 의도적으로 안 건 듯.

---

## 6. 실 DB와 DDL 파일 차이 / 알려진 부채

1. **`address` 위치**: DDL의 `auth-schema.sql`은 auth에 두지만 실 DB는 main에 있음. 컬럼명/타입도 다름 (위경도 `decimal(17,2)`로 잘림 — 정밀도 손실 가능). DDL 파일 갱신 필요.
2. **`my_mmg_auth` 단일 테이블**: 약관/동의 계열 테이블 없음. 메모리에 있던 `terms`, `user_agreements`, `policies`, `policy_agreements`는 미구현 또는 `admin.policy`로 통합.
3. **admin 테이블 자체가 없음**: 관리자 계정은 `auth.user.role = ADMIN`. notice/settlement의 `sender_admin_no` / `confirmed_by_admin_no`도 결국 `auth.user.user_no` 참조. 컬럼명만 admin이고 실제는 user.
4. **`order_detail.order_id`** 타입이 `bigint(100)` — 표시 폭만 다르고 의미 없음. 다른 `bigint(20)`과 통일 후보.
5. **DDL 파일 `main-schema.sql` 누락 테이블**: `attendance_log`, `coupon`, `couponlist`, `coupon_reward_code`, `green_point_log`, `menu_option`, `menu_option_category`, `order_status_log`, `owner_profile`. 모두 Phase 5 신규 도메인이라 `main-schema.sql`에 반영 안 된 상태. 마이그레이션 ALTER도 별 파일로 분산.
6. **`store` ↔ `owner_profile` 중복**: 사업자번호/정산정보 양쪽에. 정리 후보.
7. **rider/admin `settlement` 이원화**: rider 정산은 rider DB, store/rider 통합 정산은 admin DB. 책임 분리 의도인지 부채인지 확인 필요.
8. **collation 혼재**: 대부분 `utf8mb4_unicode_ci`, main 일부 신규 테이블만 `utf8mb4_uca1400_ai_ci` (MariaDB 11.x 기본). 비교/조인 시 문제 가능. 정규화 후보.
9. **`orders.AUTO_INCREMENT`** 정정 흔적 (`391775460588724`) — DDL 파일에만 남고 실 DB는 정상 채번.

---

## 7. 코드 ↔ DB 검증 (모든 @Entity 매핑 OK)

42개 테이블 중 @Entity가 매핑된 것:

- auth: `user`
- main: `address`, `attendance_log`, `cart`, `cart_detail`, `chat_messages`, `chat_sessions`, `coupon`, `couponlist`, `green_point_log`, `likedstore`, `menu_option`, `menu_option_category`, `notification`, `orders`, `order_detail`, `order_status_log`, `owner_profile`, `payment`, `pets`, `review`, `review_reply`
- rider: `delivery`, `delivery_log`, `notice`, `rider`, `settlement`, `work_session`
- admin: `ai_operation_metrics`, `blind`, `chatbot_inquiry`, `faq`, `notice`, `policy`, `report`, `settlement`

@Entity 없이 MyBatis만 쓰는 것 (의도적):

- main: `category`, `menu`, `menu_category`, `store`, `store_category`, `coupon_reward_code`

(Phase 3-D에서 Store/Owner MyBatis 유지 확정 — 메모리 §MOMOOLGGO_MSA progress 참조)

---

**끝.** 향후 스키마 변경 시 이 문서를 같이 갱신할 것. 학원 DB 상태가 source of truth.
