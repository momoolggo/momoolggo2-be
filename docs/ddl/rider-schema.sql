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
  `order_id`            VARCHAR(20)    NOT NULL                                COMMENT '논리 FK → my_mmg_main.orders.order_id (형식 000001A) — Figma 정정 9',
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
