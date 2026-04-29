# MSA 전환 진행 체크리스트

> 각 단계 완료 시 `[ ]` → `[x]`로 변경
> Phase 진행 중 발견된 이슈는 하단 "이슈 로그"에 기록

---

## 📍 현재 위치

**Phase 3 전체 완료 ✅ (2026-04-29)** — Phase 4-B (Gateway 라우팅) 또는 Phase 5 (팀원 분담) 다음

---

## Phase 0 — 멀티모듈 스켈레톤

### 0-A. 루트 + 공통 모듈
- [x] `settings.gradle` 작성 (모듈 6개 선언)
- [x] 루트 `build.gradle` 작성 (subprojects 공통 설정)
- [x] `gradle.properties` 작성
- [x] `mmg-common` 모듈 생성
- [x] `mmg-common/build.gradle` 작성 (라이브러리, bootJar 비활성)
- [x] `mmg-common`만 단독 빌드 성공 확인

### 0-B. 5개 서비스 + Gateway
- [x] `mmg-auth-service` 생성 + AuthApplication.java + application.yml
- [x] `mmg-main-service` 생성 + MainApplication.java + application.yml
- [x] `mmg-rider-service` 생성 + RiderApplication.java + application.yml
- [x] `mmg-admin-service` 생성 + AdminApplication.java + application.yml
- [x] `mmg-gateway` 생성 + GatewayApplication.java + application.yml
- [x] 각 서비스에 임시 HelloController (`GET /api/{prefix}/hello`)
- [x] 각 서비스 단독 기동 성공 (4개 서비스 + gateway 순차 검증)
- [ ] 5개 서비스 동시 기동 성공 (메모리 부담으로 미수행 — 사용자 수동 검증 권장)
- [x] Gateway 통해 라우팅 확인 (auth 경로 검증, 다른 3개도 동일 패턴)
- [x] **Phase 0 완료 커밋**

---

## Phase 1 — Auth 서비스 분리

### 1-A. mmg-common에 공통 코드 이동
- [x] `JwtTokenProvider`, `JwtTokenManager`, `TokenAuthenticationFilter` → mmg-common/jwt
- [x] `JwtUser`, `UserPrincipal` → mmg-common/model
- [x] `ResultResponse` → mmg-common/dto
- [x] `ConstJwt` → mmg-common/constants
- [x] `MyCookieUtil` → mmg-common/util
- [x] `WebSecurityConfiguration` 베이스 → mmg-common (그대로 이동, base/override 리팩은 1-B에서)
- [x] mmg-common 빌드 성공
- [x] 전체 빌드 성공 (6개 모듈)
- ❌ **WebConfig는 의도적으로 미포함** — main-service 전용 (이미지 정적 리소스 핸들러), Phase 2에서 직접 이동

### 1-A 후속 TODO (1-B 완료 시 일괄 처리)
- [ ] `JwtTokenManager.java` line 71 주석 잔재 정리 (`com.green.greengram` 참조)
- [ ] `WebSecurityConfiguration` base/override 패턴 리팩토링 (도메인별 경로 매칭이 mmg-common에 박혀있는 상태 해소)
- [ ] `CorsConfigurationSource`의 `localhost:5173` 하드코딩 → 환경변수화 검토
- [ ] mmg-common 빈을 각 서비스에서 활성화하는 방법 결정 (`scanBasePackages` vs `@Import` vs auto-configuration)

### 1-B. Auth 도메인 이동 (5단계로 세분)

#### 1-B-1. my_mmg_auth DB 생성 + 데이터 마이그레이션
- [x] `my_mmg_auth` schema 생성 (utf8mb4_unicode_ci)
- [x] user, address DDL 적용 (collation 변경 외 원본 동일)
- [x] user 15행 + address 20행 INSERT (schema 간 INSERT...SELECT)
- [x] 검증 5종 통과 (row count, AUTO_INCREMENT, MAX PK, FK 정합성, orphan 0)
- [x] 백업 dump (`docs/ddl/dump-*.sql`, .gitignore 차단)
- [x] `docs/ddl/README.md` 작성 (Phase 2 재사용용)

#### 1-B-2. WebSecurityConfiguration base/override 리팩토링
- [x] CORS origin 환경변수화 (`CORS_ALLOWED_ORIGINS`, 콤마 구분, SpEL split)
- [x] BaseSecurityConfig 신규 (`@ConditionalOnClass(SecurityFilterChain)` + `@ConditionalOnMissingBean` + `@EnableConfigurationProperties(ConstJwt)`)
- [x] WebSecurityConfiguration 삭제
- [x] AuthSecurityConfig 작성 (auth-service 자체 SecurityFilterChain — base.applyCommon 호출)
- [x] mmg-common 4개 @Component(JwtTokenProvider/Manager/Filter/MyCookieUtil)에 `@ConditionalOnClass` 추가 — Gateway 등 시큐리티 미사용 서비스 보호
- [x] 5개 서비스 Application 클래스에 `scanBasePackages` 추가 (`com.green.mmg.{svc}` + `com.green.mmg.common`)
- [x] root build.gradle subprojects의 BootRun `workingDir = rootProject.projectDir` (멀티모듈에서 .env 로드 위해)
- [x] auth-service `application.yml` constants.jwt + cors.allowed-origins 블록 추가
- [x] auth-service 기동 + curl 검증 (`/api/auth/hello` 200, 미인증 요청 403)

#### 1-B-3. user + UserAddress 도메인 코드 → auth-service (D2 결정으로 1-B-3/1-B-4 통합)
- [x] `application/user/*` 이동 (Review 관련 D1 결정으로 완전 제외)
- [x] `application/address/*` 중 UserAddress 부분만 이동 (AddressSearch/MapConfig는 Phase 2 main으로)
- [x] 패키지명 변경 (`com.green.momoolggo` → `com.green.mmg.auth`)
- [x] MyBatis mapper xml 이동 + 분리 (User.xml review 쿼리 제거, address.xml은 UserAddress만이라 그대로)
- [x] mmg-auth-service build.gradle: `mybatis-spring-boot-starter:4.0.1`, `mysql-connector-j` 추가
- [x] mmg-auth-service application.yml: `spring.datasource`, `mybatis.mapper-locations`
- [x] Phase 0 임시 hello 컨트롤러 제거
- [x] AuthSecurityConfig: hello 제거, `/error` permitAll 추가 (unhandled exception 시 시큐리티 거부 방지)

#### 1-B-4. auth-service 통합 검증 (1-B-3과 함께 완료)
- [x] 회원가입 → 로그인 → JWT 인증 → 내 정보 조회 → 주소 CRUD 전 흐름 200 OK
- [x] DB INSERT 정상 (user_no=18, address_id=38 — AUTO_INCREMENT 정상)
- [x] 응답 형식 `ResultResponse<T>` 모놀리식과 동일
- [x] **Phase 1 완료 커밋**

#### 1-B-3.5. UserAddress 위치 정정 (ERD 위반 수정 + Phase 2-D 통합) ✅
> 발견: ERD에서 `address` 테이블이 **my_mmg_main** (초록 그룹)인데 Phase 1-B-3에서 my_mmg_auth로 잘못 마이그레이션됨.
> Phase 2-D(AddressSearch + MapConfig)와 통합 진행.

**컬럼명 변경 (ERD 따름)**
- [x] `lat` → `latitude`
- [x] `lng` → `longitude`
- [x] `address_detail` VARCHAR(300) → VARCHAR(200) (사전 검사: max 46자, 손실 0)
- [x] **`user_no` 유지** (ERD `user_no2`는 오타로 판단 — decisions.md 기록)

**작업 항목**
- [x] my_mmg_main.address 테이블 신규 (ERD 컬럼 매핑 적용 + 외부 FK 제외)
- [x] my_mmg_auth.address → my_mmg_main.address 데이터 이동 (20행, AUTO_INCREMENT=39 동기화)
- [x] **my_mmg_auth.address DROP** (사용자 승인 후, 데이터 손실 0 확인)
- [x] auth-service UserAddress 코드 5+1 파일 → main-service 이동 (Controller/Service/Mapper + 2 model + Address.xml)
- [x] Phase 2-D AddressSearch + MapConfig 코드 4파일 통합 이동 (네이버 API 호출용)
- [x] URL 변경: `/api/user/address/**` → `/api/address/**` (옵션 B)
- [x] UserService.signup 옵션 D-1 적용 — BFF 패턴, 즉시 AT/RT 발급, UserAddressMapper 호출 제거
- [x] auth-service `address/` 패키지 + Address.xml 완전 제거
- [x] MainSecurityConfig는 `anyRequest().authenticated()`로 자동 커버 (변경 없음)
- [x] application.yml main-service에 `naver.*` 환경변수 매핑
- [x] api-spec.md URL + 회원가입 응답 갱신
- [x] decisions.md 변경 이력 섹션 추가 (ERD `user_no2` 오타, URL/컬럼/응답 변경)
- [x] CLAUDE.md §6.11 신설 (도메인 분배 결정 우선순위 = ERD source of truth)
- [x] FRONTEND_CHANGES.md 신규 (프론트 작업 가이드 누적용)
- [x] 검증: BFF 회원가입 → POST /api/address → GET /api/address 5/5 통과
- [x] 검증 후 테스트 데이터 정리 (auth.user 15행, main.address 20행 baseline 복원)

#### 1-B-3.5 작업 중 발견한 fix 2개
- UserSignupReq 필드 `Long UserNo` (대문자 'U') → `Long userNo` (소문자) — MyBatis useGeneratedKeys 매핑
- UserService.signup에서 fetch 제거, req에서 직접 (DB 1회 절감 + role 정확 반환)

#### 1-A 후속 TODO 처리
- [x] `JwtTokenManager.java` line 71 주석 잔재는 그대로 둠 (별도 작업 시 처리)
- [x] `WebSecurityConfiguration` base/override 패턴 리팩 — 1-B-2에서 완료
- [x] `CorsConfigurationSource`의 `localhost:5173` 하드코딩 → CORS env로 환경변수화 (1-B-2)
- [x] mmg-common 빈 활성화 → `scanBasePackages` 채택 (1-B-2)

## Phase 1.5 — GlobalExceptionHandler + 정리 (안전망 구축)
- [x] `BusinessException`(베이스, status 명시 가능) + `ResourceNotFoundException`(404) 작성
- [x] `GlobalExceptionHandler` (`@RestControllerAdvice` + `@ConditionalOnClass`) — BusinessException, MethodArgumentNotValidException, HttpMessageNotReadableException, RuntimeException, Exception 매핑
- [x] `JsonAuthenticationEntryPoint`(401) + `JsonAccessDeniedHandler`(403) — 시큐리티 필터 단계에서 JSON 응답 (둘 다 `@ConditionalOnClass(SecurityFilterChain)`)
- [x] BaseSecurityConfig.applyCommon에 `.exceptionHandling()` 추가 — JSON 핸들러 주입
- [x] AuthSecurityConfig에서 `/error` permitAll 제거 (GlobalExceptionHandler가 모든 예외 가로채므로 /error 미도달)
- [x] auth-service에서 RuntimeException 2개를 BusinessException으로 변경 (signin 401, check-id 409)
- [x] JwtTokenManager line 71 `com.green.greengram` 주석 잔재 제거
- [x] 테스트 사용자 정리: `my_mmg_auth.user.user_no=18` 삭제 (CASCADE로 address_id=38 자동 삭제, 사용자 명시 승인)
- [x] 5개 에러 시나리오 검증: 401/401/409/200/400 모두 ResultResponse JSON 응답 통일

---

## Phase 2 — Main 서비스 도메인 이동 (7단계로 세분)

### 2-A. my_mmg_main DB 생성 + 데이터 마이그레이션 ✅
- [x] `my_mmg_main` schema 생성 (utf8mb4_unicode_ci)
- [x] 13개 테이블 DDL 적용 (collation 변경 외 원본 동일)
- [x] 외부 FK 5개 DROP (store, likedstore, cart, orders, review_reply → user)
- [x] 같은 schema 내 FK 13개 보존
- [x] 13개 테이블 432행 INSERT...SELECT (의존 순서)
- [x] 검증 4종 통과 (row count 13/13, AUTO_INCREMENT, 외부 FK=0, 내부 FK=13)
- [x] 백업 dump (`docs/ddl/dump-my_testmomoolggo-main-*.sql`, .gitignore 차단)
- [x] DDL 보존 (`docs/ddl/main-schema.sql`)
- [x] `docs/ddl/README.md` 갱신 (재사용 가능한 절차 + 결정 근거)

### 2-B. Store + Owner + WebConfig 도메인 이동 ✅
- [x] `application/store/*` → mmg-main-service (3 java + 6 model = 9 파일, 10 endpoints)
- [x] `application/owner/*` → mmg-main-service (3 java + 11 model = 14 파일, 21 endpoints)
- [x] **WebConfig 이동** (사용자 결정으로 2-D → 2-B로 변경) — `mmg-main-service/config/WebConfig.java`. /uploads/menu, /uploads/store, /uploads/review, /uploads/pet 핸들러
- [x] thumbnailator 의존성 추가 (`net.coobird:thumbnailator:0.4.20`)
- [x] mmg-main-service `build.gradle`: mybatis-spring-boot-starter, mysql-connector-j, spring-boot-starter-security
- [x] application.yml: spring.datasource (`MAIN_DB_URL`), mybatis, multipart 10MB/20MB, file.upload paths
- [x] MainSecurityConfig 작성 (uploads/store GET public, owner OWNER, cart/order/payment CUSTOMER, review POST/PUT/DELETE 인증)
- [x] Store.xml + Owner.xml 이동 (namespace 변경)
- [x] Phase 0 임시 hello 컨트롤러 제거
- [x] mmg-common GlobalExceptionHandler에 `NoResourceFoundException` 핸들러 추가 (Spring 7.x 정적 리소스 미존재 → 404)
- [x] 검증 5종: GET /api/store 200 (마이그레이션 데이터 표시), /api/owner 401, /uploads 404 (정상)
- ⚠️ cross-schema JOIN endpoint는 예상대로 SQL 에러 — Phase 4 Feign으로 해결

### 2-C. Cart + Order + Payment 도메인 이동 ✅
- [x] `application/cart/*` → mmg-main-service (3 java + 6 model = 9 파일, 6 endpoints)
- [x] `application/order/*` → mmg-main-service (3 java + 7 model = 10 파일, 6 endpoints)
- [x] `application/payment/*` → mmg-main-service (3 java + 2 model = 5 파일, 1 endpoint)
- [x] json-simple 의존성 추가 (`com.googlecode.json-simple:json-simple:1.1.1`)
- [x] **PaymentService SECRET_KEY 환경변수화** — `private static final String` → `@Value("${toss.secret-key}")` (CLAUDE.md §6.10 보안 위반 해소)
- [x] application.yml에 `toss.secret-key: ${TOSS_SECRET_KEY}` 매핑
- [x] mapper xml 3개 이동 (Cart.xml, Order.xml, payment.xml) + namespace 변경
- [x] Order.xml의 cross-schema JOIN 2개에 TODO Phase 4 주석 추가
- [x] 검증: cart/order/payment endpoint 모두 401 반환 (CUSTOMER 권한 차단 정상)
- ⚠️ TOSS_SECRET_KEY .env 값은 placeholder 유지 — Phase 5 결제 본격 검증 시 학원 키로 교체

### 2-D. AddressSearch + MapConfig 이동 ✅ (Phase 1-B-3.5와 통합 진행)
- [x] `application/address/AddressSearchController/Service.java` → mmg-main-service/address
- [x] `application/address/MapConfigController.java` → mmg-main-service/address
- [x] `model/AddressSearchRes.java` 이동
- [x] 네이버 API 환경변수 매핑 (`naver.*` 5개 키 application.yml + .env)
- [x] 검증: GET /api/map/key 200 + NAVER_MAP_CLIENT_ID 응답

### 2-E. Review 도메인 신규 작성 ✅
- [x] `mmg-main-service/review/` 신규 — ReviewController(5 endpoints) + Service(5) + Mapper(9 methods) + Review.xml(9 SQL)
- [x] 모델: ReviewReq, ReviewRes, GetReviewReq
- [x] 엔드포인트 경로 `/api/user/review/...` 유지 (API 응답 스펙 동결)
- [x] BusinessException 적용 (FORBIDDEN/CONFLICT/NOT_FOUND)
- [x] 8/9 메서드 즉시 동작. getStoreReviews(Store.xml)만 user JOIN으로 Phase 4 Feign 후 정상화
- [x] owner_comment 테이블은 양쪽 DB 미존재 — DROP 작업 없음, decisions.md 기록

### 2-F. Rider/Admin 빈 schema 생성 ✅
- [x] my_mmg_rider DB 생성 (`utf8mb4_unicode_ci`)
- [x] my_mmg_admin DB 생성 (`utf8mb4_unicode_ci`)
- [x] 테이블은 Phase 5 신규 (rider_profile, FAQ, penalty 등)

### 2-G. main-service 통합 검증 ✅
- [x] auth + main + gateway 3개 서비스 동시 기동 검증
- [x] BFF 회원가입 흐름: POST /api/user/join (옵션 D-1) → POST /api/address (BFF 두번째) 200/200
- [x] 응답에 userNo + AT/RT 쿠키 자동 발급 검증
- [x] 가게 목록 (public) 200, NAVER MapConfig (인증) 200, Review (인증) 200
- [x] BusinessException → 401 "아이디/비번 틀렸습니다" ResultResponse 통일
- [x] user.status 컬럼 미존재 + 검증 로직 없음 — 옵션 D-1 막힘 0 확인
- [x] 검증 데이터 정리 (auth.user 15, main.address 20 baseline)
- ⚠️ Gateway 라우팅 prefix(`/api/auth/**`, `/api/main/**`)는 Phase 0-B 임시 — Phase 4-B에서 실제 경로(`/api/user`, `/api/store` 등)로 정비 예정
- ⚠️ cross-schema 의존 6개 endpoint(Store.xml/Order.xml/Owner.xml의 user JOIN)는 Phase 4 Feign 후 정상화 — 보류
- [x] **Phase 2 완료 커밋**

---

## Phase 3 — MyBatis → JPA 선별 마이그레이션 (하이브리드 영구 공존)

> 영속성 정책 = JPA + MyBatis 영구 공존. 단순 CRUD = JPA, 복잡 쿼리 = MyBatis. mybatis starter 영구 유지.

### 3-A. 정찰 + User 도메인 (auth-service) ✅ (2026-04-29)
- [x] 도메인별 쿼리 복잡도 분석 (단순 39 / 중간 16 / 복잡 18)
- [x] JPA 전환 우선순위 표 + Phase 3-A/B/C/D 분할
- [x] mmg-common: BaseEntity (`@MappedSuperclass + AuditingEntityListener`, created_at/updated_at)
- [x] mmg-auth-service: spring-boot-starter-data-jpa, ddl-auto=validate, MariaDBDialect, open-in-view=false
- [x] AuthApplication: @EnableJpaAuditing
- [x] User entity (@Entity, BaseEntity 미상속 — created_at/updated_at 컬럼 부재)
- [x] StringDateConverter (birth: DB DATE ↔ Java String "yyyy-MM-dd")
- [x] rank: `@Column(name = "\`rank\`")` (MariaDB 예약어)
- [x] UserRepository (findByUserId, existsByUserId, @Query constructor expression for UserBriefDto)
- [x] UserService 리팩토링 (Mapper → Repository, dirty checking UPDATE)
- [x] InternalUserController Repository 전환
- [x] UserMapper.java + User.xml 삭제 (mybatis starter는 유지)
- [x] 응답 스펙 검증 9/9 통과 (checkId/join/login/me/getUser/updateUser/internal 단건/batch/404)

### 3-B. Payment + Cart(+CartDetail) + LikedStore (main-service) ✅ (2026-04-29)
- [x] mmg-main-service: spring-boot-starter-data-jpa, MariaDBDialect, validate, open-in-view=false
- [x] MainApplication: @EnableJpaAuditing
- [x] 테스트 인프라 자산 (Phase 3-C/D 재사용): SnapshotAssert(JSONAssert STRICT), @SpringBootTest+MockMvc+@Transactional+@Rollback, root test workingDir + project.dir property
- [x] Phase 3-B-1 Payment: PaymentEntity @Entity (orderId String→Long, paymentTime DB DEFAULT), PaymentRepository.save, payment.xml에 existsByOrderId 잔존, 통합 테스트 3 케이스(주문 부재/금액 불일치/이미 결제됨)
- [x] Phase 3-B-2 LikedStore: @IdClass(LikedStoreId), LikedStoreRepository(existsBy/countBy/@Modifying delete), saveAndFlush 패턴, favoriteList(JOIN+LIMIT) MyBatis 잔존, 통합 테스트 4 케이스
- [x] Phase 3-B-3 Cart/CartDetail: 단순 CRUD 11 → JPA, JOIN 3 + 외부 호출 3 = 6 MyBatis 잔존, dirty checking 수량 합산, getLastCartId 제거(보존 정책 예외, decisions.md), 통합 테스트 5 케이스
- [x] BaseEntity 첫 검증 — 4 도메인 모두 audit 컬럼 부재로 미적용 → Phase 3-C `review`로 이전
- [x] 12 통합 테스트 STRICT snapshot 비교 통과 = 응답 JSON 1바이트 동결 + 하이브리드 트랜잭션 가시화 검증
- [ ] confirmPayment 정상 흐름 (토스 호출): Phase 5 TODO TossPaymentClient 추출과 함께

### 3-C. Order + OrderDetail + Review (main-service) ✅ (2026-04-29)
- [x] Phase 3-C-1: Orders @Entity + Persistable<Long> (manual ID assign 패턴 유지),
      OrderDetail @Entity, Repository 2개. OrderMapper 5 SQL 제거(insertOrder, insertOrderDetail,
      deleteOrder, maxHistoryPage, findItemsByOrderId), 7 SQL 잔존(외부 호출 3 + 복잡 3 + 외부 도메인 1).
      OrderDetailRepository @Query constructor expression(OrderHistoryDto.OrderItemDto). 통합 테스트 3.
- [x] Phase 3-C-2: 🎯 BaseEntity 첫 검증 — Review @Entity extends BaseEntity +
      @AttributeOverrides 2(write_at→createdAt, amended_at→updatedAt). saveAndFlush 후
      audit 자동 채움 + findById 재조회 보존 통과. 9 SQL 영구 잔존(다중 테이블 UPDATE/DELETE,
      JOIN, 집계, cross-table). 통합 테스트 2.
- [x] Phase 3-C-3: PaymentService.confirmPayment Cart/Order MyBatis 호출 6개 → JPA 위임
      (orderRepository.findById, dirty checking setPayState, cartRepository.findByUserNo,
      cartDetailRepository.deleteByCartId, cartRepository.delete, order.getUserNo()).
      CartMapper 최종 잔존 3, OrderMapper 최종 잔존 4. 회귀 0.
- [x] OrderHistoryReq @NoArgsConstructor 추가 (잠재 버그 수정, 응답 0 영향)
- [x] 16 통합 테스트 STRICT snapshot 통과 + 학원 DB 잔여 0 (Rollback 정상)

### 3-D. Store + Owner — MyBatis 유지 + UserAddress JPA ✅ (2026-04-29)
- [x] UserAddress @Entity + UserAddressRepository (6 SQL 모두 JPA, dirty checking 기반 update/setDefault)
- [x] @JdbcTypeCode(SqlTypes.NUMERIC) — DECIMAL(16,13) ↔ Double 매핑 (precision/scale 회피)
- [x] UserAddressMapper.java + Address.xml 완전 제거 (보존 정책 예외)
- [x] OrderMapper.findDefaultAddress 제거 → UserAddressRepository.findFirstDefaultByUserNo 위임
- [x] OrderMapper 최종 잔존 3 (복잡 영구만)
- [x] Store/Owner MyBatis 유지 확정 (Store 12 SQL + Owner 24 SQL — 복잡/동적/cross-table 영구)
- [x] AddressSearch는 외부 API (JPA 무관) — 검증만
- [x] CartMapper.findStoreNameByStoreId는 Store 도메인 경계 위반이지만 Store가 MyBatis 유지로 위치 이동 실효성 미미 → Phase 5에서 함께 검토
- [x] **Phase 3 완료 커밋** (Phase 3-A/B/C/D = 8 도메인 JPA 전환)

---

## Phase 4 — 서비스 간 통신 인프라

### 4-A. FeignClient 도입 ✅ (Phase 4 진입 우선순위 변경 — 옵션 3 채택)
- [x] mmg-common/feign/AuthFeignClient (interface) + dto/feign/UserBriefDto
- [x] auth-service Internal Controller 4 endpoints (validate/role은 단순 응답, user/users batch/owner)
- [x] AuthSecurityConfig `/internal/**` permitAll (Phase 4-B Gateway 차단 예정)
- [x] main-service: openfeign implementation + @EnableFeignClients
- [x] **5개 cross-schema SQL Feign 전환 완료**:
  - Store.xml `findOne` (가게 상세 ownerName)
  - Store.xml `getStoreReviews` (batch userName)
  - Owner.xml `getOrders` (batch customerName + tel)
  - Owner.xml `getStoreReviews` (batch userName)
  - Order.xml `findTelByUserNo` 제거 → OrderService에서 Feign 직접 호출
- [x] Order.xml `findDefaultAddress`는 자동 해결 (Phase 1-B-3.5 후 address가 main schema)
- [x] N+1 회피: batch endpoint `/internal/auth/users?ids=` 활용
- [x] 검증 통과: Internal API + 가게 상세(ownerName) + 리뷰 목록(userName batch) 정상 응답
- ⚠️ rider → main, admin → auth Feign은 Phase 5 진입 시 (rider/admin 도메인 구현 필요)
- ⚠️ **외부 FK 정합성 (Saga/Outbox)** — Phase 4-D 또는 Phase 6으로 분리 (write-side 정합성 별개 주제)

### 4-B. Gateway 라우팅 완성
- [ ] 모든 API 경로 매핑
- [ ] JWT 1차 검증을 Gateway에서 처리
- [ ] `/uploads/**` → main-service 라우팅

### 4-C. Redis 도입
- [ ] Redis 서버 구축
- [ ] 토큰 저장 (auth-service)
- [ ] 날씨 캐시 (main-service)
- [ ] Pub/Sub 채널 설계
- [ ] **Phase 4 완료 커밋**

---

## Phase 5 — 신규 기능 구현

### 5-A. 펫 시스템 (main-service)
- [ ] 엔티티/Repository 작성 (pet, pet_items, pet_inventory, pet_feedings)
- [ ] 펫 생성 API (회원가입 시 자동 지급)
- [ ] EXP/친밀도/레벨업 로직
- [ ] 간식 주기 (주문 → 자동 지급)

### 5-B. 챗봇 (main-service)
- [ ] chat_sessions, chat_messages 테이블
- [ ] Gemini API 연동 (mmg-common/gemini)
- [ ] 펫 챗봇 (entry_point=MYPET)
- [ ] CS 챗봇 (entry_point=CS)
- [ ] 4가지 톤 모드 분기
- [ ] 상담원 에스컬레이션 (status=ESCALATED)
- [ ] 무료 티어 호출 제한 대응 (분당 15회 / 일 1,500회)

### 5-C. 결제 — 토스페이먼츠 (main-service)
- [ ] 토스페이먼츠 테스트 키 발급
- [ ] 결제 위젯 연동 (Vue 프론트)
- [ ] 결제 승인 API (백엔드)
- [ ] 결제 취소/환불 API
- [ ] 결제 실패 처리
- [ ] 영수증 발급

### 5-D. 룰렛 게임 (main-service)
- [ ] roulette_results 테이블
- [ ] 등급별 확률 로직 (60/25/12/3)
- [ ] 1일 1회 무료 정책
- [ ] 결과 유효시간 2시간

### 5-E. 쿠폰 동시성 (main-service)
- [ ] 비관적 락 (`@Lock(PESSIMISTIC_WRITE)`)
- [ ] 선착순 쿠폰 발급 테스트

### 5-F. 라이더 서비스 구현
- [ ] 라이더 정보, 배달 처리 로직
- [ ] 실시간 위치 (Redis)
- [ ] SSE 알림

### 5-G. 관리자 서비스 구현
- [ ] 신고 처리 (자동 제재 3/5/7회)
- [ ] 정산 계산 (가게별 월별)
- [ ] FAQ, 시스템 공지

### 5-H. 추가 테이블 구현 (3개)
- [ ] `order_status_log` 트리거/서비스 레이어
- [ ] `search_history` 검색 시 INSERT
- [ ] `store_notices` CRUD
- [ ] **Phase 5 완료 커밋**

---

## Phase 6 — 고도화 (선택)

- [ ] Outbox 패턴 (이벤트 발행 안정성)
- [ ] Kafka 도입 (비동기 메시징)
- [ ] 분산 트랜잭션 (Saga 패턴)
- [ ] 모니터링 (Actuator + Prometheus + Grafana)
- [ ] 로그 중앙화 (ELK 또는 Loki)
- [ ] CI/CD 파이프라인
- [ ] Docker Compose / Kubernetes

---

## 📝 이슈 로그

### YYYY-MM-DD
- (이슈 발생 시 여기에 기록)

---

## 📊 통계

| Phase | 시작일 | 완료일 | 소요일 |
|---|---|---|---|
| Phase 0 | - | - | - |
| Phase 1 | - | - | - |
| Phase 2 | - | - | - |
| Phase 3 | - | - | - |
| Phase 4 | - | - | - |
| Phase 5 | - | - | - |
| Phase 6 | - | - | - |
