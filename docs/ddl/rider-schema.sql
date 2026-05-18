-- =============================================================
-- my_mmg_rider schema DDL (Phase 5-R1)
-- ADR-002 데이터 모델 — R1 범위는 rider 테이블만.
-- delivery / delivery_log / work_session / settlement / notice는 R2~R9에서 추가.
--
-- 학원 공유 DB 사전 조건:
--   1. CREATE SCHEMA my_mmg_rider; (학원 DBA 권한 필요)
--   2. green2 user에 my_mmg_rider 권한 부여
--   본 DDL은 USE my_mmg_rider; 부터 시작 (스키마 자체는 사전 생성 가정)
-- =============================================================

USE my_mmg_rider;

-- 1) rider 테이블 (라이더 프로필)
-- 외부 참조: rider.user_no → my_mmg_auth.user.user_no (논리 FK, 물리 FK 제약 X — CLAUDE.md §3 MSA 경계)
CREATE TABLE IF NOT EXISTS `rider` (
  `rider_no`        BIGINT      NOT NULL AUTO_INCREMENT COMMENT '라이더 PK',
  `user_no`         BIGINT      NOT NULL                 COMMENT '논리 FK → my_mmg_auth.user.user_no',
  `license_no`      VARCHAR(50) NOT NULL                 COMMENT '운전면허 번호',
  `license_type`    VARCHAR(20) NOT NULL                 COMMENT '운전면허 종류 (1종/2종/원동기 등) — Figma 정정 1',
  `vehicle_type`    VARCHAR(20) NOT NULL                 COMMENT '배달수단 (WALK/BICYCLE/MOTORBIKE/CAR) — Figma 정정 1',
  `status`          VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / ACTIVE / EATING / SUSPENDED — ADR-008',
  `account_bank`    VARCHAR(50) DEFAULT NULL             COMMENT '정산 은행',
  `account_no`      VARCHAR(50) DEFAULT NULL             COMMENT '정산 계좌',
  `account_holder`  VARCHAR(50) DEFAULT NULL             COMMENT '계좌주명 — Figma 정정 1',
  `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`rider_no`),
  UNIQUE KEY `uq_rider_user_no` (`user_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) delivery 테이블 (배달 단건)
-- 외부 참조: delivery.order_id → my_mmg_main.orders.order_id (논리 FK, 물리 FK 제약 X)
--           delivery.rider_no → rider.rider_no (논리 FK)
-- ADR-002 데이터 모델 + ADR-004 상태 머신 7개 (WAITING_ASSIGN/ASSIGNED/ARRIVED_AT_STORE/AWAITING_PICKUP/PICKED_UP/DELIVERING/DELIVERED)
-- 인덱스 3건: rider_no / order_id / status (Q-R2a2 (나), 2026-05-06)
CREATE TABLE IF NOT EXISTS `delivery` (
  `delivery_no`         VARCHAR(20)    NOT NULL                                COMMENT '배차번호 PK (형식 00001ABC, application 생성 — Figma 정정 9)',
  `order_id`            BIGINT         NOT NULL                                COMMENT '논리 FK → my_mmg_main.orders.order_id (BIGINT AUTO_INCREMENT — case-#34 정정 2026-05-16). Figma 정정 9 "000001A" 표기는 UI zero-pad formatter 별 트랙(Phase 6+ tech-debt)',
  `rider_no`            BIGINT         DEFAULT NULL                            COMMENT '논리 FK → rider.rider_no (NULL = WAITING_ASSIGN)',
  `status`              VARCHAR(30)    NOT NULL                                COMMENT '7개 상태 enum — ADR-004',
  `pickup_phone`        VARCHAR(20)    DEFAULT NULL                            COMMENT '가게 전화 snapshot — Figma 정정 11 평문',
  `customer_phone`      VARCHAR(20)    DEFAULT NULL                            COMMENT '손님 전화 snapshot — D7-a 평문 (정정 11)',
  `pickup_address`      VARCHAR(200)   DEFAULT NULL                            COMMENT '가게 주소 snapshot',
  `pickup_lat`          DECIMAL(16,13) DEFAULT NULL                            COMMENT '가게 위도',
  `pickup_lng`          DECIMAL(16,13) DEFAULT NULL                            COMMENT '가게 경도',
  `delivery_address`    VARCHAR(200)   DEFAULT NULL                            COMMENT '배달 주소 snapshot',
  `delivery_lat`        DECIMAL(16,13) DEFAULT NULL                            COMMENT '배달 위도',
  `delivery_lng`        DECIMAL(16,13) DEFAULT NULL                            COMMENT '배달 경도',
  `base_fee`            INT            NOT NULL                                COMMENT '기본 배달료 — Figma 정정 4',
  `extra_fee`           INT            NOT NULL DEFAULT 0                      COMMENT '추가 배달료 — Figma 정정 4',
  `delivered_method`    VARCHAR(30)    DEFAULT NULL                            COMMENT 'DIRECT/CUSTOMER_REQUEST/CUSTOMER_ABSENT — Figma 정정 10',
  `delivered_photo_url` VARCHAR(500)   DEFAULT NULL                            COMMENT '사진 URL (main-service /uploads/delivery/) — Figma 정정 10',
  `assigned_at`         DATETIME       DEFAULT NULL                            COMMENT '배차 시각',
  `arrived_at_store_at` DATETIME       DEFAULT NULL                            COMMENT '가게 도착 시각',
  `picked_at`           DATETIME       DEFAULT NULL                            COMMENT '픽업 완료 시각',
  `delivering_at`       DATETIME       DEFAULT NULL                            COMMENT '이동 시작 시각',
  `delivered_at`        DATETIME       DEFAULT NULL                            COMMENT '배달 완료 시각',
  `version`             BIGINT         NOT NULL DEFAULT 0                      COMMENT '@Version 낙관적 락 (Q5-A, ADR-004 D5)',
  `created_at`          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`delivery_no`),
  KEY `idx_delivery_rider_no` (`rider_no`),
  KEY `idx_delivery_order_id` (`order_id`),
  KEY `idx_delivery_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3) delivery_log 테이블 (상태 이력)
-- 외부 참조: delivery_log.delivery_no → delivery.delivery_no (논리 FK, 물리 FK 제약 X)
-- ADR-002 line 119-131 + ADR-004 line 116-120 (DeliveryService.updateStatus 같은 트랜잭션 자동 INSERT)
-- BaseEntity 미상속 (이력 본질, INSERT 후 변경 0) — Q-R2b-BaseEntity (a)
-- 인덱스 1건: delivery_no (R2-a Q-R2a2 (나) 자동 적용, 2026-05-06)
CREATE TABLE IF NOT EXISTS `delivery_log` (
  `log_no`         BIGINT      NOT NULL AUTO_INCREMENT             COMMENT '이력 PK',
  `delivery_no`    VARCHAR(20) NOT NULL                            COMMENT '논리 FK → delivery.delivery_no',
  `from_status`    VARCHAR(30) DEFAULT NULL                        COMMENT '이전 상태 (최초 INSERT 시 NULL)',
  `to_status`      VARCHAR(30) NOT NULL                            COMMENT '변경 후 상태 (ADR-004 7 enum)',
  `actor_role`     VARCHAR(20) NOT NULL                            COMMENT 'RIDER / SYSTEM / ADMIN',
  `actor_user_no`  BIGINT      DEFAULT NULL                        COMMENT '변경 주체 user_no (SYSTEM 시 NULL)',
  `reason`         VARCHAR(20) DEFAULT NULL                        COMMENT 'cancel 시 박제 (ACCIDENT/PERSONAL/OTHER, R6-cancel) — 다른 transition NULL',
  `changed_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '변경 시각',
  PRIMARY KEY (`log_no`),
  KEY `idx_delivery_log_delivery_no` (`delivery_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4) work_session 테이블 (근무 세션, ADR-002 정정 6 + ADR-008)
-- 외부 참조: work_session.rider_no → rider.rider_no (논리 FK, 물리 FK 제약 X)
-- BaseEntity 상속 (R2-a Delivery 패턴 일관, UPDATE 다수: work_seconds 누적 / ended_at 기록)
-- 인덱스 1건: rider_no (R3 진입 시 오늘 세션/주간 합계 조회 패턴, Q-R2a2 (나) 자동 적용 — 2026-05-07)
CREATE TABLE IF NOT EXISTS `work_session` (
  `session_no`     BIGINT      NOT NULL AUTO_INCREMENT             COMMENT '근무 세션 PK',
  `rider_no`       BIGINT      NOT NULL                            COMMENT '논리 FK → rider.rider_no',
  `vehicle_type`   VARCHAR(20) NOT NULL                            COMMENT '세션 시작 시점 vehicle snapshot — Figma 정정 6',
  `started_at`     DATETIME    NOT NULL                            COMMENT '세션 시작 시각',
  `ended_at`       DATETIME    DEFAULT NULL                        COMMENT '세션 종료 시각 (NULL = 진행 중) — D9-a',
  `work_seconds`   INT         NOT NULL DEFAULT 0                  COMMENT '누적 배달 시간 (초)',
  `break_seconds`  INT         NOT NULL DEFAULT 0                  COMMENT '누적 휴게 시간 (초) — EATING 합산',
  `created_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`session_no`),
  KEY `idx_work_session_rider_no` (`rider_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5) settlement 테이블 (정산 트랜잭션, ADR-002 정정 5 + ADR-007)
-- 외부 참조: settlement.rider_no → rider.rider_no (논리 FK, 물리 FK 제약 X)
--           settlement.confirmed_by_admin_no → my_mmg_admin.admin (논리 FK, NULLABLE)
-- BaseEntity 상속 (R2-a 패턴 일관, UPDATE 다수: admin confirm + paid_at)
-- 인덱스 1건: rider_no (R7 진입 시 라이더별 정산 조회, Q-R2a2 (나) 자동 적용 — 2026-05-07)
-- 신규 enum: SettlementStatus (PENDING / CONFIRMED) — D10-b admin 수동 confirm
CREATE TABLE IF NOT EXISTS `settlement` (
  `settlement_no`         BIGINT      NOT NULL AUTO_INCREMENT             COMMENT '정산 PK',
  `rider_no`              BIGINT      NOT NULL                            COMMENT '논리 FK → rider.rider_no',
  `period_start`          DATE        NOT NULL                            COMMENT '정산 기간 시작 (월요일)',
  `period_end`            DATE        NOT NULL                            COMMENT '정산 기간 종료 (일요일)',
  `delivery_count`        INT         NOT NULL                            COMMENT '배달 건수',
  `total_distance_m`      INT         NOT NULL DEFAULT 0                  COMMENT '총 이동 거리 (m) — Phase 5-R7 산출 방식 별도',
  `total_base_fee`        INT         NOT NULL                            COMMENT 'base_fee 합계',
  `total_extra_fee`       INT         NOT NULL                            COMMENT 'extra_fee 합계',
  `commission`            INT         NOT NULL                            COMMENT '수수료',
  `tax`                   INT         NOT NULL                            COMMENT '세금 (3.3%)',
  `insurance`             INT         NOT NULL                            COMMENT '보험료',
  `payout`                INT         NOT NULL                            COMMENT '실 수령액 = base+extra-commission-tax-insurance',
  `status`                VARCHAR(20) NOT NULL DEFAULT 'PENDING'          COMMENT 'PENDING / CONFIRMED — D10-b SettlementStatus enum',
  `confirmed_by_admin_no` BIGINT      DEFAULT NULL                        COMMENT 'admin이 confirm 시 기록 (논리 FK → my_mmg_admin.admin)',
  `confirmed_at`          DATETIME    DEFAULT NULL                        COMMENT 'admin confirm 시각',
  `paid_at`               DATETIME    DEFAULT NULL                        COMMENT '실 입금 시각 (NULL = 미입금)',
  `created_at`            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`settlement_no`),
  KEY `idx_settlement_rider_no` (`rider_no`),
  UNIQUE KEY `uq_settlement_rider_period` (`rider_no`, `period_start`, `period_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6) notice 테이블 (라이더 공지사항, ADR-002 정정 8 + ADR-009)
-- 외부 참조: notice.sender_admin_no → my_mmg_admin.admin (논리 FK, 물리 FK 제약 X)
-- 작성 흐름: admin-service Feign 호출 → rider-service POST /internal/notice → INSERT (ADR-009 흐름)
-- BaseEntity 상속 (R2-a 패턴 일관, UPDATE 다수: admin PUT/DELETE 가능)
-- 인덱스 1건: published_at (라이더 조회 시 가시성 필터 WHERE published_at <= NOW(), ADR-009 line 201 명시)
-- 신규 enum: NoticeCategory (IMPORTANT / SAFETY / GENERAL) — Figma 정정 8
CREATE TABLE IF NOT EXISTS `notice` (
  `notice_no`        BIGINT       NOT NULL AUTO_INCREMENT             COMMENT '공지 PK',
  `category`         VARCHAR(20)  NOT NULL                            COMMENT 'IMPORTANT/SAFETY/GENERAL — NoticeCategory enum',
  `title`            VARCHAR(200) NOT NULL                            COMMENT '공지 제목',
  `content`          TEXT         NOT NULL                            COMMENT '공지 본문',
  `target_type`      VARCHAR(20)  NOT NULL DEFAULT 'ALL'              COMMENT 'ALL/RIDER/SPECIFIC — NoticeTargetType enum',
  `send_type`        VARCHAR(20)  NOT NULL DEFAULT 'NOW'              COMMENT 'NOW/RESERVED — NoticeSendType enum',
  `reserved_at`      DATETIME     DEFAULT NULL                        COMMENT '예약 발송 시각 (sendType=RESERVED 시 필수)',
  `published_at`     DATETIME     NOT NULL                            COMMENT '발송 시점 (즉시 = now, 예약 = reservedAt)',
  `sender_admin_no`  BIGINT       NOT NULL                            COMMENT '논리 FK → my_mmg_admin.admin',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`notice_no`),
  KEY `idx_notice_published_at` (`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- Migration: notice 컬럼 3개 추가 (target_type / send_type / reserved_at)
-- POST /internal/rider/notice (즉시/예약 발송 분기)
-- 학원 DB 적용 시 1회 실행. 이미 적용된 환경은 skip (재실행 안전).
-- =============================================================
ALTER TABLE `notice`
  ADD COLUMN IF NOT EXISTS `target_type` VARCHAR(20) NOT NULL DEFAULT 'ALL' AFTER `content`,
  ADD COLUMN IF NOT EXISTS `send_type`   VARCHAR(20) NOT NULL DEFAULT 'NOW' AFTER `target_type`,
  ADD COLUMN IF NOT EXISTS `reserved_at` DATETIME       DEFAULT NULL          AFTER `send_type`;

-- =============================================================
-- Migration: delivery_log.reason 컬럼 추가 (R6-cancel)
-- POST /api/rider/order/{deliveryNo}/cancel 사유 박제 (ACCIDENT/PERSONAL/OTHER)
-- cancel 시만 박제, 다른 transition은 NULL.
-- 학원 DB 적용 시 1회 실행. 이미 적용된 환경은 skip (재실행 안전).
-- =============================================================
ALTER TABLE `delivery_log`
  ADD COLUMN IF NOT EXISTS `reason` VARCHAR(20) DEFAULT NULL AFTER `actor_user_no`;
