# MOMOOLGGO_MSA — 진행 스냅샷

> 작성: 2026-04-28 / 최종 갱신: **2026-05-06 Phase 5-R1-A 종결 + R2-a/b 적용 (rider 10건 PASS + DeliveryLog 3건, 학원 DB my_mmg_rider 3 테이블 적용 / Q-DB (다) → (가) 전환)**
> 한 페이지로 Phase 0~4-B 전체 상태 + 다음 단계 정리.
> 상세 체크리스트는 [migration-plan.md](migration-plan.md), 결정 근거는 [decisions.md](decisions.md), 자료 인덱스는 [INDEX.md](INDEX.md).

---

## 🏆 현재 상태 한 줄

> **5개 Spring Boot 4.0.3 멀티모듈 + 4개 schema(MariaDB) + Feign cross-schema + JPA/MyBatis 하이브리드 영구 공존 + Gateway 단일 진입점 정비. 🔥 학원 발표/시연 가능. develop 브랜치 안정.**

---

## 진행 흐름

> **Phase 1 → 2 → 3 → 4-A → 4-B → 4-C → [라이더 정리 단계] → Phase 5 → 학원 발표 (최종 마일스톤)**
>
> 학원 발표는 Phase 5 종결 후 **최종 마일스톤**이지 단계 진행 조건 아님.
> 팀원 합류는 **진행 조건 아님** (단독 진행 가능).
> Phase 4-C / 5 / 라이더 정리는 모두 **정상 순차 진행**.

| 단계 | 위치 | 비고 |
|---|---|---|
| Phase 1~3 | ✅ 완료 (Phase 3 백필 2026-05-02 종결, 177 PASS) | — |
| Phase 4-A | ✅ 완료 (Feign + Internal API + cross-schema JOIN 5개 해소) | — |
| Phase 4-B | ✅ 완료 (Gateway 라우팅 / Internal 차단 / CORS 통합) | — |
| **Phase 4-C** | ⏳ 진행 예정 (4-B 다음 정상) | Redis 토큰 저장 / 날씨 캐시 / Pub-Sub |
| **라이더 정리 단계** | 대기 (4-C 후) | mmg-rider-service 사전 설계 — 결정 항목: 서비스 경계 / 데이터 모델 / 통신 패턴(Feign vs 메시지 큐) / 상태 머신(배차 대기 → 배차 → 픽업 → 이동 → 완료) / 위치 추적 방식 / Phase 4-C Pub/Sub 활용 방안. **산출물**: 라이더 모듈 ADR + 인터페이스 명세 |
| **Phase 5** | 대기 (라이더 정리 후) | 펫/룰렛/챗봇/SSE/Rider/Admin + TossPaymentClient + 잔존 도메인 경계 정리 |
| 학원 발표 | Phase 5 종결 후 최종 마일스톤 | 진행 조건 아님 |
| Phase 4-D / 6 | Phase 5 후 또는 병행 | 외부 FK Saga/Outbox / 고도화 (Outbox/Kafka/모니터링/CI-CD) |

---

## Phase 진행 상태

| Phase | 작업 | 완료일 | 상태 |
|---|---|---|---|
| 0-A | settings.gradle + root build.gradle + mmg-common (Gradle 9.3.1) | 2026-04-28 | ✅ |
| 0-B | 5개 서비스 hello world + Gateway 임시 라우팅 | 2026-04-28 | ✅ |
| 1-A | mmg-common 공통 코드 9개 (JWT/ResultResponse/ConstJwt/Cookie/UserPrincipal 등) | 2026-04-28 | ✅ |
| 1-B-1 | `my_mmg_auth` schema + user 15행 + address 20행 (utf8mb4_unicode_ci) | 2026-04-28 | ✅ |
| 1-B-2 | BaseSecurityConfig + CORS env (`@ConditionalOnClass` + `@ConditionalOnMissingBean`) | 2026-04-28 | ✅ |
| 1-B-3+4 | user/address → auth-service + DB 연결 + MyBatis | 2026-04-28 | ✅ |
| 1.5 | GlobalExceptionHandler + JsonAuth/AccessDenied (mmg-common) | 2026-04-28 | ✅ |
| 1-B-3.5 | **address 위치 정정** (auth → main, ERD 위반 수정) + 컬럼명 ERD 따름 + URL 통일 | 2026-04-28 | ✅ |
| 2-A | `my_mmg_main` 13개 테이블 432행 + 외부 FK 5개 DROP | 2026-04-28 | ✅ |
| 2-B/C/D/E | store/owner + cart/order/payment + AddressSearch/Map + review (5 endpoints + 9 SQL) | 2026-04-28 | ✅ |
| 2-F | `my_mmg_rider`, `my_mmg_admin` 빈 schema | 2026-04-28 | ✅ |
| 2-G | 통합 검증 (BFF 회원가입 7/7) | 2026-04-28 | ✅ |
| 4-A | Feign + Internal API + cross-schema JOIN 5개 해소 | 2026-04-28 | ✅ |
| **3-A** | auth-service User JPA 전환 (BaseEntity 인프라, StringDateConverter, rank 백틱) | **2026-04-29** | ✅ |
| **3-B** | main-service Payment + LikedStore + Cart 하이브리드 검증 | **2026-04-29** | ✅ |
| **3-C** | Order/OrderDetail/Review JPA + 🎯 BaseEntity 첫 검증 | **2026-04-29** | ✅ |
| **3-D** | UserAddress JPA + Store/Owner MyBatis 유지 확정 | **2026-04-29** | ✅ |
| **4-B** | Gateway 라우팅 정비 + Internal 차단 + CORS 통합 | **2026-04-29** | ✅ |
| **4-C** | Redis 도입 (토큰 저장만 — 옵션 A. 날씨/Pub-Sub은 Phase 5 사용처와 함께) | **2026-05-02** | ✅ |
| 라이더 정리 | mmg-rider-service 사전 설계 (서비스 경계 / 데이터 모델 / 통신 패턴 / 상태 머신 / 위치 추적 / Pub-Sub 활용 ADR + 인터페이스 명세) | — | 4-C 후 정상 |
| 5 | 신규 기능 (펫/룰렛/챗봇/SSE/Rider/Admin) + TossPaymentClient | — | 라이더 정리 후 정상 |
| 학원 발표 | 최종 마일스톤 | — | Phase 5 종결 후 |
| 4-D | 외부 FK 정합성 (Saga/Outbox) | — | Phase 5 후 또는 병행 |
| 6 | 고도화 (Outbox/Kafka/모니터링/CI-CD) | — | Phase 5 후 또는 병행 |

---

## 영속성 전환 결과 (Phase 3 전체)

| 도메인 | 처리 | Phase |
|---|---|---|
| **auth.User** | ✅ JPA + UserMapper 완전 제거 | 3-A |
| **main.Payment** | ✅ INSERT JPA / existsByOrderId MyBatis 잔존 | 3-B-1 |
| **main.LikedStore** | ✅ JPA `@IdClass` 복합 PK / favoriteList(JOIN+LIMIT) MyBatis 잔존 | 3-B-2 |
| **main.Cart + CartDetail** | ✅ JPA 11 + MyBatis 잔존 3 (JOIN/Store경계) | 3-B-3 |
| **main.Order + OrderDetail** | ✅ JPA `Persistable<Long>` manual ID + MyBatis 잔존 3 (복잡 영구) | 3-C-1 |
| **main.Review** | ✅ postReview JPA + 🎯 **BaseEntity 첫 검증** (`@AttributeOverride` write_at/amended_at) / 9 SQL MyBatis 잔존 | 3-C-2 |
| **main.UserAddress** | ✅ JPA + UserAddressMapper/Address.xml 완전 제거 (`@JdbcTypeCode(NUMERIC)`) | 3-D-B |
| **main.Store / main.Owner** | 🔒 **MyBatis 영구 유지** (Store 12 + Owner 24 SQL — 복잡/동적/cross-table) | 3-D 확정 |

→ **8 도메인 JPA 전환 + 2 도메인 MyBatis 영구**. mybatis-spring-boot-starter는 영구 유지 (하이브리드 공존).

---

## 4개 schema DB 분배

| Schema | 테이블 | 행수 | 용도 |
|---|---|---|---|
| **my_mmg_auth** | user (1개) | 15 | 회원/JWT |
| **my_mmg_main** | store, store_category, menu, menu_category, category, likedstore, cart, cart_detail, orders, order_detail, payment, review, review_reply, **address** (14개) | ~452 | 가게/메뉴/주문/리뷰/결제/주소 |
| **my_mmg_rider** | rider, delivery, delivery_log (3개) | 0 | Phase 5-R1-A/R2-a/b 적용 (2026-05-06) — Q-DB (다) → (가) 전환, validate PASS |
| **my_mmg_admin** | (빈 schema) | 0 | Phase 5에 FAQ, penalty 등 |

**외부 FK 5개 DROP**: store/likedstore/cart/orders/review_reply의 user 참조 (Saga/Outbox는 Phase 4-D/6).

---

## 5개 서비스 + Gateway 상태 (Phase 4-B 후)

| 서비스 | 포트 | DB | Gateway 라우팅 |
|---|---|---|---|
| mmg-auth-service | 8081 | my_mmg_auth | `/api/user/**`, `/api/policy/**` |
| mmg-main-service | 8080 | my_mmg_main | `/api/store/**`, `/api/cart/**`, `/api/order/**`, `/api/payment/**`, `/api/address/**`, `/api/owner/**`, `/api/user/review/**`, `/uploads/**` |
| mmg-rider-service | 8082 | (없음) | `/api/rider/**` (Phase 5) |
| mmg-admin-service | 8083 | (없음) | `/api/admin/**` (Phase 5) |
| **mmg-gateway** | **8000** | — | **단일 진입점** + InternalBlockController 403(`/internal/**`) + GatewayCorsConfig (env) |

---

## 적용된 핵심 디자인 결정 (누적)

### 어제까지 (Phase 0~4-A, 2026-04-28)
1. **ERD = source of truth** (CLAUDE.md §6.11) — Phase 1-B-3 user_address 잘못 분배 사건 계기로 명문화
2. **회원가입 BFF 옵션 D-1** — POST /api/user/join이 즉시 AT/RT + userNo 반환 → 프론트가 곧장 POST /api/address
3. **cross-schema read** = Feign + batch (`/internal/auth/users?ids=`) for N+1 회피
4. **mmg-common 빈 활성화** = `scanBasePackages` + `@ConditionalOnClass(SecurityFilterChain)` 패턴
5. **컬럼 ERD 따름** = lat→latitude, lng→longitude, address_detail VARCHAR(200)
6. **TOSS_SECRET_KEY 환경변수화** (CLAUDE.md §6.10 보안)

### 오늘 추가 (Phase 3-A/3-B/3-C/3-D/4-B, 2026-04-29)
7. **JPA + MyBatis 하이브리드 영구 공존** — 단순 CRUD = JPA / 복잡 = MyBatis. mybatis-spring-boot-starter 영구 유지
8. **`saveAndFlush` 하이브리드 가시화** — 같은 트랜잭션 내 JPA INSERT 후 MyBatis JOIN SELECT가 즉시 보이도록 영속성 컨텍스트 동기화
9. **MyBatis Mapper 보존 정책 + 예외** — 사용처 0이어도 즉시 삭제 X. 예외 = `save()`로 완전 대체되는 SQL (`getLastCartId`)
10. **JPA Entity 매핑 패턴** — `@IdClass`(LikedStore), `Persistable<Long>`(Orders manual ID), `@AttributeOverride`(Review write_at/amended_at), `@JdbcTypeCode(NUMERIC)`(Address DECIMAL↔Double), `AttributeConverter`(User birth DATE↔String), 백틱(`rank`)
11. **`ddl-auto=validate` 고정** + `MariaDBDialect` 명시 (학원 DB metadata 자동감지 실패 회피)
12. **응답 스펙 동결 검증** — `SnapshotAssert`(JSONAssert STRICT) + `@SpringBootTest+MockMvc+@Transactional+@Rollback`. 학원 공유 DB 영향 0
13. **Gateway 라우트 순서 매칭** — prefix 충돌(`/api/user/review/**` vs `/api/user/**`)은 라우트 정의 순서로 해결
14. **`/internal/**` 외부 차단** — Gateway InternalBlockController 403 + 서비스 간 통신은 Feign(직접 포트, Gateway 우회)
15. **CORS Gateway 단일 처리** — 각 서비스 CORS는 이중 안전 (Phase 5 정리 검토)
16. **JWT 검증 위치** — 각 서비스 SecurityConfig 유지 (Gateway는 라우팅+Internal+CORS만, 커스텀 GatewayFilter 0)

---

## 검증 통과 흔적

| 시점 | 검증 | 결과 |
|---|---|---|
| Phase 0-B | 5 hello world + Gateway 라우팅 | ✅ |
| Phase 1-B-3+4 | BFF 흐름 7/7 | ✅ |
| Phase 1.5 | 5 에러 시나리오 (401/401/409/200/400) | ✅ |
| Phase 2-G | 통합 검증 7/7 (auth+main+gateway 동시 기동) | ✅ |
| Phase 4-A | Feign 4/4 (Internal 단건/batch + 가게 상세 + 리뷰 batch 합성) | ✅ |
| **Phase 3-A** | 9 endpoint 수동 검증 (BFF 회원가입/로그인/me/getUser/updateUser/internal 단건+batch+404) | ✅ 9/9 |
| **Phase 3-B/C/D** | 21 통합 테스트 + STRICT snapshot 13개 (JSON 1바이트 동결) | ✅ 21/21 |
| **Phase 4-B** | Gateway 경유 7 + Internal 차단 2 + CORS preflight + 미정의 경로 + 직접 호출 비교 + Phase 3 21 회귀 | ✅ 12/12 + 21/21 |

---

## 짚어둘 / 미해결 (Phase 별 계획됨)

| 항목 | 처리 시점 |
|---|---|
| user.status 컬럼 없음 (admin 승인 흐름) | Phase 5 |
| 외부 FK 정합성 (사용자 탈퇴 cleanup) | Phase 4-D 또는 6 |
| `/internal/**` mTLS / service-to-service token | Phase 6 |
| owner_comment 테이블 (ERD vs 코드 불일치) | Phase 5 결정 |
| OwnerInfoDto 분리 (현재 UserBriefDto 재사용) | Phase 5 |
| TOSS_SECRET_KEY placeholder + TossPaymentClient 추출 (HttpURLConnection → RestTemplate/WebClient) | Phase 5 결제 본격화 |
| CartMapper.findStoreNameByStoreId Store 도메인 경계 | Phase 5 (Store 본격 정리 시) |
| Owner.xml 24 SQL QueryDSL 재평가 | Phase 5 사장 페이지 본격 |
| 각 서비스 BaseSecurityConfig CORS 제거 (Gateway 단일화) | Phase 5 |
| review.amended_at INSERT 시 채움 (JPA 동작 vs MyBatis NULL 잔존) | 운영 영향 X (응답 노출 X) — 모니터링만 |

---

## Git 상태

- 브랜치: **develop** (Phase 0~4-B 모든 commit + push 완료)
- GitHub: https://github.com/momoolggo/momoolggo2-be/tree/develop
- 최신 commit: `4a450d6 docs: record Phase 4-B decisions — Gateway routing + Internal block + CORS`

---

## 테스트 백필 계획 (옵션 1 — 전체 백필 후 Phase 5)

> 배경: Phase 1 리뷰에서 단위/통합 테스트 **0개** 발견 (`mmg-common`/`mmg-auth-service` 모두). CLAUDE.md §6.5 신설(2026-04-29) — 가짜 테스트 금지 + 최소 커버리지 + DoD.
> 각 단계는 백필 → `@code-reviewer` 검증 → PASS 받아야 다음 Phase 진입.

- [x] **Phase 1 백필 (2026-04-29)** — Critical 3건 수정 + 테스트 42개 작성, 42/42 PASS
- [x] **Phase 2-Backfill-A (2026-04-29)** — Critical-1/2 수정 + Order/Payment 단위 테스트 13개, 34/34 PASS
- [x] **Phase 2-Backfill-B (2026-04-29)** — OrderService 단위 테스트 9 + Payment 통합 fixture 전환 + dead code 1건 제거, 43/43 PASS
- [x] **Phase 2-Backfill-C (2026-04-30)** — ReviewService 11 + CartService 16 + UserAddressService 2 = 단위 29 추가, 72/72 PASS
- [x] **Phase 2-Backfill-D (2026-04-30)** — Owner 18 + Store Feign null fix 5 + AddressSearch refactor 8 + Cart 권한 5+통합 4 + UserAddress 권한 2 = 신규 42 추가, 116/116 PASS
- [x] **Phase 2-Backfill-D-bis (2026-04-30)** — OwnerService 17개 메서드 권한 분기 일괄 + dto.userId 위조 방지 + Mapper 헬퍼 4개 + 32 신규 케이스, 148/148 PASS (Phase 2 백필 종료)
- [x] **Phase 3-Backfill-A (2026-04-30)** — Critical 4 + Major 1 일괄 처리 (Order DELETE 인증 / Favorite 위조 방지 / Order 내역 인증 / Feign null 패턴 전파 / UserAddress.update 권한), 19 신규 케이스, **167/167 PASS**
- [x] **Phase 3-Backfill-B + W-A1 (2026-05-02)** — readOnly 24건 + Owner 쓰기 `@Transactional` 8건 + Review 통합 2 + UserAddress save/setDefault 단위 5 + System.out 잔존 정리, **174/174 PASS**
- [x] **Phase 3-Backfill-C (2026-05-02)** — `!=` → `Objects.equals()` 잔존 10곳 전수 통일 (W-A1 6곳에서 누락된 primitive long 비교 4곳 마저 + storeSearchList 단위 2 + wishToggle delete 분기 단위 1, **177/177 PASS** — Phase 3 백필 종결)
- [x] Phase 3 백필 → `@code-reviewer` 검증 → PASS (Phase 3-Backfill-B 시점)
- [x] **Phase 4-A 백필 (2026-05-02)** — InternalUserController 통합 6 + UserBriefDto snapshot 1 + OwnerService null 가드 + 단위 1 + readOnly 3건 + Feign timeout yml. **219 → 227 (8 신규)**, code-reviewer **CONDITIONAL PASS** (Critical 0, Warning 2 = 스타일/커버리지 갭). W-2 의미 분리 (null 가드는 처리 완료 / 예외 fallback은 Phase 5 잔존).
- [x] **Phase 4-B 백필 (2026-05-02)** — Gateway 첫 통합 테스트 도입(WebMVC 5.0.1) + InternalBlock 12건 + 라우트 정의 12개 동결 + CORS preflight/404 + CORS trim 시한폭탄 즉시 처리 + HelloController dead 정리. **227 → 256 (gateway 29 신규)**, code-reviewer **PASS** (Critical 0, Warning 2 = cosmetic). Step 3 가정 정정(헤더 검증/mTLS는 Phase 6 작업 — 4-B는 헤더 무관 무조건 403 동결).
- [x] **Phase 4-C 신규 기능 (2026-05-02)** — Redis 토큰 저장(RT revoke) 단일 목적: spring-boot-starter-data-redis 의존성 + docker-compose redis + RefreshTokenStore 인터페이스/Redis 구현 + UserService.signup/signin/reissue/signout 통합. 옵션 A(날씨/Pub-Sub은 Phase 5와 함께) + D1 throw / D1-bis best-effort / D2 분리 호출 / D3 auth-service 위치 / D4 RT 문자열 자체. **256 → 266 (auth 신규 10)**, code-reviewer **PASS** (Critical 0, Warning 3 = cosmetic 발표 전 확인). 0단계 .env.example placeholder 정리 별도 PR.
- [ ] Phase 4 백필 → `@code-reviewer` 검증 → PASS
- [ ] 전체 종합 리뷰 → 라이더(Phase 5) 진입 승인

### Phase 1 백필 결과 (2026-04-29)

**Critical 버그 수정 (Step 1)**:
| 항목 | 커밋 |
|---|---|
| Step 1-A: reissue JwtException → 401 + 레이어 정리 (Controller→Service 위임, GlobalExceptionHandler에 @ExceptionHandler(JwtException) 추가) | `550e824` |
| Step 1-B: UserUpdateReq.gender `int` → `Integer` (미전송과 0 구분) | `3b06047` |
| Step 1-C: 조회 메서드 `@Transactional(readOnly=true)` (checkId/signin/getUser) | `3e28474` |

**테스트 작성 (Step 2) — 42개 / 0 failures / 0 errors**:
| 모듈/클래스 | 케이스 수 | 커밋 |
|---|---|---|
| `mmg-common` JwtTokenProviderTest (Generate 3 + Verify 5) | **8** | `803182f` |
| `mmg-auth-service` UserServiceTest (Mockito 단위, 7개 Nested 도메인) | **20** | `3519872` |
| `mmg-auth-service` UserControllerTest (standalone, 8개 Nested 엔드포인트) | **14** | `61d6b4d` |

**테스트 인프라**:
- `mmg-common`/`mmg-auth-service` 둘 다 `spring-boot-starter-test` + `useJUnitPlatform()` 추가
- `mmg-common` test에 starter-web/security 노출 (production이 compileOnly로 잡은 Jackson 3 / SecurityFilterChain을 test에서 사용)
- `mmg-auth-service` test에 spring-security-test 추가

**검증 방식 (CLAUDE.md §6.5 준수)**:
- assertNotNull 단독 사용 0건
- 반환값 / 예외 타입+메시지+status / Mockito verify(호출/미호출) / ArgumentCaptor(저장된 User 필드)
- 학원 공유 DB 의존 0 — Service는 Mock, Controller는 standaloneSetup. 실 DB 영향 없음.

**Step 1-A·1-B 수정 검증 핵심 케이스**:
- `Reissue.rtExpired_propagatesJwtException` (Service) + `Reissue.rtExpired_returns401_notFiveHundred` (Controller): RT 만료 시 500 아닌 **401** 응답 동결
- `UpdateUser.genderZero_actuallyChanges` (Service) + `UpdateUser.genderOmitted_isNull` (Controller): gender Integer 타입 — 0 명시와 미전송 구분 동결

### Phase 2-Backfill-A 결과 (2026-04-29)

**Critical 코드 수정**:
| 항목 | 커밋 |
|---|---|
| Critical-1: `calSumOrder` 파라미터 orderId → storeId (deleteOrder 후 서브쿼리 empty 버그) — Order.xml + OrderMapper + OrderService(placeOrder/deleteOrder) + OrderController | `6d75c61` |
| Critical-2: `confirmPayment` 흐름 재구성 (주문검증 → 토스호출 → 결제저장 → 주문상태 → 장바구니정리) + `callTossConfirm` protected 추출 | `6565841` |
| order-delete-not-found.json 임시 문자열 `"ㅇㅇ"` → `null` | `6e1aa68` |

**테스트 작성 — 13개 / 0 failures / 0 errors**:
| 모듈/클래스 | 케이스 수 | 커밋 |
|---|---|---|
| `mmg-main-service` OrderServiceCalSumOrderTest (placeOrder 3 + deleteOrder 3) | **6** | `e500e60` |
| `mmg-main-service` PaymentServiceTest (OrderValidation 3 + TossCall 1 + HappyPath 3, Spy 패턴) | **7** | `c3f24d5` |

**main-service 전체 빌드/테스트 통과 (34/34)**:
- 기존 통합: UserAddress 4 / Cart 5 / Order 3 / Payment 3 / Review 2 / LikedStore 4 = 21
- 신규 단위: 13
- 합계 34 — 0 failures / 0 errors / `:mmg-main-service:build` SUCCESSFUL

**Step 1-B/2-B 수정 검증 핵심 케이스**:
- `OrderServiceCalSumOrderTest.PlaceOrder.calSumOrder_calledWithCartStoreId`: orderId 아닌 **storeId(STORE_ID)** 전달 동결
- `OrderServiceCalSumOrderTest.DeleteOrder.deleteSuccess_calSumOrderCalledWithStoreId`: findById → delete → calSumOrder(STORE_ID) 호출 순서 verify
- `PaymentServiceTest.HappyPath.saveFails_subsequentStepsSkipped`: save 예외 시 order.payState=1 유지 + cartRepository.findByUserNo 미호출 (흐름 동결)

### Phase 2-Backfill-B 결과 (2026-04-29)

**dead code 정리 + OrderService 단위 테스트 + Payment fixture 전환**:
| 항목 | 커밋 |
|---|---|
| OrderService.calSumOrder(long storeId) public 메서드 제거 (사용처 0건 dead code) | `de85d58` |
| OrderService.getOrderInfo 단위 테스트 (4) — Feign+cartMapper+주소 합성 + short-circuit | `7a467f1` |
| OrderService.getOrderHistory 단위 테스트 (2) — items 합성 N회 verify | `bb65cfe` |
| OrderService.orderHistoryDetail + maxHistoryPage 단위 테스트 (3) | `877d316` |
| PaymentControllerIntegrationTest fixture 패턴 전환 (학원 DB row 하드코딩 제거) | `2f6b093` |

**main-service 전체 빌드/테스트 통과 (43/43)**:
| 분류 | 케이스 수 |
|---|---|
| 기존 통합: UserAddress 4 / Cart 5 / Order 3 / Payment 3 (fixture로 전환됨) / Review 2 / LikedStore 4 | 21 |
| 단위 (Phase 2-A): OrderServiceCalSumOrderTest 6 + PaymentServiceTest 7 | 13 |
| 단위 (Phase 2-B): OrderServiceTest 9 (getOrderInfo 4 + getOrderHistory 2 + orderHistoryDetail 1 + maxHistoryPage 2) | 9 |
| **합계** | **43** |

**Phase 2-Backfill-B 핵심 검증 케이스**:
- `OrderServiceTest.GetOrderInfo.noCart_throwsAndShortCircuits`: cart 없을 때 Feign/주소 조회 모두 미호출 (verifyNoInteractions) — 불필요한 외부 호출 동결
- `OrderServiceTest.MaxHistoryPage.delegatesToCountByUserNo`: `verifyNoInteractions(orderMapper)` — MyBatis → JPA 전환 결과 동결 (회귀 시 즉시 검출)
- `PaymentControllerIntegrationTest.insertOrder` 헬퍼: Phase 2-A에서 user FK는 DROP했지만 store_id FK는 살아있음 — 학원 DB 실존 store_id=21 사용으로 통합 테스트 안정화

### Phase 2-Backfill-C 결과 (2026-04-30)

**테스트 작성 — 29개 / 0 failures / 0 errors / 5 커밋**:
| 모듈/클래스 | 케이스 수 | 커밋 |
|---|---|---|
| `mmg-main-service` ReviewServiceTest.PostReview (happy + 403 + 409) | **3** | `ba284a7` |
| `mmg-main-service` ReviewServiceTest.GetReviews + GetReviewById (happy/zeroCount + happy/404) | **4** | `aded988` |
| `mmg-main-service` ReviewServiceTest.DeleteReview + UpdateReview (happy InOrder + 403) | **4** | `0ca5578` |
| `mmg-main-service` CartServiceTest (GetCart 2 + AddToCart 5 + ClearAndAddToCart 2 + UpdateCartItem 2 + DeleteCartItem 3 + ClearCart 2) | **16** | `f6a15ab` |
| `mmg-main-service` UserAddressServiceTest.Delete (happy + Repository 예외 propagate) | **2** | `d21834d` |

**main-service 전체 빌드/테스트 통과 (72/72)**:
| 분류 | 케이스 수 |
|---|---|
| 기존 통합: UserAddress 4 / Cart 5 / Order 3 / Payment 3 / Review 2 / LikedStore 4 | 21 |
| 단위 (Phase 2-A): OrderServiceCalSumOrderTest 6 + PaymentServiceTest 7 | 13 |
| 단위 (Phase 2-B): OrderServiceTest 9 | 9 |
| 단위 (Phase 2-C): ReviewServiceTest 11 + CartServiceTest 16 + UserAddressServiceTest 2 | 29 |
| **합계** | **72** |

**Phase 2-Backfill-C 핵심 검증 케이스**:
- `ReviewServiceTest.PostReview.otherUser_throwsForbiddenAndShortCircuits`: `checkReviewWriter` 불일치 시 `BusinessException FORBIDDEN` + `saveAndFlush` 미호출 동결 — 권한 분기 회귀 검출
- `ReviewServiceTest.PostReview.duplicateReview_throwsConflict`: `DataIntegrityViolationException` → `BusinessException CONFLICT` 변환 + `updateStoreRating` 미호출 동결
- `ReviewServiceTest.DeleteReview.happyPath_deletesAndUpdatesRating`: `InOrder`로 findStoreId → delete → updateRating **호출 순서 동결** (storeId 캐싱 분기 회귀 방지)
- `CartServiceTest`: 권한 분기 부재를 클래스/메서드 주석으로 명시 동결 — Phase 2-Backfill-D에서 권한 분기 추가 시 본 테스트 갱신 필요
- `UserAddressServiceTest.Delete`: `verifyNoMoreInteractions`로 `deleteById` 외 모든 호출 부재 동결 — userNo 파라미터 + 권한 분기 추가는 D 단계 작업

**진단/결정 (Step 1)**:
- CartService.updateCartItem/deleteCartItem 및 UserAddressService.delete에 **userNo 파라미터 부재 + 권한 검증 분기 부재** 발견 (보안 부채)
- 옵션 C 채택: 현재 동작 동결 + tech-debt.md "예정된 작업" 섹션에 Phase 2-Backfill-D 항목으로 등록 (권한 분기 추가는 라이더 진입 전 D 단계에서 처리)

### Phase 2-Backfill-D 결과 (2026-04-30)

**신규 코드 변경 + 테스트 작성 — 42개 신규 / 0 failures / 0 errors / 11 커밋**:

| 단계 | 내용 | 케이스 | 커밋 |
|---|---|---|---|
| D-1-B (가게) | OwnerService.registerStore + updateStore + deleteStore 단위 | **6** | `481c3cc` |
| D-1-B (Feign) | OwnerService.getOrders 단위 (Feign batch + 합성 + 예외 propagate) | **4** | `b263168` |
| D-1-B (주문) | OwnerService.updateOrderState + deleteOrder 단위 (InOrder) | **3** | `c167f57` |
| D-1-B (메뉴) | OwnerService.registerMenu + updateMenu + deleteMenu 단위 | **5** | `1ac986f` |
| D-2-A (fix) | StoreService.storeOneGet Feign null NPE 처리 (BusinessException NOT_FOUND) | — | `2102bb5` |
| D-2-A (test) | StoreServiceTest (5 시나리오: happy / store 없음 / ownerId null / Feign null / Feign 예외) | **5** | `bdb6c6e` |
| D-2-B (refactor) | RestTemplateConfig 신설 + AddressSearchService 싱글톤 주입 (connect 3s / read 5s) | — | `fb3b021` |
| D-2-B (test) | AddressSearchServiceTest 7 + RestTemplateConfigTest 1 (timeout 동결) | **8** | `b658378` |
| D-3-B (feat) | CartService cartItem 소유자 검증 추가 — 시그니처 변경 + Controller principal 주입 | (단위 5 신규) | `f35d1c9` |
| D-3-B (test) | CartIntegrationTest — UPDATE 회로 + 권한 통합 (Warning 1 해소) | **4** | `f4b810b` |
| D-4-B (feat) | UserAddressService.delete 소유자 검증 추가 — 시그니처 + Controller principal | — | `890e3ad` |
| D-4-B (test) | UserAddressServiceTest 4 케이스 (happy / 403 / 404 / null userNo 방어적) | (2 신규) | `5621730` |

**main-service 전체 빌드/테스트 통과 (116/116)**:
| 분류 | 케이스 수 |
|---|---|
| 기존 통합 | 21 |
| 단위 (Phase 2-A): OrderServiceCalSumOrderTest 6 + PaymentServiceTest 7 | 13 |
| 단위 (Phase 2-B): OrderServiceTest 9 | 9 |
| 단위 (Phase 2-C): ReviewServiceTest 11 + CartServiceTest 16 + UserAddressServiceTest 2 | 29 |
| 단위 (Phase 2-D): OwnerServiceTest 18 + StoreServiceTest 5 + AddressSearchServiceTest 7 + RestTemplateConfigTest 1 + CartServiceTest 권한 5 추가 + UserAddressServiceTest 2 추가 | 38 |
| 통합 (Phase 2-D): CartIntegrationTest 4 | 4 |
| 시그니처 갱신 (Phase 2-D 영향): CartServiceTest 기존 13 동작 동결은 권한 검증 통합으로 흡수 | (포함) |
| **합계** | **116** |

**Phase 2-Backfill-D 핵심 검증 케이스**:
- `StoreServiceTest.feignNull_throwsNotFound`: Feign 응답 null 시 `owner.getName()` NPE 차단 검증 — 직렬화/디코더 변경 회귀 방지
- `AddressSearchServiceTest.localException_fallsBackToGeocoding`: 지역검색 timeout/예외 시 search() 전체가 깨지지 않고 Geocoding fallback 회로 동결
- `RestTemplateConfigTest.restTemplate_hasConfiguredTimeouts`: connect 3s / read 5s 동결 (Reflection으로 SimpleClientHttpRequestFactory private field 확인)
- `CartServiceTest.UpdateCartItem.otherUserCartItem_throwsForbidden`: cartId → Cart.userNo 조회 후 비교하는 *실제 권한 분기* 검증 (mock 우회 X)
- `CartIntegrationTest.update_byOwner_actuallyPersistsToDb`: JPA 영속성 컨텍스트에서 dirty checking → flush → DB row 갱신까지 동결 (Warning 1 해소)
- `CartIntegrationTest.update_byOtherUser_throwsForbiddenAndDbUnchanged`: 403 발생 시 DB row 미변경 (롤백 안전성)
- `UserAddressServiceTest.otherUserAddress_throwsForbiddenAndShortCircuits`: findById 후 userNo 비교 분기 + delete 미호출 동결

**진단/결정**:
- Owner 도메인은 14개 메서드 모두 권한 분기 부재 발견 — 옵션 C(현재 동작 동결 + D-bis로 분리) 채택
- CartService/UserAddressService.delete는 D 단계에서 권한 분기 추가 완료
- StoreService.storeOneGet Feign null은 실제 발생 경로 없지만 방어적 코딩으로 BusinessException NOT_FOUND 처리
- AddressSearchService는 외부 API 무한 대기 위험 차단 (timeout 적용) — 라이더 진입 전 보안 부채 정리

### Phase 2-Backfill-D-bis 결과 (2026-04-30) — Phase 2 백필 종료

**Owner 17개 메서드 권한 분기 일괄 추가 — 32 신규 / 0 failures / 0 errors / 11 커밋**:

| 그룹 | 메서드 수 | 신규 케이스 | feat 커밋 | test 커밋 |
|---|---|---|---|---|
| 사전 인프라 | Mapper 헬퍼 4개 + Service verify 4개 | — | `a0ba8a2` | — |
| ㅁ 카테고리 | 4 | 10 | `1e27ead` | `20aa3ec` |
| ㄷ 메뉴 | 4 | 10 (기존 5 갱신 + 5 신규) | `b61d5d8` | `78f958b` |
| ㄹ 매출 | 2 | 4 | `0d1cda6` | `b6ebd07` |
| ㄱ 가게 | 6 (registerStore 위조 방지 + Long.parseLong) | 14 (기존 6 갱신 + 8 신규) | `aa65c86` | `8963d58` |
| ㄴ 주문 | 3 | 11 (기존 3 갱신 + 8 신규) | `579fef3` | `9f5cc3f` |

**main-service 전체 빌드/테스트 통과 (148/148)**:
- Phase 2-D 누적 116 + D-bis 신규 32 = **148**
- Owner 권한 검증 SQL 4개 신설 (store/orders/menu/menu_category JOIN)
- 통합 테스트 영향 0 (OwnerService는 다른 서비스에서 호출 안 함)

**핵심 검증 케이스**:
- `RegisterStore.dtoUserIdMismatch_throwsForbidden`: 프론트가 dto.userId 위조 시도 시 FORBIDDEN. 옵션 B 적용 — 명시적 거부.
- `UpdateStore.invalidStoreIdFormat_throwsBadRequest`: String storeId의 비숫자 입력 → BAD_REQUEST. dto 타입 정리는 tech-debt 등재 (프론트 협의 필요).
- `DeleteOrder.deletesInOrder`: InOrder로 `findStoreOwnerByOrderId → deleteOrderDetail → deleteOrder` 순서 동결.
- `UpdateMenu.otherOwnerMenu_throwsForbiddenAndShortCircuits`: 다른 점주 메뉴 수정 시도 시 권한 검증 → FORBIDDEN + Mapper 미호출. 메시지 "본인 가게의 메뉴만 접근 가능합니다."
- `GetOrders.otherOwnerStore_throwsForbiddenAndShortCircuits`: 권한 미통과 시 Feign batch 호출 미발생 (불필요한 외부 호출 차단).

**진단/결정**:
- 그룹 진행 순서 ㅁ → ㄷ → ㄹ → ㄱ → ㄴ (단순 → 복잡, 위험한 것 마지막)
- 프론트 영향 LOW 확정 (`AddStoreView.vue`의 dto.userId 자동 일치 검증)
- uploadImage 권한 추가 제외 OK (Controller hasRole("OWNER") 1차 필터 + multipart 10MB 제한 적용)
- Owner 도메인은 다른 서비스에서 호출 안 함 → 통합 테스트 영향 0

### Phase 3-Backfill-A 결과 (2026-04-30)

**Phase 3 진단에서 발견된 Critical 4 + Major 1 일괄 처리 — 19 신규 / 0 failures / 0 errors / 11 커밋**:

| Step | 내용 | 신규 케이스 | 커밋 |
|---|---|---|---|
| A-1 | Order DELETE 인증 (`OrderService.deleteOrder` callerUserNo + 소유자 검증, 응답 스펙 동결) | 1 (3 갱신) | `08a4a28` `e57587b` |
| A-2 | Favorite dto.userNo 위조 방지 (`wishToggle/checkWish/wishListGet` 옵션 B) + System.out 1건 제거 | 6 | `6a284c0` `6979ce4` |
| A-3 | Order 내역 인증 (`getOrderHistory/orderHistoryDetail/maxHistoryPage` 권한) + System.out 제거 | 5 (4 갱신) | `54a267b` `9ebc82f` |
| A-4 | Feign null 패턴 전파 (`OrderService.getOrderInfo` getUser null + `StoreService.getStoreReviews` batch null) | 4 | `6d6cc14` `361ce00` `2011b90` |
| A-5 | UserAddressService.update 소유자 검증 (D 단계 일관성) | 4 | `5253bef` `ca78b7a` |

**main-service 전체 빌드/테스트 통과 (167/167)**:
- Phase 2 누적 148 + A 신규 19 = **167**

**Phase 3-Backfill-A 핵심 검증 케이스**:
- `OrderServiceCalSumOrderTest.DeleteOrder.otherUserOrder_throwsForbiddenAndShortCircuits`: 다른 사용자 미결제 주문 삭제 시도 → FORBIDDEN + 삭제/calSumOrder 미호출 (보안 + InOrder 동결 양립)
- `StoreServiceTest.WishToggle.dtoUserNoMismatch_throwsForbidden`: D-bis registerStore 패턴 그대로 — 명시적 403 throw, 조용히 덮어쓰기 X
- `OrderServiceTest.GetOrderInfo.feignNull_throwsNotFound`: Phase 2-D StoreService.storeOneGet 패턴이 Order로 전파됐는지 동결 (BusinessException NOT_FOUND + 후속 미호출)
- `StoreServiceTest.GetStoreReviews.feignNull_userNamesAreBlank`: batch null이면 빈 Map → review의 userName 빈 문자열 fallback (NPE 차단). Owner.getOrders와 의도적 다른 결정 — 도메인 critical 차이.
- `UserAddressServiceTest.Update.otherUserAddress_throwsForbiddenAndShortCircuits`: D-4 delete에만 있던 소유자 검증을 update에도 일관 적용 (delete 패턴 복붙)

**진단/결정**:
- 응답 스펙 동결: deleteOrder 미존재 orderId는 기존 동작 유지 (return 0 → "삭제실패" 200) — CLAUDE.md §6 규칙 7 준수
- Owner.getOrders Feign 예외는 명시적 propagate 동결 (W-2 — Phase 5 fallback 작업), Store.getStoreReviews는 fallback 동결 — 도메인 critical 차이로 분기
- A-5는 D-4 누락분 일관성 보강 — Major이지만 백필 자연스러운 위치

### Phase 3-Backfill-B + W-A1 결과 (2026-05-02)

**W-A1 + B-1~B-4 완료 — 신규 7 케이스 / 32건 트랜잭션 어노테이션 / main-service System.out 0건 / 12 커밋**:

| Step | 내용 | 신규 케이스 / 어노테이션 | 커밋 |
|---|---|---|---|
| W-A1 | 권한 비교 `Objects.equals()` **6곳 통일** (Long != long / .equals() / null 가드 혼용 제거 — null-safe 단일 패턴) — *primitive `long != long` 4곳은 누락(스타일 통일 가치 미판정) → C에서 전수 종결* | 0 신규 (167 PASS 유지) | `e95cf97` `ef9a097` `aefb576` |
| B-1 | 조회 메서드 `@Transactional(readOnly=true)` 적용 (24건, 6도메인) + Owner 쓰기 `@Transactional` 추가 (8건, 데이터 정합성 부채 즉시 처리) | 0 신규 (어노테이션 32건) | `38cff0b` `3db1076` `8fe7e7b` `3bf15d0` `6024407` `207996d` `11422c3` |
| B-2 | Review 통합 happy path (`postReview` / `deleteReview` — Service 호출 + JPQL/findById 재조회 검증) | 2 | `dca7c02` |
| B-3 | UserAddressService.save / setDefault 단위 테스트 (defaultAd 분기 3 + setDefault InOrder + NOT_FOUND) | 5 | `8ffba23` |
| B-4 | StoreController.StoreListGet `System.out.println` 제거 (Phase 3-Backfill-A 후속 잔존 1건 정리 — main-service 내 0건 도달) | 0 신규 | `ad63030` |

**main-service 전체 빌드/테스트 통과 (174/174)**:
- Phase 3-Backfill-A 누적 167 + B 신규 7 = **174**

**Phase 3-Backfill-B 핵심 검증 케이스**:
- `ReviewControllerIntegrationTest.postReview_happy`: BaseEntity Auditing + Service 통합 흐름 동결 — JPQL로 `Review WHERE orderId = :oid` 재조회 → rating/contents/photo + write_at/amended_at 채움 검증
- `ReviewControllerIntegrationTest.deleteReview_happy`: `entityManager.flush() + clear()` 후 `findById` empty 동결 (1차 캐시 우회)
- `UserAddressServiceTest.Save.defaultAd1_resetsThenSaves`: `InOrder` + `ArgumentCaptor`로 `resetDefault → flush → save` 순서 + 저장 entity 6필드 동결
- `UserAddressServiceTest.SetDefault.happyPath_setsDefaultViaDirtyChecking`: dirty checking 검증 — Mock entity의 `getDefaultAd()`가 1로 변경됐는지 직접 확인
- `UserAddressServiceTest.SetDefault.addressNotFound_throwsNotFound`: `resetDefault/flush` 호출 후 NOT_FOUND throw — 트랜잭션 롤백 의존 동작 명시 동결

**진단/결정**:
- W-A1 메모리 정정: 기존 메모는 "Cart/UserAddress = .equals() 패턴"이라고 기록했으나 실제로는 1곳(CartService:147)만 .equals()였고 나머지는 모두 `Long != long`이었음. `Objects.equals()` 단일 표준으로 통일 + `feedback_owner_check_pattern.md` 신설.
- OwnerService 쓰기 `@Transactional` 누락은 발견 시점에 *즉시 처리* (tech-debt 등재 X). `registerStore` 3 INSERT 부분 실패 위험을 미루지 않음. 영향 분석으로 OwnerServiceTest는 Mockito 단위 — 트랜잭션 동작 무관 확인.
- `System.out.println`은 Phase 3-Backfill-A에서 이미 2건(Favorite/Order) 제거됐고, 잔존 1건도 Logger 전환 가치 없는 디버그 잔재 → 단순 제거.

### Phase 3-Backfill-C 결과 (2026-05-02) — Phase 3 백필 종결

**`!=` 전수 통일 + 단위 3건 / 0 failures / 0 errors / 6 커밋**:

| Step | 내용 | 신규 케이스 / 변경 | 커밋 |
|---|---|---|---|
| C-1a | OwnerService 잔존 `!=` 5곳 → `Objects.equals()` (registerStore + verifyStore/Order/Menu/Category 헬퍼 4) | 0 신규 (스타일 통일) | `682a240` |
| C-1b | StoreService 잔존 `!=` 3곳 → `Objects.equals()` (wishToggle/checkWish/getWishListResponse) | 0 신규 | `a136582` |
| C-1c | OrderService 잔존 `!=` 2곳 → `Objects.equals()` (getOrderHistory/maxHistoryPage) | 0 신규 | `8a62d48` |
| C-2 | StoreService.storeSearchList null/blank early return 단위 동결 (verifyNoInteractions) | 2 | `fe8db01` |
| C-3 | StoreService.wishToggle delete 분기 단위 (deleteByUserNoAndStoreId verify + saveAndFlush 미호출) | 1 | `eced681` |

**main-service 전체 빌드/테스트 통과 (177/177)**:
- Phase 3-Backfill-B 누적 174 + C 신규 3 = **177**

**Phase 3-Backfill-C 핵심 검증 케이스**:
- `StoreServiceTest.StoreSearchList.nullSearchText_returnsEmptyAndSkipsMapper`: null 입력 → `List.of()` + `verifyNoInteractions(storeMapper)` 동결 (NPE 차단 + 불필요 SQL 차단)
- `StoreServiceTest.StoreSearchList.blankSearchText_returnsEmptyAndSkipsMapper`: 공백만(`"   "`) 입력 시 `searchText.trim().isEmpty()` 분기 동결 — trim 효과 검증
- `StoreServiceTest.WishToggle.alreadyLiked_callsDeleteAndReturnsFalse`: existsByUserNoAndStoreId=true 분기에서 delete 호출 + saveAndFlush 미호출 검증 — toggle 양방향 단위 완성

**진단/결정**:
- W-A1 "6곳 통일" 기록의 부분성 정정: primitive `long != long` 4곳(스타일 차이만, 기능 동일)은 W-A1에서 의식적으로/암묵적으로 제외됐으나 결정 근거 미명시 → C에서 전수 통일 + 표준 메모(`feedback_owner_check_pattern.md`)에 "primitive 비교도 단일 표준" 명시
- autoboxing으로 동작 동일 → 위험 0, 비용 5분, 일관성 가치 큼 (다음 백필 레퍼런스)
- StoreService Mapper 1:1 위임 4개(storeListGet/getMaxPage/menuListGet/findNearbyStores) 단위는 *형식적 검증*이라 의도적 제외. NAJACKS 가짜 패턴 방지
- OwnerService.getOrders Feign null 가드는 W-2(Phase 5 fallback) 동결 유지 — 도메인 critical 차이 의도 보존 → **Phase 4-A 백필에서 W-2 의미 분리 후 즉시 처리**

### Phase 4-A 백필 결과 (2026-05-02) — Feign 인프라 동결

**코드 1 + 통합 6 + 단위 1 + snapshot 1 + readOnly 3 + yml — 8 신규 / 0 failures / 6 커밋**:

| Step | 내용 | 신규 / 변경 | 커밋 |
|---|---|---|---|
| 1-code | OwnerService.getOrders Feign null 가드 (A-4 패턴 전파 누락 정정) | 코드 1줄 | `d2f87ce` |
| 1-test | OwnerServiceTest.GetOrders.feignNull_customerFieldsNotSet 단위 1건 | 1 (177→178) | `39e43cd` |
| 2 | InternalUserController 통합 테스트 (provider 동결): getUser happy/404 + getOwner happy/404 + getUsers batch happy/partial | 6 (34→40) | `b562532` |
| 3 | UserBriefDto Jackson 직렬화 snapshot (consumer 보호 — 키 4개 동결 + 라운드트립) | 1 (8→9) | `05a2e2b` |
| 4 | InternalUserController 3 메서드 `@Transactional(readOnly=true)` 적용 (B-1 패턴 일관) | 0 신규 (어노테이션 3건) | `642bd89` |
| 5 | main-service Feign client timeout yml (60s → connect 3s / read 5s). auth는 의존성 부재로 미적용 | 설정 변경 | `7aec4d7` |

**전체 누적 (Phase 1~4-A 백필)**: **227 PASS** (common 9 + auth 40 + main 178)

**W-2 의미 분리 (Phase 4-A 핵심 결정)**:
- W-2 defer는 "Feign **예외** fallback (auth 다운 시 점주 화면 동작)" 결정이지 "**null NPE 가드**"가 아님. 두 개념 혼용 박제 정정.
- `null 가드`는 코드 버그(NPE 위험) — Phase 4-A에서 즉시 처리 (`d2f87ce` `39e43cd`).
- `예외 fallback`은 비즈니스 결정 — Phase 5 라이더 정리 또는 본격 작업 중 처리 (W-2 잔존).

**Phase 4-A 핵심 검증 케이스**:
- `InternalUserControllerIntegrationTest.getUser_happy_serializationFrozen`: 200 + JsonPath 4필드 동결 — Feign consumer 4곳이 의존하는 직렬화 형식
- `InternalUserControllerIntegrationTest.getUsers_batchHappy`: JsonPath filter(`$[?(@.userNo == N)]`)로 순서 비보장 검증 — 실제 batch 동작
- `InternalUserControllerIntegrationTest.getUsers_batchPartial`: 존재 + 미존재 → 존재만 반환 (consumer는 nameMap.getOrDefault로 fallback)
- `UserBriefDtoSerializationTest.serializationRoundtrip_freezesFieldNames`: JSON 키 개수 4 동결 — 필드 추가/제거 시 consumer 깨짐 회귀 검출
- `OwnerServiceTest.GetOrders.feignNull_customerFieldsNotSet`: null 응답 → 모든 row의 customerName/tel null fallback (`feignException_propagates`와 양립)

**진단/결정**:
- auth-service 통합 테스트 첫 도입: main-service 패턴(`@SpringBootTest+MockMvc+@Transactional+@Rollback+fixture INSERT`)을 그대로 적용. user 엔티티 fixture(UUID 8자 user_id)로 학원 DB 변경 0 보장.
- Feign timeout yml은 main-service에만 적용 — auth-service는 `spring-cloud-starter-openfeign` 의존성 없음(provider). dead config 회피.
- code-reviewer Warning 2건은 머지 차단 아님: W-1 yml flat key 표기(`spring.jpa:`와 일관, 빌드 통과), W-2 getUsers empty ids 통합 누락(코드는 정상, 테스트 커버리지 갭) — Phase 5 진입 전 보완 가능, tech-debt 등재.

### Phase 4-B 백필 결과 (2026-05-02) — Gateway 동작 종합 동결

**HelloController 정리 + Gateway 첫 통합 도입 + 4 @Nested 동결 + CORS trim 시한폭탄 — 29 신규 / 0 failures / 6 커밋**:

| Step | 내용 | 신규 / 변경 | 커밋 |
|---|---|---|---|
| 1 | HelloController 삭제 (Phase 0-B 잔재 — Gateway 외부 노출 가능했던 debug endpoint) | 코드 1 파일 삭제 | `a4488b8` |
| 2+3 | Gateway 통합 테스트 셋업(`@SpringBootTest+MockMvc.webAppContextSetup+@Autowired CorsFilter` — auth fixture 패턴 일관) + InternalBlock 동결: sub-path 5 / HTTP method 5 / 위조 헤더 1 / 대비 1 (Phase 6 헤더 검증/mTLS 전 동작 잠금) | 12 신규 | `8af4dfa` |
| 4 | 라우트 정의 12개 강한 동결: `@Autowired GatewayMvcProperties` + ParameterizedTest CsvSource 12 인덱스별 id/uri/Path 동결 + review-route 1번 위치 명시(prefix 충돌 회피) | 13 신규 | `08a226c` |
| 5a | CORS preflight + 404 동결: 2 preflight(allowed origin/credentials + 6 methods) + /api/nonexistent 404 | 3 신규 | `9d92347` |
| 5b | CORS allowedOrigins `.split(',')` trim 누락 시한폭탄 즉시 처리: `parseAllowedOrigins(raw)` 정적 메서드 추출 + map(trim) + filter(empty) | 코드 1 메서드 | `7e87567` |
| 5c | parseAllowedOrigins 단위 테스트: 단일/공백/연속콤마/빈 입력 6 assertion | 1 신규 | `f7f8bf6` |

**전체 누적**: **256 PASS** (common 9 + auth 40 + main 178 + gateway 29 신규)

**Phase 4-B 핵심 검증 케이스**:
- `GatewayIntegrationTest.InternalBlock.allSubPaths_returns403WithResultResponse`: ParameterizedTest 5 sub-path → 403 + resultMessage 동결
- `GatewayIntegrationTest.InternalBlock.forgedHeaders_returnSame403`: X-Internal/Authorization/X-Forwarded-For 주입에도 동일 403 — 헤더 검증 부재 명시 동결 (Phase 6 도입 시 변경 기준점)
- `GatewayIntegrationTest.RouteDefinition.routeFieldsFrozen`: 12 인덱스별 id/uri/Path predicate 동결 + review-route 1번 + auth-user-route 2번 위치 (yml 정렬 변경 즉시 검출)
- `GatewayIntegrationTest.Cors.preflight_allowsConfiguredOrigin`: webAppContextSetup이 CorsFilter 자동 등록 X → `addFilter(corsFilter)` 명시 적용 후 preflight 200 + Allow-Origin/Credentials 동결
- `GatewayCorsConfigTest.parseAllowedOrigins_trimAndFilterEmpty`: 콤마 뒤 공백 / 연속 콤마 / 빈 입력 6 케이스로 시한폭탄 동결

**진단/결정 + 가정 정정 사례 (4번째)**:
- **Step 3 가정 정정 (사용자 권장 반영)**: 가이드의 "X-Internal 헤더 통과/위조 차단" 시나리오는 CLAUDE.md §3 + InternalUserController 주석에 따라 **Phase 6 작업**으로 분류. 4-B는 *현재 동작 동결*(헤더 무관 무조건 403, 헤더 검증 로직 부재 명시) — 옵션 X 채택.
- **CorsFilter 자동 등록 부재 (4번째 진단 가정 정정)**: webAppContextSetup이 ServletContext 필터를 자동 적용하지 않아 첫 빌드 시 2건 FAIL → `@Autowired CorsFilter + addFilter` 명시 패턴으로 정정. 누적 가정 정정 사례: ① 4-A ResultResponse 필드명, ② 4-B build.gradle testImplementation, ③ 4-B InternalBlock 헤더, ④ 4-B CorsFilter.
- **CORS trim 즉시 처리 결정**: env 공백 origin 매칭 실패는 학원 발표 환경변수 설정 시 사고 위험 — Phase 5 미루지 않고 4-B 백필에 묶어 처리.
- **HelloController 삭제 범위**: gateway 단독. admin/rider HelloController는 Phase 5 신규 기능 도입 시 함께 정리 (현재 미기동 영향 0).
- **Q3 BaseSecurityConfig CORS PATCH 누락**: tech-debt 등재만 (외부는 Gateway 경유 실질 영향 0, 이중화 제거 작업 시 함께).
- **WebMVC Gateway 5.0.1 신규 도입 학습**: `GatewayMvcProperties` 빈 주입 가능, RouteProperties/PredicateProperties 별도 클래스, 라우트 검증은 ApplicationContext에서 직접 — 라이더(Phase 5) 신규 모듈 통합 테스트 도입 시 패턴 재사용.
- code-reviewer Warning 2건(cosmetic): W-1 nonInternalPath lambda 단순화 가능, W-2 클래스 Javadoc 'Step 4·5 추가 예정' 잔존 — 다음 파일 손볼 때 정리.

### Phase 4-C 신규 기능 결과 (2026-05-02) — Redis 토큰 저장 단일 목적

**0단계 .env.example placeholder + Redis 인프라 + RefreshTokenStore + UserService 통합 — 9 커밋 / 10 신규**:

| Step | 내용 | 신규 / 변경 | 커밋 |
|---|---|---|---|
| 0 | `.env.example` placeholder 정리 (실 시크릿 git 추적 차단 — Q6 (b) 별도 PR 즉시) | 설정 | `3720d8f` |
| 1-1 | spring-boot-starter-data-redis 의존성 + application.yml redis 설정 | 의존성 | `75767d4` |
| 1-2 | docker-compose.yml — redis:7-alpine + healthcheck (Q5 docker-compose) | 인프라 | `50cc7f7` |
| 1-3 | README Redis 실행 섹션 (`docker compose up -d redis`) | 문서 | `1492326` |
| 2-1 | `RefreshTokenStore` 인터페이스 + `RedisRefreshTokenStore` 구현 (Lettuce, key `rt:{userNo}`, RT 문자열 그대로) | 신설 코드 | `693bf84` |
| 2-2 | RedisRefreshTokenStoreTest 단위 (save 2 + get 2 + delete 1, ArgumentCaptor) | 5 | `3259c28` |
| 3-1 | UserService.signup/signin: `issueAndStoreTokens` 헬퍼 (D2 분리 호출) + D1 throw 동결 | 코드 + 테스트 갱신 6 + 신규 2 | `c26a4c4` |
| 3-2 | UserService.reissue: 저장 RT 비교 (storedRtMissing → 401 / 불일치 → 401) | 코드 + 테스트 갱신 1 + 신규 2 | `6fd8ce1` |
| 3-3 | UserService.signout 시그니처 `(long userNo, res)` + D1-bis best-effort + UserController @AuthenticationPrincipal | 코드 + 테스트 갱신 2 + 신규 1 | `d903672` |

**전체 누적**: **266 PASS** (common 9 + auth 50 + main 178 + gateway 29). auth 40 → 50 (+10).

**Phase 4-C 핵심 검증 케이스**:
- `RedisRefreshTokenStoreTest.Save.save_capturesKeyValueAndTtl`: ArgumentCaptor 3개로 키 `rt:42` + RT 문자열 + TTL Duration 동결
- `UserServiceTest.Signup.redisStoreFailure_throws` / `Signin.redisStoreFailure_throws`: D1 정합성 — Redis 다운 시 login 자체 실패 (5xx)
- `UserServiceTest.Reissue.storedRtMissing_throws401`: signout 후 reissue 시도 → 401 '로그아웃되었습니다' (RT revoke 검증 핵심)
- `UserServiceTest.Reissue.cookieRtMismatchesStored_throws401`: 위조 RT 차단 동결
- `UserServiceTest.Signout.redisDeleteFailure_continuesWithCookieDeletion`: D1-bis best-effort — delete 실패해도 쿠키 만료 진행 (warn 로그만)

**진단/결정 + 가정 정정 사례 (5번째)**:
- **JwtUser 메서드명 정정 (5번째 진단 가정 정정)**: 빌드 fail로 발견 — `getUserNo()` 가정 → 실제 `getSignedUserNo()`. Lombok `@Getter` 동작 확인 후 정정. 누적: ① 4-A ResultResponse, ② 4-B build.gradle, ③ 4-B 헤더, ④ 4-B CorsFilter, ⑤ 4-C JwtUser 필드명.
- **D1 throw vs D1-bis best-effort 분리 결정**: 시작점(login)은 정합성 우선(throw), 종료점(signout)은 UX 우선(best-effort). 한 항목 = 한 개념(W-2 교훈) 적용.
- **D2 분리 호출 (mmg-common 영향 0)**: `JwtTokenManager.issue` 시그니처 변경 회피 → UserService에서 `generateAccessToken/RefreshToken` 한 번만 호출 + `setAccessTokenInCookie/setRefreshTokenInCookie` 분리 호출. JwtTokenProvider.generateToken은 매 호출 다른 토큰 생성하므로 한 번만 generate해서 쿠키+Redis 같은 RT 동기화.
- **날씨/Pub-Sub 의도 제외 (옵션 A)**: 사용처 부재 인프라 도입은 dead config. NAJACKS 변종 회피. Phase 5 펫(날씨)/라이더+챗봇(Pub-Sub) 도입 시 함께.
- **AT/RT 만료 그대로 15일/15일 유지 (Q3 a)**: 운영값(30분/14일) 변경은 Phase 1 JwtTokenProviderTest 영향 가능 + 별도 결정 트리거. 학원 발표 후.
- **mock RedisTemplate (Q4 a)**: Spring Boot 4 호환 안전. embedded-redis/Testcontainers 회피. 4-A InternalUserController 패턴(mock 위주) 일관.
- code-reviewer Warning 3건(cosmetic): W-1 issueAndStoreTokens 쿠키-Redis 순서(Servlet spec 보장이지만 발표 전 리허설 권장), W-2 reissue 메시지 부분 일치 검증 정확도, W-3 docker-compose Redis 비밀번호 (개발 환경 무방, 운영 전환 시).

### 라이더 정리 단계 결과 (2026-05-05) — ADR 9개 + 인터페이스 명세 + Figma 정정 박제

**코드 0 / 문서 산출물만 / 12 커밋 / develop 브랜치 그대로**:

| Step | 내용 | 산출물 | 커밋 |
|---|---|---|---|
| 0 | develop 116 커밋 미푸시 처리 (학원 데모 자료 손실 방지) | git push | `c6f6cd8..ee0a9f3` |
| 0 | .gitignore에 .claude/agent-memory/ 추가 + Figma 10장 추가 + 추가 push | settings + assets | `066dcb3`, `ca6e3c7` |
| 1 | Figma 분석 결과 + 진단 정정 10건 + 결정 매트릭스 (Q1~Q8 + D5~D10) 박제 | `docs/adr/rider/README.md` + `figma-analysis.md` | `5cd0500` |
| 2 | ADR-001 서비스 경계 (Q1-C: auth만 + 추가정보 별도 endpoint) | ADR-001 | `94803d6` |
| 3 | ADR-002 데이터 모델 + DB 분리 (mmg_rider 6 테이블, Figma 정정 1·4·5·6·8·9·10·11) | ADR-002 | `e4b0b02` |
| 4 | ADR-003 통신 패턴 (Feign + WebSocket+STOMP, Q4-X Pub/Sub 미도입) | ADR-003 | `13104b4` |
| 5 | ADR-004 상태 머신 7개 (정정 2·3, Q5-A 낙관적 락, D5 HTTP 409) | ADR-004 | `836b49c` |
| 6 | ADR-005 위치 추적 (Q6-A Redis KV TTL 30s, D6 5s/10s, STOMP) | ADR-005 | `e78c1fc` |
| 7 | ADR-006 Redis 활용 (KV만, Pub/Sub/Geospatial/RT 이관 미도입) | ADR-006 | `f7a1ab2` |
| 8 | ADR-007 정산 (정정 5 MVP 격상, D10-b admin 수동 confirm) | ADR-007 | `1489676` |
| 9 | ADR-008 근무 세션 + 토글 (정정 6·7, D8 EATING 차단, D9 종료=세션) | ADR-008 | `3b26bce` |
| 10 | ADR-009 공지사항 (정정 8, admin → rider Feign broadcast) | ADR-009 | `1a9b8c0` |
| 11 | Feign 인터페이스 명세 (구현 0, Phase 5-R1~R9에서 작성) | `interfaces.md` | `67b567a` |
| 12 | 라이더 정리 단계 완료 표시 | 본 갱신 + tech-debt | (이 커밋) |

**전체 산출물**: `docs/adr/rider/` 디렉토리 — README + figma-analysis + ADR-001~009 + interfaces (총 12 파일)

**핵심 결정 매트릭스 요약**:
- Q1-C 가입: auth만 + 추가정보 별도 / Q2-B 면허/승인: PENDING→admin / Q3-A account 합침 / Q4-X Pub/Sub MVP 미도입
- Q5-A 낙관적 락 / Q6-A Redis KV / Q7 작업 R1~R9 그대로 / Q8 tech-debt 5-R1과 함께
- D5 충돌 HTTP 409 / D6 5s 발표/10s 운영 / D7-a 평문 전화번호 / D8-a EATING 차단 / D9-a 업무 종료=세션 / D10-b admin 수동 confirm

**진단/결정 + 가정 정정 사례 (라이더 정리에서 4건 추가, 누적 9건)**:
- ⚠6 Role enum 부재 (JwtUser.role은 String 필드)
- ⚠7 main에 /internal/order/** 부재 (Rider→Main 상태 전파 흐름 미구현)
- ⚠8 mmg_rider DDL / main에 rider/delivery 테이블 부재 (orders 컬럼만)
- ⚠9 Gateway rider 라우트 이미 정의됨 (Phase 4-B에서 선반영)
- ⚠10 Figma 13장이 아닌 10장 (Step 3 정정)
- 누적: 4-A·4-B·4-C 5건 + 라이더 4건 = 9건. 원칙 정착.

**Figma 정정 사항 10건 박제 (figma-analysis.md 상세)**:
1. rider 필드 추가 (license_type, vehicle_type, account_holder)
2. delivery 상태 5→7 (ARRIVED_AT_STORE, AWAITING_PICKUP 추가)
3. orders.delivery_state 매핑 정정 (ADR-004)
4. 배달료 base_fee + extra_fee 분리
5. 정산 도메인 MVP 포함 격상 (ADR-007)
6. work_session 도메인 신설 (ADR-008)
7. 상태 토글 라벨 분리 (내부 ACTIVE/EATING vs UI "배달중/식사중")
8. 공지사항 도메인 신설 (ADR-009)
9. delivery_no ≠ order_id 분리
10. 전달 완료 사유 분류 (DIRECT/CUSTOMER_REQUEST/CUSTOMER_ABSENT) + 사진

**Phase 5 작업 분할 (R1~R9)** — figma-analysis.md / README.md 참조

**tech-debt 갱신 (라이더 정리에서 발견)**:
- 폐기: RefreshTokenStore mmg-common 이관 (Q1-C 결과로 라이더 별도 RT 불필요)
- 신규: Pub/Sub Y 옵션 (다중 Main 진입 시) / Redis Geospatial (가용 라이더 매칭) / 위치 사후 분석 / 손님 전화번호 마스킹 (D7-a) / 사가 패턴 (Q1-C 사후) / 정산 자동 배치 (D10-b 사후) / rider_account 분리 + audit (Q3-A 사후)

---

### 라이더 정리 ADR cosmetic 정정 + D11 추가 (2026-05-05)

**code-reviewer Warning 4건 중 3건 정정 + admin 의존성 임시 우회 결정 (D11 옵션 A-1) — 4 커밋**:

| 커밋 | 내용 |
|---|---|
| `06e2cc0` | ADR-001 헤더 Q2 추가 (W-3) + **D11 임시 운영 섹션 신설** — `rider.auto-approve` toggle (개발/발표 true / 운영 false) + RiderService.join() 명시 블록 + TODO 주석. Q2-B 결정 자체 변경 X (admin-service 의존성 임시 우회만). admin-service는 팀원 작업 영역으로 절대 건드리지 않음. |
| `69398e5` | ADR-007/009 결정 섹션에 트랜잭션 정책 (readOnly/@Transactional) 명시 (W-1) — Phase 4-A InternalUserController + Phase 3 main B-1 패턴 일관 |
| `7b98129` | ADR-006 헤더에 관련 Figma 필드 추가 (W-2) — 8건 형식 일관 (직접 매핑 없음 명시 + ADR-003/005 참조 안내) |
| (이 커밋) | PROGRESS 갱신 (cosmetic 정정 박제 + Phase 5-R1 범위 정정) |

**W-4 (MainInternalClient `GET /internal/order/{orderId}` 미명세)**: Phase 5-R4 진입 직전 처리 — 라이더 본격 작업 시 자연 통합. 지금 별도 처리 X.

**Phase 5-R1 작업 범위 정정 (D11 반영)**:
- 원안: ① RIDER role 가입/로그인 + ② tech-debt cleanup
- 정정: ① RIDER role 가입/로그인 (auth-service 분기, license_type/vehicle_type 포함) + ② **`rider.auto-approve` toggle 구현 (RiderProperties + application.yml + 명시 블록 + TODO 주석)** + ③ tech-debt cleanup (gateway timeout, getUsers empty 통합 테스트, GatewayIntegrationTest cosmetic 2건)
- 실제 구현: rider 브랜치 checkout 후 (라이더 정리 결과 + Phase 4-C 머지 선행)

**D11 결정 매트릭스 추가**:
- D11 (옵션 A-1) Q2-B의 admin-service 의존성 임시 우회 — `rider.auto-approve` toggle. admin endpoint 도입 시 toggle false + 임시 블록 제거로 해소. **admin-service 절대 건드리지 않음 (팀원 작업 영역)**

---

### Phase 5-R1-A 중간 종결 (2026-05-05) — 단위 7건 PASS, 통합/A-7/R1-B/Z 다음 세션 이월

**rider 브랜치 작업** (develop fast-forward 머지 후 R1-A 진입). R1-A Step A-1 ~ A-6 (단위)까지 완료, 통합 테스트는 학원 DB 사전 조건 대기 — **7 커밋 + Q-DB 미결정**:

| Step | 커밋 | 내용 |
|---|---|---|
| (인프라) | `919ce06..035dbae` | rider 브랜치를 develop으로 fast-forward 머지 + push (origin/rider 동기화) |
| A-1 | `721c011` | rider-service build.gradle: data-jpa + security + mysql-connector |
| A-2 | `03c03c2` | DDL `docs/ddl/rider-schema.sql` (rider 테이블만, R1 범위) |
| A-2 | `417742c` | Rider 엔티티 + RiderRepository + datasource yml + RiderApplication @EnableJpaAuditing |
| A-3 | `eee2a68` | RiderProperties + auto-approve toggle (D11) — `rider.auto-approve: ${RIDER_AUTO_APPROVE:true}` |
| A-4 | `de27710` | RiderService.joinProfile + auto-approve 임시 블록 + TODO 주석 |
| A-5 | `c403f29` | RiderController PUT /profile + GET /me + RiderSecurityConfig (hasRole RIDER) |
| A-6 단위 | `4c7a87e` | RiderServiceTest 7건 PASS (가짜 0건) — JoinProfile 5 + FindProfile 2 |

**단위 테스트 결과 (7건 PASS)**:
- JoinProfile: autoApprove true (ArgumentCaptor + ACTIVE 검증) / false (PENDING 유지) / 중복 CONFLICT 409 / vehicleType 화이트리스트 위반 / blank 검증
- FindProfile: happy (dto 전수 검증) / NOT_FOUND
- 가짜 0건 원칙 일관 — assertEquals + assertThatThrownBy + ArgumentCaptor + verify(never)

**진단 가정 정정 (R1-A 진단에서 ⚠11 추가, 누적 10건)**:
- ⚠11 InternalUserController 경로 `/internal/users` 가정 → 실제 `/internal/auth/user/{userNo}` + `/internal/auth/users` (auth prefix). interfaces.md §5 정정 필요 — Q-W11 결정 (R4 진입 직전 처리)

**사용자 결정 매트릭스 (Phase 5-R1)**:

| ID | 결정 | 상태 |
|---|---|---|
| Q-B | (A) ADR-001 Q1-C 그대로 | ✅ 채택 + 구현 (A-4) |
| Q-Toggle | (A) rider-service | ✅ 채택 + 구현 (A-3, A-4) |
| Q-Timeout | (A) 모든 라우트 일괄 | 🟡 채택, 미구현 (R1-B) |
| Q-Split | (2) R1-A / R1-B | ✅ 채택 |
| Q-Sec | (b) tech-debt + ADMIN 임시 가드 | 🟡 채택, 미구현 (R1-B) |
| Q-Status | (b) status=null + DB lookup | ✅ 채택 + 명시 (RiderService docstring) |
| Q-W11 | (b) R4 진입 직전 처리 | ⏳ 채택, 미구현 (R4) |
| **Q-DB** | **미결정 — 학원 DB schema/권한** | **⏳ 다음 세션 (가/나/다)** |

**보류 (다음 세션)**:
- **Step A-6 (통합 테스트)**: 학원 DB 사전 조건 대기 — `CREATE SCHEMA my_mmg_rider` + `GRANT ALL PRIVILEGES TO 'green2'@'%'` + `docs/ddl/rider-schema.sql` 실행 + `.env`의 `RIDER_DB_URL` 학원 DB 값 설정
- **Step A-7 (code-reviewer)**: A-6 통합 테스트 후
- **R1-B 전체 (Step B-1 ~ B-5)**: ADMIN 가드 (Q-Sec) + gateway timeout (Q-Timeout) + getUsers empty 통합 (4-A W-2) + GatewayIntegrationTest cosmetic 2건 + reviewer
- **Z 단계 (Z-1 ~ Z-4)**: 빌드/테스트 / PROGRESS·tech-debt·MEMORY 갱신 / push / develop 머지 결정

**Q-DB 옵션 (다음 세션 결정)**:
- (가) 학원 DB 작업 완료 → A-6 통합 테스트 진행 → A-7 reviewer → R1-B
- (나) DBA 응답 대기 → R1-B 우선 진입 (Step B-1 ~ B-5)
- (다) 단위 7건만으로 종결 + 통합은 R6 외부 endpoint 작업 시 자연 통합 ← 권장

**추가 발견 (R1-A 진행 중)**:
- HelloController (Phase 0-B 잔재) 그대로 유지 — `/api/rider/hello` hasRole(RIDER) 보호, 외부 노출 X
- validation starter 미도입 — auth/main 기존 패턴 일관 (Service 명시 if 검증)
- `accountNo` 본인 한정 노출 (RiderProfileRes 평문) — Phase 6+ 마스킹 검토 (D7 패턴 일관, tech-debt 등재 권장)

---

## 다음 단계

**Phase 5-R1-A 중간 종결.** 진행 흐름: R1-A 잔여 (Q-DB 결정 후) → R1-B → R2~R9 → 학원 발표.

- **다음 세션 시작점**: Q-DB 결정 (가/나/다)
  - (가) 학원 DB 작업 완료 → A-6 통합 테스트 진행 (4건 이상)
  - (나) DBA 응답 대기 → **R1-B 우선 진입** (Step B-1 ~ B-5)
  - (다) 단위만으로 R1-A 종결 → R1-B 진입 → R6에서 통합 테스트 자연 통합
- **Phase 5 후속** — R2~R9 (mmg_rider DDL → 상태 머신 → Internal API → 위치 추적 → 외부 endpoint → 정산 → 근무 세션 → 공지). R5/R6 병렬 가능. R4 진입 직전 W-4 정리. R7~R9 학원 발표 데모 시간 여유 시.
- **학원 발표** — Phase 5 종결 후 최종 마일스톤. Redis docker compose + RT revoke + WebSocket+STOMP 시연 사전 리허설 권장.
- **admin-service 절대 건드리지 않음** (D11 일관, 팀원 작업 영역).

---

### Phase 5-R1 종결 (2026-05-06) — Q-DB (다) → (가) 전환

R1-B 폐기(영역 침범) → R1-A 단독 종결 박제. 학원 DB my_mmg_rider 적용 + bootRun validate PASS.

영역 매트릭스 확정 (CLAUDE.md §3): main/gateway/common 모두 팀원, 본인 영역 = mmg-rider-service + docs/ 단독.

`docs/team-handoff.md` 5건 핸드오프 (Q-Sec / Q-Timeout / Q-W11 / GatewayIntegrationTest cosmetic 2건).

---

### Phase 5-R1-FE (2026-05-07) — momoolggo2-fe 신규 + 라이더 가입/로그인 화면

FE 영역 결정: `momoolggo2-fe`(별도 repo) `views/rider/**` + 라이더 라우트/스토어/서비스 단독, 그 외 팀원.

작업 결과 (FE 4 커밋 + BE 2 커밋, push 완료):
- RiderSignupView (Figma 170121 추종, 1차 customer 패턴) + RiderSigninView (Figma 170125 1번째)
- riderService + riderStore 스켈레톤 + 라우터 라이더 라우트 3건 추가
- BE: CLAUDE.md §3 영역 표 갱신, figma-analysis.md 매핑 정정 4건
- 시연 검증 4 endpoint PASS (BE 5 서비스 + FE Vite 동시 기동, Gateway 라우팅 OK)

---

### Phase 5-R2 종결 (2026-05-07) — 5 테이블 entity 일괄 도입 (R2-a ~ R2-e)

라이더 도메인 5 테이블 entity + repository + 단위 테스트 + 학원 DB 적용 일괄. R3+ Service 진입 시 비즈니스 메서드 추가 박제.

| Sub | 도메인 | 신규 enum | 인덱스 | 단위 테스트 | 커밋 |
|---|---|---|---|---|---|
| R2-a | delivery | DeliveryStatus 7값 (ADR-004) | 3건 (rider_no/order_id/status) | 3건 | 5 |
| R2-b | delivery_log | (재사용 DeliveryStatus) | 1건 (delivery_no) | 3건 | 6 (tech-debt 1) |
| R2-c | work_session | (없음) | 1건 (rider_no) | 3건 | 6 (tech-debt 1) |
| R2-d | settlement | SettlementStatus 2값 (PENDING/CONFIRMED) | 1건 (rider_no) | 3건 | 5 |
| R2-e | notice | NoticeCategory 3값 (IMPORTANT/SAFETY/GENERAL) | 1건 (published_at) | 3건 | 5 |

**패턴 일관 (4 테이블)**: BaseEntity 상속 + 명시 생성자 + setter 0 + Mockito 단위만 (Q-R2a6-Test (iii)). delivery_log만 BaseEntity 미상속(이력 본질, R2-b 본질 차이 박제).

**reviewer 결과**: R2-a/c/d/e 모두 PASS, Critical 0건. W-2 cosmetic 2건 → tech-debt 등재(actorRole / vehicleType R3 enum 도입).

**학원 DB 적용 (Q-DB (가) 일관)**: my_mmg_rider 6 테이블 모두 박제 + rider-service bootRun validate PASS 4회 재기동.

---

## 다음 단계 (R2 종결 후)

- **R3 진입 후보**: DeliveryService.updateStatus 상태 머신 (Q5-A 낙관적 락 + actorRole/vehicleType enum 도입과 함께 — tech-debt 2건 자연 해소)
- **FE 진입 후보** (Q-FE-Timing (ii)): 라이더 메인 화면 / 배달현황 (Figma 170131/170137 추종)
- **잔존 R 단계**: R4 Internal API / R5 위치 추적 STOMP / R6 외부 endpoint / R7 정산 / R8 근무 세션 / R9 공지

---

## R5 진입 전 박제 정정 (2026-05-11)

R5 위치 추적 진입 직전 6번 절차 자동 검증 + 박제 정정 5건 통합. 메모리 3 파일 박제 정정. PROGRESS.md R3/R4 종결 갱신 부채는 별건 (R5 종결 시 함께 갱신 권장).

| # | 정정 위치 | 내용 |
|---|---|---|
| [1] | `feedback_decision_load_balance.md` + `feedback_verify_diagnostic_assumptions.md` | **#27 번호 충돌 정정** — `decision-#XX` (결정 식별자) vs `case-#XX` (사례 식별자) prefix 분리. case-#27(jackson-databind testCompileClasspath 부재) vs decision-#27(§4.1 vs §7.2 통합 R9 시점) 동시 존재 충돌 해소. 점진 정착 (R5+ 진입 시 일관 적용 시작, 기존 메모리 인용은 grep 부담 회피). |
| [2] | `feedback_decision_load_balance.md` | **R4 decision-#26 압축률 측정 시작 박제** — R3 1:4.5 측정 + R4 decision-#26 1:2~3 추정 → R6/R7 진입 시 *실측 효과 추적* 강제. 압축률 가설 검증 자료 누적 (1:1 / 1:2 / 1:2~3 분기). 표 신설 (R4 종결 1:1 베이스라인 / R6 진입 시 분기 / R7 진입 시 분기). |
| [3] | `feedback_verify_spec_assumptions.md` | **6번 절차 강화 1차 효과 박제** — R4 본격 작업 중(6 커밋) 본인 박제 인용 부정확 추가 0건. 누적 #15→#17-A→#20→#23→#24→#25 = 6/6 임계값 멈춤. 강화 1(출처 인용 검증)/2(출처 부재 진입 차단)/3(외부 자료 확인 출처 명시) 작동 신호 1차 입증. R5/R6 진입 시 추가 측정 강제. |
| [4] | `feedback_verify_spec_assumptions.md` (동일 박제 내) | **case-#28 후보 검증** — 복원 메시지 "통합 5건" → git show 16c0844(5 케이스 본 카운트, +217줄) + dbd038e(W-1 orderId UUID 격리, 같은 5 케이스 유지, +6/-4) 실측 = **정확** (정정 불필요, 사례 표 등재 X). 6번 절차 자동 검증 작동 사례. |
| [5] | `feedback_verify_diagnostic_assumptions.md` | **case 카운트 정확 박제** — "21건 누적" → **"27건 누적 (#XX 단위, case-#17-A/B 분리 카운트로 28 항목)"** 정정. R3-c 진입 직전 2 + R3-b 박제 직후 1 + R3 종결 직후 1 + R3 종결 후 자료 시뮬 1 + R4 진입 직전 1 + R4-4 통합 직후 1 = 7건 후속 갱신 누락 부채 동시 해소. line 3 description + line 10 body + line 12 표 헤더 + description 끝 prefix 분리 박제 통합. |

### 5/6번 절차 자동 작동 결과 (R5 진입 직전, case 신규 추가 0건)

- **5번 절차** (영역/의존성/매트릭스/명세 인용/진입 메시지 실측):
  - ADR-005 line 28 Q6-A 채택 (Redis KV `rider:loc:{riderNo}` TTL 30s) ✅ 인용 정확
  - ADR-005 line 65 위치 PUT API + line 77 Internal GET ✅ 영역 본인 ✅
  - ADR-005 line 87 STOMP broadcast = Main 책임 (영역 ❌, 팀원) — R5에서 본인 X
  - ADR-006 line 59 KV Phase 5-R5 도입 + line 73 RiderLocationStore 인터페이스 ✅ 4-C `RefreshTokenStore` 패턴 일관
  - `interfaces.md` line 64-78 §1.2 위치 조회 박제 ✅ R4 R3-a 정정 일관 (case-#26)
- **6번 절차** (박제 메시지 인용 자동 검증):
  - 복원 메시지 "통합 5건" ✅ 정확 (case-#28 후보)
  - "ADR-006 Redis KV TTL 30s 박제 인용" ✅ 정확 (line 28 + 59 + 87 박제)
  - "R4 RiderService.getInternalLocation stub 위치" ✅ `RiderService.java:131` BusinessException(NOT_FOUND) throw 박제, R5 Redis 인프라 도입 예정 명시
  - 진단 가정 정정 추가 0건 = 강화 1차 효과 입증 일관

### 다음 단계: R5 진단 보고 + 단계 분할

ADR-005/006 박제 충분 + 6번 절차 결과 0건 정정 = **본인 결정 자리 0건 추정** (Code 자율 진입). R5 단계 분할 (Code 자율 B 분류 채택 후보):
- **R5-1 인프라**: `spring-boot-starter-data-redis` 의존성 + `RiderLocationStore` 인터페이스 + `RedisRiderLocationStore` 구현 (4-C `RefreshTokenStore`/`RedisRefreshTokenStore` 패턴 일관)
- **R5-2 본체**: `PUT /api/rider/location` (LocationController + LocationService 분리) + `RiderService.getInternalLocation` stub 채움 (Redis GET → NULL 시 404 → JSON 응답)
- **R5-3 테스트**: 단위 (Mockito mock RedisTemplate + ArgumentCaptor, 4-C 정착) + 통합 (학원 Redis 의존 1건 — Q-DB (가) 일관)

**예외 결정 자리 후보** (있을 시 본인 자리 A 분류):
- D1 throw 패턴 (Redis 다운 시 5xx) — ADR-005 line 72 박제 일관, 결정 자리 0
- 위치 직렬화 포맷 (JSON vs MessagePack) — ADR-006 line 146 미해결, R5-1 진입 시 본인 자리 후보
- 통합 테스트 학원 Redis 의존 vs mock-only — Q-DB (가) 일관 추정, 본인 자리 후보
