# 뭐물꼬 MSA 코드 리뷰 / 자기 코드 설명서

> **대상**: 본인이 작성·합작한 `MOMOOLGGO_MSA` 백엔드
> **작성 일자**: 2026-05-31
> **목적**: "이건 왜 이걸 썼지?" "이게 어떻게 흘러가지?" 를 다시 읽고 이해할 수 있게 정리
> **읽는 법**: 위에서 아래로. 1장(왜 MSA) → 2장(공통 인프라) → 3장(요청 흐름) → 4장(도메인 흐름) 순서.

---

## 1. 왜 MSA로 갔는가 (기획 의도)

원래는 모놀리식 Spring Boot + MyBatis 프로젝트(`MOMOOLGGO`)였음. MSA로 바꾼 이유는 단 하나가 아니라 **여러 가지가 겹쳐서**:

1. **팀원 분담을 코드 충돌 없이** 하고 싶었음. 같은 jar 안에서 5명이 같은 Controller 건드리면 매번 conflict. 모듈 분리 → 각자 자기 서비스만 만지면 됨.
2. **포트폴리오용 기술 스택**. MSA + Spring Cloud Gateway + Feign + JPA + Redis는 학원 프로젝트치고 흔치 않은 조합.
3. **장애 격리 연습**. 라이더 서비스가 죽어도 가게/주문 API는 살아 있게.
4. **DB 스키마도 같이 쪼개기**. 1 DB · 4 schema (auth/main/rider/admin). 코드만 쪼개고 DB는 그대로 두는 가짜 MSA가 아니라, **schema 경계도 진짜로 자른** MSA.

대신 비용도 컸음:
- 서비스 간 호출이 단순 메서드 호출에서 **HTTP/Feign 호출**로 바뀜 → DTO 따로 만들어야 하고, 직렬화 비용 발생
- **JOIN을 못 씀**. 다른 schema 데이터 필요하면 Feign으로 가져와서 메모리에서 조합
- **로컬 트랜잭션이 깨짐**. 한쪽 commit 후 Feign 실패하면 일관성 깨짐 (탈퇴 / 결제 / 배차 같은 데서 직접 겪은 문제)

이 비용 때문에 곳곳에 등장하는 게 **Saga / Outbox / best-effort fallback** 같은 패턴. 4장 도메인 흐름에서 다시 등장.

### 1.1 모듈 구조 (정답이라 외워두기)

```
mmg-common  ── (모두가 의존)
  ↓
mmg-auth-service   (8081)  ── 회원/JWT
mmg-main-service   (8080)  ── 가게/메뉴/주문/결제/리뷰/펫/쿠폰/챗봇  ← 가장 큼
mmg-rider-service  (8082)  ── 라이더/배달/근무/정산
mmg-admin-service  (8083)  ── 관리자/신고/제재/통합 정산/FAQ/공지
mmg-gateway        (8000)  ── 외부 단일 진입점
```

**철칙 3개** (`CLAUDE.md §6`):
1. 서비스 → 다른 서비스 코드 **import 금지**. 통신은 Feign(HTTP)으로만.
2. 다른 schema 직접 **JOIN 금지**. 필요하면 Feign으로 데이터 가져와서 메모리에서.
3. 외부 schema FK는 **논리 FK만** (물리 `FOREIGN KEY` 제약 X). 정합성은 애플리케이션에서.

---

## 2. mmg-common — 왜 공통 라이브러리가 따로 있나

각 서비스가 똑같이 필요한 게 있음: **JWT 검증**, **예외 처리**, **응답 포맷**, **BaseEntity**, **Feign DTO**. 이걸 다섯 번 복붙하면 일관성이 깨지니까 `mmg-common`에 모아둔 것.

### 2.1 `BaseEntity` — JPA Auditing

파일: `mmg-common/.../common/entity/BaseEntity.java`

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

**왜 이렇게?**
- 같은 컬럼(`created_at`/`updated_at`)을 가진 테이블이 많음. 매번 `@PrePersist` 쓰는 게 귀찮음.
- `AuditingEntityListener`가 JPA가 INSERT/UPDATE할 때 자동 채워줌.
- 단, **`@EnableJpaAuditing`을 각 서비스 Application 클래스에 추가해야 활성화**. (auth, main, rider, admin 다 붙어있음. 확인: `AuthApplication.java:9`, `RiderApplication.java:9`)

**함정**: MyBatis로 INSERT/UPDATE 할 때는 자동 안 채워짐. SQL에 `NOW()` 직접 적어야 함. 그래서 같은 테이블을 JPA + MyBatis 하이브리드로 쓰면 둘 다 챙겨야 함 (Phase 3-B Cart 사례).

### 2.2 JWT 3종 세트

파일들:
- `JwtTokenProvider.java` — 토큰 생성/파싱 (raw library 호출)
- `JwtTokenManager.java` — 쿠키 입출력 + SecurityContext 변환 (편의 wrapper)
- `TokenAuthenticationFilter.java` — 매 요청 시 쿠키 → SecurityContext 자동 세팅

**왜 세 개로 쪼개?**
- **Provider**: 단순 변환기. JWT 라이브러리(`jjwt`) 직접 다룸. 다른 코드는 이걸 모름.
- **Manager**: 도메인 매니저. "쿠키에 토큰 세팅해줘", "쿠키에서 꺼내서 SecurityContext로 변환해줘" 같은 동사 단위 API 제공.
- **Filter**: Spring Security 필터 체인의 일원. 매 요청마다 한 번씩 실행되어 인증 상태 결정.

**페이로드 구조** (`JwtTokenProvider:43-57`):
```java
.claim("signedUser", makeClaimByUserToJson(jwtUser))
```
표준 claim에 안 박고 `signedUser`라는 custom claim에 `JwtUser` 객체를 통째로 JSON으로 직렬화해서 넣음. 이유는 `JwtUser`에 `userNo`, `role`, `status`, `name`이 다 들어가서 API마다 매번 DB 조회 안 해도 되도록.

**쿠키로 다루는 이유**:
- HttpOnly 쿠키 → JS에서 못 읽음 → XSS로 토큰 탈취 어려움
- 프론트가 `Authorization: Bearer ...` 헤더 직접 세팅 안 해도 됨 (브라우저가 알아서 보냄)
- 단점: CSRF 방어 필요. 지금은 `csrf().disable()` + SameSite 쿠키로 대충 막는 중. 운영 가면 SameSite=Strict 또는 토큰 분리 필요.

**조용히 실패하는 필터** (`TokenAuthenticationFilter:36-40`):
```java
} catch (Exception e) {
    log.warn("JWT 인증 실패 (무시): {}", e.getMessage());
}
filterChain.doFilter(request, response);
```
잘못된/만료된 JWT가 와도 필터가 throw 안 함. **단순히 SecurityContext를 비워두고 다음 필터로 넘김**. 그러면 그 다음에 `.anyRequest().authenticated()`에서 막혀서 401이 응답됨. 의도된 동작 — 만료 JWT에서 500 안 나오게.

### 2.3 `BaseSecurityConfig` — 시큐리티 공통 베이스

파일: `mmg-common/.../common/security/BaseSecurityConfig.java`

핵심 패턴 두 가지:
1. **`@ConditionalOnClass(SecurityFilterChain.class)`** — Spring Security 의존성이 classpath에 있는 서비스에서만 활성. Gateway처럼 시큐리티 안 쓰는 서비스는 자동으로 비활성.
2. **`@ConditionalOnMissingBean(SecurityFilterChain.class)`** — 자식 서비스(auth, main, rider, admin)가 자기만의 `SecurityFilterChain`을 등록하면 기본 체인은 자동 비활성. 자식이 없으면 default(`anyRequest().permitAll()`) 적용.

`applyCommon()` 메서드(`BaseSecurityConfig:49-61`)가 공통 보안 설정을 다 묶어 둠:
- CORS 적용
- 세션 `STATELESS` (JWT 쓰니까 세션 X)
- CSRF / httpBasic / formLogin 끄기
- 401/403 응답을 JSON으로 (`JsonAuthenticationEntryPoint`, `JsonAccessDeniedHandler`)
- `TokenAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 끼움

자식이 할 일은 단 한 줄: `base.applyCommon(http).authorizeHttpRequests(...).build()`. `AuthSecurityConfig.java`가 그 예시.

### 2.4 `GlobalExceptionHandler` — 응답 통일

파일: `mmg-common/.../common/exception/GlobalExceptionHandler.java`

모든 서비스가 같은 응답 포맷 쓰게 강제:
```json
{ "resultMessage": "...", "resultData": ... }
```
(이게 `ResultResponse<T>`. 프론트가 `resultData`만 까서 쓰면 됨.)

5단 우선순위:
1. `BusinessException` → status는 예외가 결정 (400/401/403/404/409 등). 가장 흔히 던지는 예외.
2. `MethodArgumentNotValidException` → `@Valid` 실패 → 400
3. `HttpMessageNotReadableException` → JSON 파싱 실패 → 400
4. `NoResourceFoundException` → 정적 리소스 404
5. **`JwtException` → 401** (만료 RT가 흔한 정상 시나리오라 500 안 나오게)
6. `RuntimeException` → 500 fallback
7. `Exception` → 500 최후

**왜 BusinessException으로 status를 같이 들고 다님?** Spring의 `@ResponseStatus`는 컴파일 타임 고정. 같은 예외 클래스로 400도 401도 던지고 싶으면 status를 인스턴스 필드로 갖고 다니는 게 편함. 그래서 `throw new BusinessException("메시지", HttpStatus.CONFLICT)` 패턴이 곳곳에 깔려 있음.

---

## 3. 요청 흐름 3가지

### 3.1 외부 클라이언트 → Gateway → 서비스 (가장 기본)

```
브라우저 (localhost:5173)
   │   GET /api/store/list  (쿠키 자동 첨부)
   ▼
Gateway (localhost:8000)
   │   CorsFilter → preflight OK
   │   application.yml의 routes 매칭: /api/store/** → http://localhost:8080
   │   X-Forwarded-* 헤더 추가하고 그대로 forward
   ▼
main-service (localhost:8080)
   │   TokenAuthenticationFilter → 쿠키 ACCESS_TOKEN 파싱 → SecurityContext에 UserPrincipal 세팅
   │   SecurityFilterChain → /api/store/list가 permitAll인지 authenticated인지 확인
   │   StoreController → StoreService → MyBatis or JPA
   ▼
   ResponseEntity<ResultResponse<...>>
   ↑ 거꾸로 같은 길 (Gateway가 CORS 헤더 추가)
```

Gateway의 라우팅 규칙은 **`application.yml`에 평면적으로 나열** (`mmg-gateway/.../application.yml:9-100`). 각 라우트는 `Path=/api/xxx/**` predicate로 매칭. 라우트 순서 중요 — `/api/user/review/**`가 `/api/user/**`보다 먼저 와야 충돌 안 남 (실제로 그렇게 되어 있음, `application.yml:10-18`).

**Gateway가 안 하는 것**:
- JWT 검증 (각 서비스가 알아서. Gateway는 dumb pipe)
- 인가 (서비스 SecurityFilterChain에서)
- 토큰 발급 (auth-service의 책임)

이게 좀 단순한 Gateway지만, Phase 4-B 결정으로 의도된 것. JWT 검증을 Gateway에서 또 하면 코드 중복. CLAUDE.md §3에 "Gateway는 1차 인증"이라 적혀있지만 실제로는 라우팅 + CORS 만 함.

### 3.2 JWT 인증 흐름 (로그인 → 매 요청 인증 → 만료 시 재발급)

#### 3.2.1 로그인 (`POST /api/user/login`)

```
Frontend → Gateway → auth-service (UserController.signin)
    │
    ├─ UserService.signin:
    │     userRepository.findByUserId(...) → User
    │     passwordEncoder.matches() 검증
    │     status != ACTIVE면 BusinessException
    │     JwtUser 생성 (userNo, role, status, name)
    │     issueAndStoreTokens(res, jwtUser):
    │         AT 생성 → 쿠키 ACCESS_TOKEN (1296000000ms = 15일)
    │         RT 생성 → 쿠키 REFRESH_TOKEN (15일)
    │         refreshTokenStore.save(userNo, RT, 15일)  ← Redis
    │
    └─ Set-Cookie 응답 (AT, RT 둘 다)
```

**왜 RT를 Redis에 또 저장?** (Phase 4-C 결정)
- 쿠키 RT만 있으면 사용자가 "로그아웃했어요" 해도 RT 자체는 만료 전까지 유효. 위조/탈취 위험.
- Redis에 "현재 유효한 RT"를 박제 → reissue 때 쿠키 RT와 Redis RT를 **비교**. 다르면 401.
- 로그아웃 시 Redis 키 삭제 → 누가 그 RT로 reissue 시도해도 막힘.
- 이게 `refreshTokenStore.get(userNo).orElseThrow(...)` 패턴 (`UserService:219-225`).

#### 3.2.2 인증된 요청 (`GET /api/user/me`)

```
브라우저 → Gateway → auth-service
    │
    ├─ TokenAuthenticationFilter:
    │     쿠키 ACCESS_TOKEN 꺼내기
    │     JwtTokenProvider.getJwtUserFromToken(token) → JwtUser (서명 검증 + JSON 파싱)
    │     UserPrincipal(jwtUser) 만들기
    │     SecurityContextHolder에 세팅
    │
    ├─ SecurityFilterChain:
    │     /api/user/me는 .anyRequest().authenticated() → SecurityContext에 인증 있으면 통과
    │
    ├─ UserController.getMe(@AuthenticationPrincipal UserPrincipal principal):
    │     principal.getSignedUserNo() → userNo 추출
    │     userService.getMe(userNo)
    │
    └─ 응답
```

**핵심**: SecurityContext에 박힌 `UserPrincipal`은 매 요청마다 새로 만들어짐 (StatelessSession). 내부적으로 `JwtUser`를 들고 있고, `getSignedUserNo()` / `getRole()` 등으로 꺼내 씀.

#### 3.2.3 AT 만료 → RT로 재발급 (`POST /api/user/reissue`)

```
브라우저 (AT 만료된 쿠키 + 살아있는 RT 쿠키) → reissue
    │
    ├─ UserService.reissue:
    │     쿠키 RT 꺼내기 (null이면 401)
    │     JwtTokenProvider.getJwtUserFromToken(RT) → JwtUser (만료/위조면 JwtException → 401)
    │     refreshTokenStore.get(userNo) → 저장된 RT  (없으면 "로그아웃됐어요" 401)
    │     쿠키 RT == 저장 RT 비교 (불일치면 위조 의심 → 401, 재로그인 강제)
    │     User 조회 → status ACTIVE 검증
    │     새 AT만 발급해서 쿠키에 세팅. RT는 그대로 둠.
    │
    └─ 응답
```

**왜 RT를 새로 만들지 않음?** RT는 한번 발급하면 만료까지 그대로 두는 정책. 매번 갱신하면 활동 중인 사용자는 영원히 로그아웃 안 되는 셈. 보안과 UX 트레이드오프 — 지금은 단순함 우선.

**현재 만료 시간이 비현실적인 이유** (`.env`):
- 액세스 15일, 리프레시 15일 — `1296000000ms`
- 학원 발표/시연 환경에서 시연 중 만료되면 곤란. 운영 가면 액세스 30분 / 리프레시 14일로 줄일 예정 (CLAUDE.md §2 명시).

### 3.3 서비스 간 통신 — Feign + Internal API (Gateway 안 거침)

문제: auth-service가 회원가입 직후 main-service의 `pets` 테이블에 펫 1마리 자동 지급하고 싶음. 어떻게?

**답**: auth → main을 직접 HTTP 호출. Gateway 거치지 않고 `http://localhost:8080/internal/pet/init` 직타.

```
auth-service.UserController.signup(...)
    │
    ├─ userService.signup → User INSERT + JWT 발급
    └─ triggerPetInit(userNo):
            mainPetClient.initPet(new PetInitReq(userNo))  ← @FeignClient
                │
                │ HTTP POST http://localhost:8080/internal/pet/init
                │
                ▼
            main-service.InternalPetController.initPet(req)
                │
                ▼
            PetService.getOrCreatePet(userNo)
                │
                ▼
            INSERT INTO pets (...)
```

Feign 인터페이스 정의 (`MainPetClient.java:10-17`):
```java
@FeignClient(name = "mmg-main-pet", url = "${feign.main-service.url:http://localhost:8080}")
public interface MainPetClient {
    @PostMapping("/internal/pet/init")
    ResultResponse<PetInitRes> initPet(@RequestBody PetInitReq req);
}
```

Spring Cloud OpenFeign이 이 인터페이스의 동적 프록시를 만들어줌. 호출하면 알아서 HTTP 요청.

#### 3.3.1 왜 `/internal/**` prefix를 따로?

서비스 간 통신용 endpoint(`/internal/pet/init`, `/internal/order/...`, `/internal/user/...`)는 **외부에 절대 노출하면 안 됨**. 인증 우회 가능.

방어 2단:
1. **Gateway에 `/internal/**` 라우트를 안 만듦** + `InternalBlockController`가 명시적 403 응답 (`mmg-gateway/.../InternalBlockController.java`). 외부에서 `localhost:8000/internal/...` 와도 무조건 403.
2. 서비스 간 통신은 **각 서비스 포트로 직타** (Feign URL이 `localhost:8081`, `localhost:8080` 등). Gateway 안 거치니까 Gateway 차단 룰을 우회 못 함. 동시에 Gateway 차단 룰이 막을 일도 없음.

#### 3.3.2 회원 탈퇴 — Feign 다발성 호출의 예 (4장 미리보기)

`UserService.withdraw(...)` 안에서:
1. 로컬: User status = WITHDRAWN, withdrawnAt 박제
2. Feign: `mainUserCleanupClient.cleanupWithdrawnUser(userNo)` — main의 cart, likedstore 같은 데이터 정리
3. Feign: `riderUserClient.hasActiveWork(userNo)` — rider에 진행 중 배달 있는지 확인
4. Feign: `adminSettlementClient.hasUnpaidStoreSettlement(...)` — admin에 미정산 있는지

이 4단계가 다 같은 트랜잭션이 아님. **2번 Feign이 실패하면 1번 commit은 이미 됨**. 그래서 cleanup 실패 시 main에 잔여 데이터가 남는 일관성 문제. 현재는 Phase 4-A의 한계로 그냥 throw — 사용자 본다는 결과: 탈퇴 실패. **나중에 Saga / Outbox 패턴으로 보완 예정** (CLAUDE.md §9 Phase 6).

---

## 4. 도메인 흐름 — 핵심 3개

### 4.1 회원가입 BFF 패턴

`POST /api/user/join` 한 방으로:
1. auth-service: User INSERT (auth schema)
2. auth-service: JWT 발급 (즉시 로그인 상태)
3. auth-service → main-service Feign: 펫 자동 지급
4. (OWNER일 때만) auth-service → main-service Feign: owner_profile INSERT

이게 **BFF(Backend-For-Frontend) 패턴**. 프론트는 join 한 번만 호출하면 됨. 서버 안에서 여러 서비스로 fan-out.

**왜 펫 자동 지급은 best-effort?** (`UserController.triggerPetInit:57-65`)
```java
private void triggerPetInit(Long userNo) {
    if (userNo == null) return;
    try {
        mainPetClient.initPet(new PetInitReq(userNo));
    } catch (Exception e) {
        log.warn("펫 자동 지급 Feign 실패 (lazy fallback 진행) — userNo={}, cause={}",
                userNo, e.getMessage());
    }
}
```

- 펫이 없어도 회원가입 자체는 성공. 사용자가 처음 펫 페이지 진입할 때 `PetService.getOrCreatePet`이 알아서 만들어줌(lazy fallback).
- 회원가입을 펫 INSERT 때문에 실패시키면 UX 손해 큼.

**OWNER owner_profile은 best-effort 아님** (`UserService.signup:125-136`):
- OWNER는 사업자 정보가 핵심. owner_profile 없으면 가게 등록 못 함.
- 그래서 Feign 실패하면 그대로 예외 → 트랜잭션 롤백 → User INSERT까지 취소.
- 단, **Feign 실패 시점에서 자기 서비스 트랜잭션은 commit 안 됨** (JPA dirty flush 전). 외부 호출 후 commit 순서라 안전. 이건 좋은 사례.

### 4.2 주문 → 결제 → 배차 → 배달 → 완료 → 정산

```
Customer       Main(주문/결제)      Rider(배달)        Main(상태동기화)
   │                 │                  │                    │
   │ POST /api/cart  │                  │                    │
   │────────────────▶│                  │                    │
   │                 │ Cart INSERT      │                    │
   │                 │                  │                    │
   │ POST /api/order │                  │                    │
   │────────────────▶│                  │                    │
   │                 │ Order INSERT     │                    │
   │                 │ order_state=1   │                    │
   │                 │                  │                    │
   │ POST /api/payment/toss/confirm     │                    │
   │────────────────▶│                  │                    │
   │                 │ TossPayments 호출 │                    │
   │                 │ Payment INSERT   │                    │
   │                 │ pay_state=2     │                    │
   │                 │                  │                    │
   ┄  (사장 수락)     │                  │                    │
   │                 │ order_state=3   │                    │
   │                 │ ─Feign─────────▶│                    │
   │                 │   POST /internal/rider/delivery       │
   │                 │                  │ Delivery INSERT   │
   │                 │                  │ status=WAITING_   │
   │                 │                  │       ASSIGN      │
   │                 │                  │ delivery_log SYS  │
   │                 │                  │ SSE broadcast     │
   │                 │                  │                    │
   │             (라이더 수락)          │                    │
   │                 │                  │ DeliveryService.  │
   │                 │                  │   claimDelivery   │
   │                 │                  │ status=ASSIGNED   │
   │                 │                  │ riderNo=...       │
   │                 │ ◄─Feign──────────│                    │
   │                 │   PUT /internal/order/{id}/status     │
   │                 │ order_state=4   │                    │
   │                 │                  │                    │
   ┄  ...픽업/이동/배달 완료...        │                    │
   │                 │                  │                    │
   │                 │                  │ completeDelivery  │
   │                 │                  │ status=DELIVERED  │
   │                 │                  │ settlement upsert │
   │                 │ ◄─Feign──────────│                    │
   │                 │ order_state=6   │                    │
   │                 │                  │                    │
   │ POST /api/user/review              │                    │
   │────────────────▶│                  │                    │
   │                 │ Review INSERT   │                    │
```

**주의해야 할 비대칭**:
- main의 `orders.order_state`는 **숫자 1~6** (legacy). 의미: 1주문수락전 / 2취소 / 3조리중 / 4배차중 / 5배차완료 / 6배달완료.
- rider의 `delivery.status`는 **문자 enum 7개**: WAITING_ASSIGN / ASSIGNED / ARRIVED_AT_STORE / AWAITING_PICKUP / PICKED_UP / DELIVERING / DELIVERED.

두 개를 매번 매핑해서 sync해야 함. Phase 6에서 통일 후보지만 지금은 그대로.

### 4.3 Delivery 상태 머신 — 이 코드의 백미

파일: `mmg-rider-service/.../delivery/DeliveryService.java`

```
WAITING_ASSIGN ──── (라이더 claim or admin assign) ───▶ ASSIGNED
                                                              │
                            ┌─────────────────────────────────┤
                            │                                  │
                            ▼                                  ▼
                  ARRIVED_AT_STORE                    AWAITING_PICKUP (직행)
                            │                                  │
                            └──────────▶ AWAITING_PICKUP ◀────┘
                                                  │
                                                  ▼
                                            PICKED_UP
                                                  │
                                                  ▼
                                          DELIVERING
                                                  │
                                                  ▼
                                          DELIVERED  (terminal)

  + ASSIGNED/ARRIVED/AWAITING/PICKED/DELIVERING → WAITING_ASSIGN  (reject or cancel)
```

핵심 코드 (`DeliveryService:81-99`):
```java
EnumMap<DeliveryStatus, Set<DeliveryStatus>> map = new EnumMap<>(DeliveryStatus.class);
map.put(DeliveryStatus.WAITING_ASSIGN, EnumSet.of(DeliveryStatus.ASSIGNED));
map.put(DeliveryStatus.ASSIGNED, EnumSet.of(ARRIVED_AT_STORE, AWAITING_PICKUP, WAITING_ASSIGN));
// ...
ALLOWED_TRANSITIONS = Map.copyOf(map);
```

**왜 이렇게 화이트리스트로?**
- 라이더가 임의로 DELIVERED로 점프하는 걸 차단
- 상태 누락 없이 단계 박제 (assigned_at, picked_at, delivered_at 시각 기록)
- 코드만 보고도 어떤 전이가 가능한지 명백 — 유지보수성

**낙관적 락 패턴** (`DeliveryService:143-149`):
```java
try {
    deliveryRepository.saveAndFlush(delivery);
} catch (ObjectOptimisticLockingFailureException e) {
    throw new BusinessException("동시 변경 충돌이 발생했습니다. ...", HttpStatus.CONFLICT);
}
```

- `Delivery` 엔티티에 `@Version` 컬럼 있음 (`delivery.version`). JPA가 UPDATE할 때 `WHERE version = ?`로 비교.
- 두 라이더가 동시에 같은 배달을 잡으면 한 명만 성공, 다른 한 명은 `ObjectOptimisticLockingFailureException` → 409 응답.
- **`saveAndFlush`를 명시한 이유**: 기본 `save`는 lazy commit이라 예외가 트랜잭션 끝에서 터짐 (Service 메서드 밖). `saveAndFlush`로 메서드 안에서 즉시 발생시켜야 try-catch로 잡을 수 있음.

**`performRiderTransition` helper** (`DeliveryService:623-662`)

6개 transition (accept, arrive, pickup, depart, complete, reject, cancel)이 거의 같은 패턴이라 helper 메서드로 묶음:
1. delivery 로드
2. RIDER 권한 검증 (본인 배달인가?)
3. 화이트리스트 검증
4. `beforeChange.accept(delivery)` — 상태 외 컬럼 변경 콜백 (markDelivered, unassignRider 등)
5. `changeStatus(to, now)` — 상태 + 단계별 시각 박제
6. `saveAndFlush` (낙관적 락 try-catch)
7. `delivery_log` INSERT (같은 트랜잭션)

이 패턴이 깔끔한 이유: **6개 endpoint가 비슷한데 다른 만큼만 다르게 표현**. 람다(`Consumer<Delivery>`)로 차이만 주입.

### 4.4 회원 탈퇴 — Cross-service Saga의 현실

`UserService.withdraw(...)` 안에서 일어나는 일 (4장 서두에서 말한 것 풀이):

```
1. Local: User 조회 + 비밀번호 검증
2. Local: 진행 중 업무 확인 (Feign 호출, role별 분기)
     CUSTOMER → mainUserCleanupClient.hasActiveOrders(userNo)
     OWNER    → mainUserCleanupClient.checkOwnerWithdraw(userNo) + adminSettlementClient.hasUnpaidStoreSettlement
     RIDER    → riderUserClient.hasActiveWork(userNo)
   (어느 하나라도 true면 BusinessException — 탈퇴 차단)
3. Local: user.status = WITHDRAWN, withdrawnAt = now()
4. Feign: mainUserCleanupClient.cleanupWithdrawnUser(userNo) — main의 cart/likedstore 정리
5. Local: signout(userNo, res) — Redis RT 삭제 + 쿠키 만료
```

문제점:
- 3번 commit 시점은 메서드 끝 (트랜잭션 boundary). 4번 Feign이 3번 commit **전**에 호출됨.
- 만약 4번 Feign 호출은 성공했는데 3번 commit이 실패하면 → main에 cleanup 됐는데 auth는 ACTIVE 그대로. 정합성 깨짐.
- **현재 코드는 이 케이스를 안 막음**. JPA dirty flush가 cleanup 호출보다 늦게 일어남 (메서드 종료 시).

이게 분산 트랜잭션의 본질적 어려움. Phase 6에서 Outbox 패턴(이벤트 테이블 + 별도 publisher)으로 보완 예정.

### 4.5 정산 자동화 — 이벤트 패턴

`DeliveryService.completeDelivery:528-535`:
```java
DeliveryTransitionResult result = performRiderTransition(...);
settlementService.recalculateThisWeek(result.riderNo());
```

같은 트랜잭션 안에서 settlement도 UPSERT. 라이더가 한 건 배달 완료할 때마다 그 주 정산이 자동 갱신.

추가로 SSE(Server-Sent Events) broadcast (`SettlementSseListener` 계열). 라이더가 자기 페이지 열고 있으면 정산 액수가 실시간 갱신.

같은 트랜잭션 안이라 settlement 실패 시 transition도 롤백 — **정합성 안전**. 데이터가 어긋날 위험이 없음.

---

## 5. 영속성 — JPA와 MyBatis 동거하는 이유

`CLAUDE.md §5` 선택 기준 표:

| 기준 | 선택 |
|---|---|
| 단일 엔티티 CRUD | **JPA** |
| 단순 조건 조회 | JPA + Spring Data 메서드명 |
| 동적 WHERE | JPA + QueryDSL **또는** MyBatis |
| 복잡한 다중 JOIN | **MyBatis** |
| 통계/집계 쿼리 | **MyBatis** |
| 네이티브 함수 사용 | **MyBatis** |

**Phase 3에서 JPA로 옮긴 도메인 8개**:
- auth: `user`
- main: `Payment`, `LikedStore`, `Cart/CartDetail`, `Order/OrderDetail`, `Review`, `UserAddress`

**MyBatis 영구 유지 도메인**:
- main: `Store` (12 SQL — 가게 검색/추천/통계 다중 JOIN), `Owner` (24 SQL), `Category`, `Menu`, `Menu_Category`, `Coupon_reward_code`

이걸 왜 굳이 하이브리드로?
- 학원 모놀리식 원본에서 그대로 가져온 SQL이 너무 많음. 전부 JPA로 옮기면 학기 끝남.
- MyBatis는 SQL 그대로 보여서 디버깅이 쉬움. 통계 쿼리는 SQL 가독성 우선.
- 단순 CRUD는 JPA가 압도적으로 적게 씀. 같은 엔티티에 6번 INSERT/UPDATE 패턴이라면 JPA가 적합.

**함정**:
1. **같은 테이블을 JPA + MyBatis로 동시 쓰면 1차 캐시 정합성 깨짐**. JPA로 UPDATE하고 같은 트랜잭션에서 MyBatis로 SELECT하면 옛날 값을 봄. 해결: `entityManager.flush()` 후 MyBatis로.
2. **BaseEntity auditing은 JPA에서만 동작**. MyBatis INSERT 시 `NOW()` 명시 필요. Cart/CartDetail이 이 사례.
3. **JPA `@Version` 낙관적 락은 MyBatis UPDATE를 감지 못 함**. Delivery는 JPA만 쓰니까 안전.

---

## 6. 알아두면 좋은 함정/패턴

### 6.1 `@AuthenticationPrincipal` 패턴

Controller에서 로그인한 사용자 정보 받는 표준 방식 (`UserController` 곳곳):
```java
public ResultResponse<UserSigninRes> getMe(@AuthenticationPrincipal UserPrincipal principal) {
    if (principal == null) {
        return new ResultResponse<>("로그인이 필요합니다.", null);
    }
    return new ResultResponse<>("조회 성공", userService.getMe(principal.getSignedUserNo()));
}
```

- `UserPrincipal`은 `mmg-common`에 정의되어 있고 내부에 `JwtUser`를 갖고 있음
- `principal.getSignedUserNo()`로 userNo 추출
- request body에서 userNo를 받지 않는 게 **포인트**: 받으면 위조 가능 (다른 사람 user_no로 자기 정보처럼 조작). SecurityContext에서만 꺼냄.

DeliveryService도 같은 패턴 (`callerUserNo` 매개변수로 강제) — `DeliveryService:114, 117`.

### 6.2 `@Transactional` 위치

- **`@Transactional(readOnly = true)`** — 조회 메서드. JPA는 dirty checking을 off로 두어 성능 향상.
- **`@Transactional`** — 쓰기 메서드. 메서드 안에서 예외 throw하면 자동 롤백.
- **Feign 호출은 트랜잭션 밖에 두는 게 안전**하지만, 지금 코드는 메서드 안에서 같이 호출하는 경우 많음. 이게 4.4의 일관성 문제 근원.

### 6.3 `@ConditionalOnClass` / `@ConditionalOnMissingBean` 트릭

`mmg-common`의 빈들이 모든 서비스에 강제로 등록되면 곤란 (Gateway엔 Spring Security 없는데 Security 빈이 등록되려고 시도 → 에러). 그래서:
- `@ConditionalOnClass(SecurityFilterChain.class)` — 클래스가 classpath에 있을 때만 활성
- `@ConditionalOnMissingBean(SecurityFilterChain.class)` — 자식 서비스가 자기 빈 등록 안 했을 때만 default 활성

이런 옵션 패턴 덕에 같은 `mmg-common`을 5개 서비스에서 부담 없이 import 가능. Spring Boot 자동 설정 mechanism의 기본기.

### 6.4 `MainXxxClient` / `XxxInternalClient` 네이밍

```
auth/feign/MainPetClient.java        ← auth가 main을 부르는 Feign
auth/feign/MainOwnerProfileClient.java
auth/feign/RiderUserClient.java
rider/feign/AuthInternalClient.java  ← rider가 auth를 부르는 Feign
rider/feign/MainInternalClient.java
```

규칙: `{호출 대상 서비스}{도메인}Client`. 호출자 패키지에 둠. 호출 대상의 `/internal/**` endpoint와 1:1 매핑.

### 6.5 `validate` vs `update` ddl-auto

- `mmg-auth-service`, `mmg-main-service`, `mmg-rider-service` → `validate` (스키마 변경 X, 엔티티-DB 일치 검증만)
- **`mmg-admin-service` → `update`** (JPA가 자동으로 스키마 변경)

admin이 `update`인 이유는 Phase 5 신규 도메인이라 DDL 파일을 별도로 안 만들고 JPA가 알아서 만들게 둔 것. **이게 운영 환경에선 위험** — 컬럼 잘못 추가/이름 변경하면 prod DB도 같이 바뀜. 다행히 `application-prod.yml`은 모두 `none`으로 박혀 있음.

### 6.6 `eco_selected`, `green` — 도메인 기획의 흔적

`orders.eco_selected`, `user.green` — 친환경 포인트 시스템. 친환경 선택 시 포인트 적립, 일정 단계 도달하면 쿠폰(`coupon_reward_code`)이 자동 발급. 이게 펫 시스템과 같이 차별화 포인트.

---

## 7. 의문점 / 부채 / 다음에 정리하면 좋을 것

(코드 보면서 발견한 정리 후보. 우선순위 순.)

1. **`DeliveryService`의 5개 캐시되어 있는 `inProgress` 리스트 (3번 중복 정의)**. `getRiderInternalStatus`, `getMyInProgressDeliveries`, 그리고 enum 변환 보러 다시 보임. enum 상수로 빼면 깔끔.
2. **회원 탈퇴 흐름의 commit/Feign 순서**. 현재 main cleanup이 auth commit 전. Saga 또는 명시적 순서 조정 필요.
3. **`orders.order_state`(int) vs `delivery.status`(enum)** 매핑 코드가 main↔rider 양쪽에 중복. 한쪽 표준으로 통일하면 sync 코드가 줄어듦.
4. **`admin-service` ddl-auto=update**. 위에서 말한 위험. Phase 5 마무리되면 schema 박제 후 validate로.
5. **JWT 액세스 15일**. 운영 가기 전 30분으로 줄여야 함. 줄이면 reissue 로직이 실전에서 처음으로 빡세게 돌아갈 것 — 디버그 시간 필요.
6. **Gateway가 단순 forward만 함**. JWT 1차 검증을 Gateway에서 했으면 서비스마다 동일한 보안 코드 줄어듦. 다만 mmg-common이 그 역할을 대신 하고 있어서 효과는 비슷.
7. **`menu_option*` 테이블이 entity는 있는데 사용 코드는 잘 안 보임**. 누가 만들고 안 쓰는지 확인.
8. **rider/admin `settlement` 이원화** (TABLE_SPEC.md §6에서도 지적). 비즈니스 의도 명시 또는 통합.

---

## 8. 끝 — 다시 읽을 때 조언

- 막막하면 **`UserService.signin` → `JwtTokenManager.issue` → `TokenAuthenticationFilter.doFilterInternal`** 한 사이클을 손가락으로 따라가보기. 절반 이해됨.
- **`DeliveryService.completeDelivery`** 한 메서드만 정독해도 상태 머신 + 낙관적 락 + Feign + SSE + 트랜잭션 다 들어 있음. 이 프로젝트 코드 패턴의 표본.
- ADR(`docs/adr/*`)이 "왜?"의 1차 출처. 코드만 보고 이상하다 싶을 땐 ADR 먼저 검색.

---

**참고 문서**:
- `docs/ddl/TABLE_SPEC.md` — 전체 테이블 명세
- `docs/adr/` — 결정 기록 (ADR-002 데이터 모델, ADR-004 상태 머신, ADR-008 라이더 상태 등)
- `CLAUDE.md` — 프로젝트 가이드, 영역 매트릭스, 절대 규칙
