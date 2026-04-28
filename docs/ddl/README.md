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

### 다음 schema 마이그레이션 시 주의

- **`my_mmg_main` (Phase 2)**: cart, likedstore, orders, review_reply, store 5개 테이블이 원본에서 user를 물리 FK로 참조 중. **이 5개 FK를 모두 DROP** 후 논리 FK로 전환 필요 (CLAUDE.md §3 "MSA 경계 외부 참조는 논리 FK만 사용").
- **`my_mmg_rider` / `my_mmg_admin`**: 원본에 해당 도메인 테이블 없음 — Phase 5에서 신규 생성.
