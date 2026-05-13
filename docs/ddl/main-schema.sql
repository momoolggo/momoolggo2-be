-- =============================================================
-- my_mmg_main schema DDL (Phase 2-A)
-- 원본: my_testmomoolggo (utf8mb4_bin) — 13개 테이블
-- 변경:
--   1) collation utf8mb4_bin → utf8mb4_unicode_ci (1-B-1과 일관)
--   2) 외부 FK 5개 DROP (사용자 결정 Q1):
--      store.owner_id, cart.user_no, likedstore.user_no,
--      orders.user_no, review_reply.owner_id
--      → MSA 경계, 향후 Phase 4-A에서 Saga/Outbox로 정합성 처리
--   3) 같은 schema 내 FK 13개 모두 유지
-- =============================================================

USE my_mmg_main;

-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 112.222.157.157    Database: my_mmg_main
-- ------------------------------------------------------
-- Server version	11.8.2-MariaDB-ubu2404

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cart`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart` (
  `cart_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '장바구니번호 PK',
  `user_no` bigint(20) NOT NULL COMMENT '회원번호 FK',
  `store_id` bigint(20) NOT NULL COMMENT '가게번호 FK',
  PRIMARY KEY (`cart_id`),
  KEY `cart_ibfk_1` (`user_no`),
  KEY `cart_ibfk_2` (`store_id`),
  CONSTRAINT `cart_ibfk_2` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=115 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cart_detail`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_detail` (
  `cart_item_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '장바구니내역 PK',
  `cart_id` bigint(20) NOT NULL COMMENT '장바구니번호 FK',
  `menu_id` bigint(20) NOT NULL COMMENT '메뉴번호 FK',
  `quantity` int(11) NOT NULL DEFAULT 1 COMMENT '수량',
  PRIMARY KEY (`cart_item_id`),
  KEY `cart_detail_ibfk_1` (`cart_id`),
  KEY `cart_detail_ibfk_2` (`menu_id`),
  CONSTRAINT `cart_detail_ibfk_1` FOREIGN KEY (`cart_id`) REFERENCES `cart` (`cart_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `cart_detail_ibfk_2` FOREIGN KEY (`menu_id`) REFERENCES `menu` (`menu_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=177 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `category`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `category_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '카테고리번호 PK',
  `category_name` varchar(20) NOT NULL COMMENT '카테고리명',
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `likedstore`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `likedstore` (
  `user_no` bigint(20) NOT NULL COMMENT '회원번호 FK',
  `store_id` bigint(20) NOT NULL COMMENT '가게번호 FK',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`user_no`,`store_id`),
  KEY `likedstore_ibfk_2` (`store_id`),
  CONSTRAINT `likedstore_ibfk_2` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `menu`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `menu` (
  `menu_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '메뉴번호 PK',
  `category_id` bigint(20) DEFAULT NULL COMMENT '카테고리번호 FK',
  `name` varchar(30) DEFAULT NULL COMMENT '메뉴명',
  `price` int(11) DEFAULT 0 COMMENT '가격',
  `menu_pic` varchar(1000) DEFAULT NULL,
  `soldout` int(11) DEFAULT 0 COMMENT '품절여부 (1:품절)',
  `menu_info` varchar(300) DEFAULT NULL COMMENT '메뉴설명',
  `menu_order` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`menu_id`),
  KEY `FK_menu_menu_category` (`category_id`),
  CONSTRAINT `FK_menu_menu_category` FOREIGN KEY (`category_id`) REFERENCES `menu_category` (`category_id`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=140 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `menu_category`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `menu_category` (
  `category_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `store_id` bigint(20) NOT NULL,
  `category` varchar(15) NOT NULL,
  `category_order` int(11) DEFAULT 1,
  PRIMARY KEY (`category_id`),
  KEY `menu_category_ibfk_1` (`store_id`),
  CONSTRAINT `menu_category_ibfk_1` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=133 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_detail`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_detail` (
  `detail_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '주문상세번호 PK',
  `order_id` bigint(100) NOT NULL COMMENT '주문번호 FK',
  `menu_id` bigint(20) NOT NULL COMMENT '메뉴번호 FK',
  `quantity` int(11) NOT NULL DEFAULT 1 COMMENT '수량',
  `menu_name` varchar(30) DEFAULT NULL COMMENT '주문당시 메뉴명',
  `menu_price` int(11) DEFAULT 0 COMMENT '주문당시 가격',
  PRIMARY KEY (`detail_id`),
  KEY `order_detail_ibfk_1` (`order_id`),
  KEY `order_detail_ibfk_2` (`menu_id`),
  CONSTRAINT `order_detail_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=196 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `orders`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_no` bigint(20) NOT NULL COMMENT '회원번호 FK',
  `store_id` bigint(20) NOT NULL COMMENT '가게번호 FK',
  `order_time` datetime DEFAULT current_timestamp() COMMENT '주문일시',
  `request` varchar(200) DEFAULT NULL COMMENT '요청사항',
  `rider_request` varchar(200) DEFAULT NULL COMMENT '라이더요청',
  `address` varchar(100) DEFAULT NULL COMMENT '배달주소',
  `address_detail` varchar(100) DEFAULT NULL COMMENT '배달상세주소',
  `delivery_fee` int(11) DEFAULT 1500 COMMENT '배달비',
  `amount` int(11) DEFAULT NULL COMMENT '총결제금액',
  `delivery_state` int(11) DEFAULT 1 COMMENT '1배달전 2픽업완료 3배달완료',
  `pay_state` int(11) DEFAULT 1 COMMENT '1결제전 2결제완료 3결제환불',
  `order_state` int(11) DEFAULT 1 COMMENT '1주문수락전 2주문취소 3주문수락조리중 4라이더배차중 5라이더 배차완료 6배달완료',
  PRIMARY KEY (`order_id`),
  KEY `orders_ibfk_1` (`user_no`),
  KEY `orders_ibfk_2` (`store_id`),
  CONSTRAINT `orders_ibfk_2` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`) ON DELETE NO ACTION ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=391775460588724 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment` (
  `payment_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '결제번호 PK',
  `order_id` bigint(20) NOT NULL COMMENT '주문번호 FK',
  `payment_key` varchar(200) NOT NULL COMMENT '토스 결제키',
  `amount` int(11) NOT NULL COMMENT '결제금액',
  `pay_state` int(11) NOT NULL DEFAULT 1 COMMENT '1카드 2카카오 3네이버 4만나서',
  `payment_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '결제일시',
  PRIMARY KEY (`payment_id`),
  KEY `payment_ibfk_1` (`order_id`),
  CONSTRAINT `payment_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `review`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
  `review_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '리뷰번호 PK',
  `order_id` bigint(20) NOT NULL,
  `rating` int(11) DEFAULT 5 COMMENT '별점 (1~5)',
  `contents` varchar(1000) DEFAULT NULL COMMENT '리뷰내용',
  `photo` varchar(1000) DEFAULT NULL COMMENT '리뷰이미지',
  `write_at` datetime DEFAULT current_timestamp() COMMENT '작성일시',
  `amended_at` datetime DEFAULT NULL COMMENT '수정일시',
  PRIMARY KEY (`review_id`),
  UNIQUE KEY `FK_review_my_testmomoolggo.orders` (`order_id`) USING BTREE,
  CONSTRAINT `FK_review_orders` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `review_reply`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_reply` (
  `reply_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '댓글번호 PK',
  `review_id` bigint(20) NOT NULL COMMENT '리뷰번호 FK',
  `owner_id` bigint(20) NOT NULL COMMENT '사장 회원번호 FK',
  `content` varchar(1000) DEFAULT NULL COMMENT '댓글내용',
  `writ_at` datetime DEFAULT current_timestamp() COMMENT '작성일시',
  PRIMARY KEY (`reply_id`),
  KEY `review_reply_ibfk_1` (`review_id`),
  KEY `review_reply_ibfk_2` (`owner_id`),
  CONSTRAINT `review_reply_ibfk_1` FOREIGN KEY (`review_id`) REFERENCES `review` (`review_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `store`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store` (
  `store_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '가게번호 PK',
  `owner_id` bigint(20) NOT NULL COMMENT '사장 회원번호 FK',
  `store_name` varchar(30) NOT NULL COMMENT '가게명',
  `business_hours` varchar(30) DEFAULT NULL COMMENT '운영시간',
  `min_price` int(11) DEFAULT 0 COMMENT '최소주문금액',
  `holiday` varchar(30) DEFAULT NULL COMMENT '휴무일',
  `state` int(11) DEFAULT 0 COMMENT '가게상태 0종료 1오픈',
  `location` varchar(100) DEFAULT NULL COMMENT '주소',
  `detail_location` varchar(100) DEFAULT NULL COMMENT '상세주소',
  `latitude` decimal(16,13) DEFAULT NULL COMMENT '위도',
  `longitude` decimal(16,13) DEFAULT NULL COMMENT '경도',
  `created_at` date DEFAULT curdate() COMMENT '등록일시',
  `updated_at` date DEFAULT curdate() COMMENT '수정일시',
  `notice` varchar(300) DEFAULT NULL COMMENT '공지',
  `business_number` varchar(30) DEFAULT NULL COMMENT '사업자등록번호',
  `business_name` varchar(30) DEFAULT NULL COMMENT '사업자명',
  `store_tel` varchar(20) DEFAULT NULL COMMENT '가게전화번호',
  `store_pic` varchar(1000) DEFAULT NULL,
  `store_info` varchar(300) DEFAULT NULL COMMENT '가게소개글',
  `rating_avg` int(11) DEFAULT NULL,
  `rating_count` int(11) DEFAULT NULL,
  `order_count` int(11) DEFAULT NULL,
  PRIMARY KEY (`store_id`),
  KEY `owner_id` (`owner_id`)
) ENGINE=InnoDB AUTO_INCREMENT=59 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `store_category`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store_category` (
  `store_id` bigint(20) NOT NULL COMMENT '가게번호 FK',
  `category_id` bigint(20) NOT NULL COMMENT '카테고리번호 FK',
  PRIMARY KEY (`store_id`,`category_id`),
  KEY `store_category_ibfk_2` (`category_id`),
  CONSTRAINT `store_category_ibfk_1` FOREIGN KEY (`store_id`) REFERENCES `store` (`store_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `store_category_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-28 15:13:22
