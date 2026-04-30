# MOMOOLGGO_MSA — 진행 스냅샷

> 작성: 2026-04-28 / 최종 갱신: **2026-04-30 Phase 2-Backfill-D 완료**
> 한 페이지로 Phase 0~4-B 전체 상태 + 다음 단계 정리.
> 상세 체크리스트는 [migration-plan.md](migration-plan.md), 결정 근거는 [decisions.md](decisions.md), 자료 인덱스는 [INDEX.md](INDEX.md).

---

## 🏆 현재 상태 한 줄

> **5개 Spring Boot 4.0.3 멀티모듈 + 4개 schema(MariaDB) + Feign cross-schema + JPA/MyBatis 하이브리드 영구 공존 + Gateway 단일 진입점 정비. 🔥 학원 발표/시연 가능. develop 브랜치 안정.**

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
| 4-C | Redis 도입 (토큰/캐시/Pub-Sub) | — | ⏳ 다음 |
| 4-D / 6 | 외부 FK 정합성 (Saga/Outbox) | — | 대기 |
| 5 | 신규 기능 (펫/룰렛/챗봇/SSE/Rider/Admin) + TossPaymentClient | — | 팀원 합류 시 |
| 6 | 고도화 (Outbox/Kafka/모니터링/CI-CD) | — | 대기 |

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
| **my_mmg_rider** | (빈 schema) | 0 | Phase 5에 rider_profile 등 |
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
- [x] **Phase 2-Backfill-D (2026-04-30)** — Owner 18 + Store Feign null fix 5 + AddressSearch refactor 8 + Cart 권한 5+통합 4 + UserAddress 권한 2 = 신규 42 추가, **116/116 PASS**
- [ ] Phase 2-Backfill-D-bis (OwnerService 14개 메서드 권한 일괄 추가, 라이더 진입 전)
- [ ] Phase 3 백필 → `@code-reviewer` 검증 → PASS
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

---

## 다음 단계

**Phase 2-Backfill-D-bis (라이더 진입 전 마지막 단계)** 또는 **Phase 4-C (Redis)** 또는 **Phase 5 (팀원 합류 시)**.

- **Phase 2-Backfill-D-bis**: OwnerService 14개 메서드 권한 분기 일괄 추가 (D 단계에서 보류된 항목, tech-debt.md "예정된 작업" 참조)
- Phase 4-C: 학원 발표 후 — 토큰 저장 (auth) + 날씨 캐시 (main) + Pub/Sub
- Phase 5: 팀원 합류 시 본격 — 펫/룰렛/챗봇/SSE/Rider/Admin + TossPaymentClient + 잔존 도메인 경계 정리
