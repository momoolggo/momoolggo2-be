# docs/ddl/

DB 스키마 DDL과 마이그레이션 절차 기록. 미래 schema(main/rider/admin) 마이그레이션 시 같은 패턴 재사용.

## 학원 DB 환경

| 항목 | 값 |
|---|---|
| 호스트 | `112.222.157.157:5012` |
| 계정 | `green2` |
| 권한 | `my\_%` 패턴에 ALL PRIVILEGES |
| **서버 종류** | **MariaDB 11.8.2** (MySQL 8.0 호환) |
| 클라이언트 | MySQL 8.0.44 (정상 호환) |
| 원본 schema | `my_testmomoolggo` (utf8mb4_bin) |
| 신규 schema 패턴 | `my_mmg_{service}` (utf8mb4_unicode_ci) |

> ⚠️ **MariaDB**임을 인지. `mysqldump`에 `--column-statistics=0` 옵션 필수 (해당 information_schema 테이블 미지원). Phase 3 JPA 마이그레이션 시 `MariaDBDialect` 검토.

## 파일 종류

| 파일 패턴 | 용도 | git 추적? |
|---|---|---|
| `*-schema.sql` | 신규 schema DDL (CREATE TABLE) | ✅ 추적 |
| `dump-*.sql` | 원본 데이터 백업 (mysqldump 출력) | ❌ `.gitignore` 차단 (실데이터 + PII) |
| `backup-*.sql` | 임시 백업 | ❌ 차단 |

## Phase 1-B-1 — `my_mmg_auth` 마이그레이션 (완료)

### 결과 요약
| 테이블 | 원본 행수 | 신규 행수 | 매칭 |
|---|---|---|---|
| user | 15 | 15 | ✅ |
| address | 20 | 20 | ✅ |

| 테이블 | 원본 AUTO_INCREMENT | 신규 AUTO_INCREMENT | 매칭 |
|---|---|---|---|
| user | 18 | 18 | ✅ |
| address | 38 | 38 | ✅ |

- FK 정합성: `my_mmg_auth.address.user_no → my_mmg_auth.user.user_no` (신규 schema 내부) ✅
- Orphan 0개 ✅
- collation 변경: `utf8mb4_bin` → `utf8mb4_unicode_ci` (사용자 ID 대소문자 무시 정책)
- user_id LOWER 중복 사전 검사 통과 (충돌 0건)

### 실행 절차

```bash
# 1) 신규 schema 생성
mysql -h 112.222.157.157 -P 5012 -u green2 -p \
  -e "CREATE DATABASE IF NOT EXISTS my_mmg_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2) DDL 적용
mysql -h 112.222.157.157 -P 5012 -u green2 -p < docs/ddl/auth-schema.sql

# 3) 데이터 복사 (schema 간 INSERT...SELECT — 같은 서버 내라 효율적)
mysql -h 112.222.157.157 -P 5012 -u green2 -p -e "
  INSERT INTO my_mmg_auth.user    SELECT * FROM my_testmomoolggo.user;
  INSERT INTO my_mmg_auth.address SELECT * FROM my_testmomoolggo.address;
"

# 4) 검증 (row count, AUTO_INCREMENT, FK, orphan)
# (자세한 쿼리는 Phase 1-B-1 작업 로그 참조)

# 5) 백업 (참고용)
mysqldump -h 112.222.157.157 -P 5012 -u green2 -p \
  --single-transaction --no-tablespaces --skip-add-locks --skip-lock-tables \
  --column-statistics=0 --default-character-set=utf8mb4 \
  my_testmomoolggo user address > docs/ddl/dump-my_testmomoolggo-auth-{ts}.sql
```

### Phase 2-F — `my_mmg_rider` / `my_mmg_admin` 빈 schema 생성 (완료)

```sql
CREATE DATABASE my_mmg_rider CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE my_mmg_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

- 원본 my_testmomoolggo에 rider/admin 도메인 테이블 미존재
- Phase 5 신규 기능 구현 시 ERD 따라 신규 테이블 생성 (rider_profile, rider_cache, FAQ, penalty 등)

---

## Phase 2-A — `my_mmg_main` 마이그레이션 (완료)

### 결과 요약

13개 테이블, 432행 모두 무손상 복사. collation `utf8mb4_unicode_ci` 일관 적용.

| 테이블 | 행수 | AUTO_INCREMENT | 비고 |
|---|---|---|---|
| store | 37 | 59 | — |
| store_category | 37 | (composite PK, AI 없음) | category × store |
| menu | 121 | 140 | — |
| menu_category | 84 | 133 | store별 카테고리 |
| category | 12 | 13 | 마스터 |
| likedstore | 28 | (composite PK) | user × store |
| cart | 1 | 115 | |
| cart_detail | 1 | 177 | |
| orders | 39 | 391775460588724 | ⚠️ 비즈니스 키 패턴 (timestamp) |
| order_detail | 58 | 196 | |
| payment | 32 | 44 | |
| review | 12 | 46 | Phase 2-E 코드 신규 작성 |
| review_reply | 0 | 1 | |

### 외부 FK 처리 (Phase 1 D2 결정 + 사용자 Q1=A)

**DROP된 FK 5개** (모두 `→ user.user_no` 참조):
- `store.store_ibfk_1` (owner_id)
- `likedstore.FK_likedstore_user` (user_no)
- `cart.cart_ibfk_1` (user_no)
- `orders.orders_ibfk_1` (user_no)
- `review_reply.review_reply_ibfk_2` (owner_id)

**보존된 내부 FK 13개**: 같은 schema 내부(store/category/menu/orders/cart/review 사이) — 그대로.

> ⚠️ **TODO (Phase 4-A)**: 외부 FK 정합성 처리 — 사용자 탈퇴 시 main 도메인 데이터 cleanup. Saga 패턴 또는 Outbox 패턴 검토. auth-service의 사용자 탈퇴 → main-service에 cleanup 이벤트 전송.

### 실행 절차 (재사용 가능)

```bash
# 1) 신규 schema 생성
mysql -h 112.222.157.157 -P 5012 -u green2 -p \
  -e "CREATE DATABASE IF NOT EXISTS my_mmg_main CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2) DDL 추출 (mysqldump --no-data) → utf8mb4_bin → utf8mb4_unicode_ci 변환
mysqldump --no-data --no-tablespaces --skip-add-locks --skip-lock-tables \
  --column-statistics=0 --default-character-set=utf8mb4 --skip-add-drop-table \
  -h 112.222.157.157 -P 5012 -u green2 -p \
  my_testmomoolggo store store_category menu menu_category category likedstore \
  cart cart_detail orders order_detail payment review review_reply \
  > /tmp/main-ddl.sql
sed -i 's/utf8mb4_bin/utf8mb4_unicode_ci/g' /tmp/main-ddl.sql

# 3) 적용 (FOREIGN_KEY_CHECKS=0 자동 포함되어 외부 user FK도 일단 import)
mysql -h ... my_mmg_main < /tmp/main-ddl.sql

# 4) 외부 FK 5개 DROP (Saga/Outbox 도입 전까지 논리 FK)
mysql -h ... -e "
  ALTER TABLE my_mmg_main.store        DROP FOREIGN KEY store_ibfk_1;
  ALTER TABLE my_mmg_main.likedstore   DROP FOREIGN KEY FK_likedstore_user;
  ALTER TABLE my_mmg_main.cart         DROP FOREIGN KEY cart_ibfk_1;
  ALTER TABLE my_mmg_main.orders       DROP FOREIGN KEY orders_ibfk_1;
  ALTER TABLE my_mmg_main.review_reply DROP FOREIGN KEY review_reply_ibfk_2;"

# 5) 데이터 복사 (의존 순서: 마스터/leaf → 관계 자식)
mysql -h ... -e "
  SET FOREIGN_KEY_CHECKS=0;
  INSERT INTO my_mmg_main.category       SELECT * FROM my_testmomoolggo.category;
  INSERT INTO my_mmg_main.store          SELECT * FROM my_testmomoolggo.store;
  INSERT INTO my_mmg_main.store_category SELECT * FROM my_testmomoolggo.store_category;
  INSERT INTO my_mmg_main.menu_category  SELECT * FROM my_testmomoolggo.menu_category;
  INSERT INTO my_mmg_main.likedstore     SELECT * FROM my_testmomoolggo.likedstore;
  INSERT INTO my_mmg_main.menu           SELECT * FROM my_testmomoolggo.menu;
  INSERT INTO my_mmg_main.cart           SELECT * FROM my_testmomoolggo.cart;
  INSERT INTO my_mmg_main.orders         SELECT * FROM my_testmomoolggo.orders;
  INSERT INTO my_mmg_main.cart_detail    SELECT * FROM my_testmomoolggo.cart_detail;
  INSERT INTO my_mmg_main.order_detail   SELECT * FROM my_testmomoolggo.order_detail;
  INSERT INTO my_mmg_main.payment        SELECT * FROM my_testmomoolggo.payment;
  INSERT INTO my_mmg_main.review         SELECT * FROM my_testmomoolggo.review;
  INSERT INTO my_mmg_main.review_reply   SELECT * FROM my_testmomoolggo.review_reply;
  SET FOREIGN_KEY_CHECKS=1;"

# 6) 검증 (row count, AUTO_INCREMENT, 외부 FK 0, 내부 FK 13)
# 7) 백업 → docs/ddl/dump-* (gitignore)
```
