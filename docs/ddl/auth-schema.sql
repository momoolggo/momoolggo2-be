-- =============================================================
-- my_mmg_auth schema DDL (Phase 1-B-1)
-- 원본: my_testmomoolggo (utf8mb4_bin)
-- 변경: collation utf8mb4_bin → utf8mb4_unicode_ci
--   (사용자 ID 대소문자 구분 없는 매칭 / 한글 정렬 정확도 개선)
-- 그 외 컬럼/제약조건은 원본 100% 유지
-- =============================================================

USE my_mmg_auth;

-- 1) user 테이블 (leaf, FK 없음)
CREATE TABLE IF NOT EXISTS `user` (
  `user_no` BIGINT NOT NULL AUTO_INCREMENT COMMENT '회원번호 PK',
  `user_id` VARCHAR(20) DEFAULT NULL COMMENT '회원 아이디',
  `user_pw` VARCHAR(1000) DEFAULT NULL COMMENT '회원 비밀번호 (BCrypt)',
  `role` ENUM('CUSTOMER','OWNER','RIDER','ADMIN') DEFAULT 'CUSTOMER' COMMENT '역할',
  `name` VARCHAR(10) DEFAULT NULL COMMENT '이름',
  `birth` DATE DEFAULT NULL COMMENT '생년월일',
  `gender` INT DEFAULT NULL COMMENT '성별 (1:남 2:여)',
  `green` INT DEFAULT 0 COMMENT '친환경점수',
  `kind` INT DEFAULT 0 COMMENT '주문 빈도',
  `rank` ENUM('BRONZE','SILVER','GOLD','VIP','VVIP') DEFAULT 'BRONZE' COMMENT '등급',
  `tel` VARCHAR(20) DEFAULT NULL COMMENT '핸드폰번호',
  PRIMARY KEY (`user_no`),
  UNIQUE KEY `uq_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) address 테이블 (user 의존, 같은 schema 내 물리 FK 유지)
CREATE TABLE IF NOT EXISTS `address` (
  `address_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '주소번호 PK',
  `user_no` BIGINT NOT NULL COMMENT '회원번호 FK',
  `default_ad` INT DEFAULT 0 COMMENT '기본주소 여부 (1:기본)',
  `address` VARCHAR(100) DEFAULT NULL COMMENT '기본주소',
  `address_detail` VARCHAR(300) DEFAULT NULL COMMENT '상세주소',
  `lat` DECIMAL(16,13) DEFAULT NULL COMMENT '위도',
  `lng` DECIMAL(16,13) DEFAULT NULL COMMENT '경도',
  PRIMARY KEY (`address_id`),
  KEY `address_ibfk_1` (`user_no`),
  CONSTRAINT `address_ibfk_1` FOREIGN KEY (`user_no`)
    REFERENCES `user` (`user_no`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
