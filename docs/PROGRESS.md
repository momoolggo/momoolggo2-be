# MOMOOLGGO_MSA — 진행 스냅샷

> 작성: 2026-04-28 / 마지막 갱신 시점: **Phase 4-A 완료**
> 한 페이지로 Phase 0~4-A 전체 상태 + 다음 단계 정리.
> 상세 체크리스트는 [migration-plan.md](migration-plan.md), 결정 근거는 [decisions.md](decisions.md), 자료 인덱스는 [INDEX.md](INDEX.md).

---

## 🏆 현재 상태 한 줄

> **5개 Spring Boot 4.0.3 멀티모듈 + 4개 schema(MariaDB)에 33개 테이블 분배 + Feign으로 cross-schema 5개 해소 + BFF 회원가입 검증 통과. develop 브랜치 안정.**

---

## Phase 진행 상태

| Phase | 작업 | 상태 |
|---|---|---|
| 0-A | settings.gradle + root build.gradle + mmg-common (Gradle 9.3.1) | ✅ |
| 0-B | 5개 서비스(mmg-{auth,main,rider,admin,gateway}) hello world + Gateway 임시 라우팅 | ✅ |
| 1-A | mmg-common에 공통 코드 9개(JWT/ResultResponse/ConstJwt/Cookie/UserPrincipal 등) | ✅ |
| 1-B-1 | `my_mmg_auth` schema + user 15행 + address 20행 (utf8mb4_unicode_ci) | ✅ (1-B-3.5에서 address는 main으로 재이동) |
| 1-B-2 | BaseSecurityConfig (`@ConditionalOnClass` + `@ConditionalOnMissingBean`) + CORS env | ✅ |
| 1-B-3+4 | user/address → auth-service + DB 연결 + MyBatis | ✅ |
| 1.5 | GlobalExceptionHandler + JsonAuth/AccessDenied (mmg-common) | ✅ |
| 1-B-3.5 | **address 위치 정정** (auth → main, ERD 위반 수정) + 컬럼명 ERD 따름 + URL 통일 | ✅ |
| 2-A | `my_mmg_main` 13개 테이블 432행 + 외부 FK 5개 DROP | ✅ |
| 2-B | store + owner + WebConfig | ✅ |
| 2-C | cart + order + payment + TOSS_SECRET_KEY 환경변수화 | ✅ |
| 2-D | AddressSearch + MapConfig (네이버 API) | ✅ |
| 2-E | review 도메인 신규 작성 (5 endpoints + 9 SQL) | ✅ |
| 2-F | `my_mmg_rider`, `my_mmg_admin` 빈 schema | ✅ |
| 2-G | 통합 검증 (BFF 회원가입 7/7) | ✅ |
| **4-A** | **Feign + Internal API + cross-schema JOIN 5개 해소** | ✅ **방금 완료** |
| 3 | MyBatis → JPA 선별 마이그레이션 | ⏳ 다음 |
| 4-B | Gateway 라우팅 정비 | 대기 |
| 4-C | Redis 도입 | 대기 |
| 4-D / 6 | 외부 FK 정합성 (Saga/Outbox) | 대기 |
| 5 | 신규 기능 (펫/룰렛/챗봇/SSE/Rider/Admin) | 대기 |
| 6 | 고도화 (Outbox/Kafka/모니터링/CI-CD) | 대기 |

---

## 4개 schema DB 분배

| Schema | 테이블 | 행수 | 용도 |
|---|---|---|---|
| **my_mmg_auth** | user (1개) | 15 | 회원/JWT |
| **my_mmg_main** | store, store_category, menu, menu_category, category, likedstore, cart, cart_detail, orders, order_detail, payment, review, review_reply, **address** (14개) | ~452 | 가게/메뉴/주문/리뷰/결제/주소 |
| **my_mmg_rider** | (빈 schema) | 0 | Phase 5에 rider_profile 등 |
| **my_mmg_admin** | (빈 schema) | 0 | Phase 5에 FAQ, penalty 등 |

**외부 FK 5개 DROP**: store/likedstore/cart/orders/review_reply 의 user 참조 (Saga/Outbox는 Phase 4-D/6).

---

## 적용된 핵심 디자인 결정

1. **ERD = source of truth** (CLAUDE.md §6.11) — Phase 1-B-3에서 user_address 잘못 분배한 사건 계기로 명문화
2. **회원가입 = BFF 옵션 D-1** — POST /api/user/join이 즉시 AT/RT 발급 + userNo 반환 → 프론트가 곧장 POST /api/address (FRONTEND_CHANGES.md)
3. **cross-schema read** = Feign + batch (`/internal/auth/users?ids=`) for N+1 회피
4. **/internal/** 보안** = Phase 4-A permitAll 임시, Phase 4-B Gateway 외부 차단, Phase 6 mTLS 검토
5. **mmg-common 빈 활성화** = `scanBasePackages` + `@ConditionalOnClass(SecurityFilterChain)` 패턴
6. **컬럼 ERD 따름** = lat→latitude, lng→longitude, address_detail VARCHAR(200)
7. **TOSS_SECRET_KEY 환경변수화** (CLAUDE.md §6.10 보안)

---

## 5개 서비스 상태

| 서비스 | 포트 | DB | 시큐리티 | 상태 |
|---|---|---|---|---|
| mmg-auth-service | 8081 | my_mmg_auth | AuthSecurityConfig + Internal API | ✅ |
| mmg-main-service | 8080 | my_mmg_main | MainSecurityConfig + WebConfig + Feign client | ✅ |
| mmg-rider-service | 8082 | (없음) | hello world | Phase 5 대기 |
| mmg-admin-service | 8083 | (없음) | hello world | Phase 5 대기 |
| mmg-gateway | 8000 | — | (시큐리티 없음) | 라우팅 임시 (Phase 4-B 대기) |

---

## 검증 통과 흔적

| 시점 | 검증 | 결과 |
|---|---|---|
| Phase 0-B | 5 hello world + Gateway 라우팅 (auth 1개) | ✅ |
| Phase 1-B-3+4 | BFF 흐름 7/7 | ✅ |
| Phase 1.5 | 5 에러 시나리오 (401/401/409/200/400) | ✅ |
| Phase 2-G | 통합 검증 7/7 (auth+main+gateway 동시 기동) | ✅ |
| Phase 4-A | Feign 4/4 (Internal 단건/batch + 가게 상세 + 리뷰 batch 합성) | ✅ |

---

## 짚어둘 / 미해결 (Phase 별 계획됨)

| 항목 | 처리 시점 |
|---|---|
| Gateway 라우팅이 Phase 0-B 임시 (`/api/auth/**` 등) | **Phase 4-B** |
| user.status 컬럼 없음 (admin 승인 흐름) | Phase 5 |
| 외부 FK 정합성 (사용자 탈퇴 cleanup) | Phase 4-D 또는 6 |
| `/internal/**` 보안 강화 (mTLS 등) | Phase 6 |
| owner_comment 테이블 (ERD vs 코드 불일치) | Phase 5 결정 |
| OwnerInfoDto 분리 (현재 UserBriefDto 재사용) | Phase 5 |
| TOSS_SECRET_KEY placeholder | Phase 5 결제 본격화 |

---

## Git 상태

- 브랜치: **develop** (Phase 0~4-A 모든 commit + push 완료)
- 최근 커밋: `9ca822c feat(feign): add Internal API + Feign client for cross-schema JOIN`
- GitHub: https://github.com/momoolggo/momoolggo2-be/tree/develop

---

## 다음 단계 — Phase 3 진입 신호 대기

JPA 선별 마이그레이션 (단순 CRUD부터). 정찰부터 자동 진행 가능.
