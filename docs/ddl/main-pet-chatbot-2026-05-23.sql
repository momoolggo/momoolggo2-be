-- Phase 5 펫/챗봇 풀세트 트랙 DDL (2026-05-23)
-- schema: my_mmg_main
-- 신설 3개: pets / chat_sessions / chat_messages
-- BaseEntity 일관: created_at / updated_at datetime(6)
-- 논리 FK 명시 (외부 schema 참조는 물리 FK 금지, CLAUDE.md §3)

-- ============================================================
-- 1) pets — 회원당 1마리 (user_no UNIQUE, Q3 (다-1) 자동 지급)
-- ============================================================
CREATE TABLE IF NOT EXISTS `pets` (
  `pet_no` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '펫번호 PK',
  `user_no` bigint(20) NOT NULL COMMENT '회원번호 (논리FK my_mmg_auth.user.user_no, UNIQUE)',
  `species` enum('DOG','CAT','RABBIT','HAMSTER') NOT NULL DEFAULT 'DOG' COMMENT '종족',
  `name` varchar(50) NOT NULL COMMENT '펫 이름 (회원가입 시 자동 지정 — JPA 레벨)',
  `level` int(11) NOT NULL DEFAULT 1 COMMENT '레벨 (1~)',
  `exp` int(11) NOT NULL DEFAULT 0 COMMENT '경험치 (0~)',
  `intimacy` int(11) NOT NULL DEFAULT 0 COMMENT '친밀도 (0~)',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`pet_no`),
  UNIQUE KEY `uq_pets_user_no` (`user_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 1:1 펫';

-- ============================================================
-- 2) chat_sessions — 챗봇 대화 세션 (MYPET / CS 분기, 톤 모드 4종)
-- ============================================================
CREATE TABLE IF NOT EXISTS `chat_sessions` (
  `session_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '세션 PK',
  `user_no` bigint(20) NOT NULL COMMENT '회원번호 (논리FK my_mmg_auth.user.user_no)',
  `pet_no` bigint(20) DEFAULT NULL COMMENT '펫번호 (MYPET 진입 시, 논리FK pets.pet_no)',
  `entry_point` enum('MYPET','CS') NOT NULL COMMENT '진입점',
  `tone_mode` enum('PLAYFUL','GOURMET','EMPATHY','SERIOUS') NOT NULL DEFAULT 'PLAYFUL' COMMENT '톤 모드 (장난꾸러기/미식가/공감/진지)',
  `status` enum('ACTIVE','ESCALATED','CLOSED') NOT NULL DEFAULT 'ACTIVE' COMMENT '세션 상태 (ESCALATED = 상담원 연결 대기)',
  `escalated_at` datetime(6) DEFAULT NULL COMMENT '에스컬레이션 시점',
  `closed_at` datetime(6) DEFAULT NULL COMMENT '세션 종료 시점',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`session_id`),
  KEY `idx_chat_sessions_user_no` (`user_no`),
  KEY `idx_chat_sessions_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='챗봇 세션';

-- ============================================================
-- 3) chat_messages — 챗봇 메시지 (USER / ASSISTANT / SYSTEM)
-- ============================================================
CREATE TABLE IF NOT EXISTS `chat_messages` (
  `message_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '메시지 PK',
  `session_id` bigint(20) NOT NULL COMMENT '세션 FK',
  `role` enum('USER','ASSISTANT','SYSTEM') NOT NULL COMMENT '발화 주체',
  `content` text NOT NULL COMMENT '메시지 본문',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`message_id`),
  KEY `idx_chat_messages_session_id` (`session_id`),
  CONSTRAINT `fk_chat_messages_session` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`session_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='챗봇 메시지';
