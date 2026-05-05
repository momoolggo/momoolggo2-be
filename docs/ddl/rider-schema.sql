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
